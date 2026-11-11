/*
 * Copyright 2013-2026 consulo.io
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package consulo.desktop.awt.ui.impl.messageList;

import consulo.application.Application;
import consulo.desktop.awt.ui.impl.htmlView.ConsuloHtmlRendererContext;
import consulo.logging.Logger;
import consulo.ui.HtmlView;
import consulo.ui.image.Image;
import consulo.ui.messageList.AsyncHtmlImageLoader;
import org.cobraparser.html.HtmlRendererContext;
import org.cobraparser.html.domimpl.HTMLDocumentImpl;
import org.cobraparser.html.gui.HtmlBlockPanel;
import org.cobraparser.html.gui.HtmlPanel;
import org.cobraparser.html.parser.DocumentBuilderImpl;
import org.cobraparser.html.parser.InputSourceImpl;
import org.cobraparser.ua.UserAgentContext;
import org.jspecify.annotations.Nullable;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelListener;
import java.io.Reader;
import java.io.StringReader;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiPredicate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * The text of a message rendered by Cobra - parsed off the ui thread, laid out at the width of the message, as tall
 * as its content. Images go through the image loader of the list when it has one, and the body renders again as they
 * arrive.
 *
 * @author VISTALL
 * @since 2026-11-10
 */
final class CobraMessageBody extends HtmlPanel {
    private static final Logger LOG = Logger.getInstance(CobraMessageBody.class);
    // org.cobraparser.html.style.RenderState constants - the package is not exported
    private static final int OVERFLOW_HIDDEN = 3;
    private static final int OVERFLOW_VISIBLE = 4;

    private static final Pattern IMAGE_SOURCE = Pattern.compile("(<img\\b[^>]*?\\ssrc=\")([^\"]*)(\")", Pattern.CASE_INSENSITIVE);

    private final ConsuloHtmlRendererContext myContext;
    private final Runnable myRendered;
    private final Map<String, Image> myImages = new ConcurrentHashMap<>();
    private final Map<String, Boolean> myLoading = new ConcurrentHashMap<>();

    /**
     * Whether a document is being built - the text that came meanwhile waits in {@link #myPendingHtml}.
     */
    private boolean myParsing;
    private @Nullable String myPendingHtml;
    private @Nullable AsyncHtmlImageLoader myImageLoader;
    private String myHtml = "";
    private int myEstimatedHeight;
    private boolean myHasDocument;

    /**
     * @param linkHandler told a link clicked, true when it handled it
     * @param rendered    told once a new document is in place, so the list measures the message again
     */
    CobraMessageBody(BiPredicate<String, MouseEvent> linkHandler, Runnable rendered) {
        myRendered = rendered;
        myContext = new ConsuloHtmlRendererContext(this, this::resolveImage, linkHandler);
        setOpaque(false);
        // a message is as tall as its content - it never scrolls inside the list
        setDefaultOverflowX(OVERFLOW_HIDDEN);
        setDefaultOverflowY(OVERFLOW_VISIBLE);
    }

    @Override
    protected HtmlBlockPanel createHtmlBlockPanel(UserAgentContext uContext, HtmlRendererContext rContext) {
        HtmlBlockPanel block = new HtmlBlockPanel(new Color(0, 0, 0, 0), false, uContext, rContext, this);
        // the wheel scrolls the list - a component without a wheel listener hands the event up to its scroll pane
        for (MouseWheelListener listener : block.getMouseWheelListeners()) {
            block.removeMouseWheelListener(listener);
        }
        return block;
    }

    /**
     * Renders the body - the html of the content alone, the document around it is made here.
     *
     * @param estimatedHeight what the body is taken for until its document is there
     */
    void render(String bodyHtml, String styleSheet, @Nullable AsyncHtmlImageLoader imageLoader, int estimatedHeight) {
        myImageLoader = imageLoader;
        myEstimatedHeight = estimatedHeight;
        String html = "<html><head><style>" + styleSheet + "</style></head><body>" + routeImages(bodyHtml) + "</body></html>";
        if (html.equals(myHtml) && myHasDocument) {
            return;
        }
        myHtml = html;
        parse(html);
    }

    /**
     * Builds the document off the ui thread - one at a time: a text streamed meanwhile is built once this one is shown,
     * so a growing message always shows its latest finished document instead of none.
     */
    private void parse(String html) {
        if (myParsing) {
            myPendingHtml = html;
            return;
        }
        myParsing = true;
        Application.get().executeOnPooledThread(() -> {
            HTMLDocumentImpl document = null;
            try (Reader reader = new StringReader(html)) {
                DocumentBuilderImpl builder = new DocumentBuilderImpl(myContext.getUserAgentContext(), myContext);
                document = (HTMLDocumentImpl) builder.parse(new InputSourceImpl(reader, "file://message.html"));
                document.finishModifications();
                setDocument(document, myContext);
            }
            catch (Exception e) {
                LOG.warn("Cannot render a message", e);
            }
            boolean parsed = document != null;
            SwingUtilities.invokeLater(() -> {
                myParsing = false;
                if (parsed) {
                    myHasDocument = true;
                    revalidate();
                    repaint();
                    myRendered.run();
                }
                String pending = myPendingHtml;
                myPendingHtml = null;
                if (pending != null) {
                    parse(pending);
                }
            });
        });
    }

    /**
     * With an image loader the images are asked of it: their sources point to images this body resolves itself.
     */
    private String routeImages(String html) {
        if (myImageLoader == null || !html.contains("<img")) {
            return html;
        }
        Matcher matcher = IMAGE_SOURCE.matcher(html);
        StringBuilder result = new StringBuilder();
        while (matcher.find()) {
            String source = matcher.group(2);
            String routed = HtmlView.IMAGE_SRC_PREFIX + URLEncoder.encode(source, StandardCharsets.UTF_8);
            matcher.appendReplacement(result, Matcher.quoteReplacement(matcher.group(1) + routed + matcher.group(3)));
        }
        matcher.appendTail(result);
        return result.toString();
    }

    /**
     * Called by Cobra while it lays out - an image not loaded yet is asked of the loader, and the body renders again
     * once it is there.
     */
    private @Nullable Image resolveImage(String source) {
        Image image = myImages.get(source);
        if (image != null) {
            return image;
        }
        AsyncHtmlImageLoader loader = myImageLoader;
        if (loader != null && myLoading.putIfAbsent(source, Boolean.TRUE) == null) {
            loader.load(null, source).whenComplete((loaded, error) -> {
                if (loaded != null) {
                    myImages.put(source, loaded);
                    SwingUtilities.invokeLater(() -> parse(myHtml));
                }
            });
        }
        return null;
    }

    /**
     * As tall as the content laid out at the width of the parent - or as estimated while the document is not there.
     */
    @Override
    public Dimension getPreferredSize() {
        Container parent = getParent();
        int width = parent == null ? 0 : parent.getWidth();
        if (width <= 0) {
            return new Dimension(0, myEstimatedHeight);
        }
        HtmlBlockPanel block = htmlBlockPanel;
        if (!myHasDocument || block == null || block.getRootRenderable() == null) {
            return new Dimension(width, myEstimatedHeight);
        }
        setPreferredWidth(width);
        return new Dimension(width, block.getPreferredSize().height);
    }
}
