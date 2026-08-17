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
package consulo.desktop.qt.ui.impl.htmlView;

import consulo.desktop.qt.ui.impl.QtComponentDelegate;
import consulo.desktop.qt.ui.impl.image.DesktopQtImage;
import consulo.logging.Logger;
import consulo.ui.HtmlView;
import consulo.ui.UIAccess;
import consulo.ui.event.HyperlinkEvent;
import consulo.ui.image.Image;
import consulo.util.io.StreamUtil;
import io.qt.core.QUrl;
import io.qt.gui.QPixmap;
import io.qt.gui.QTextDocument;
import io.qt.widgets.QFrame;
import io.qt.widgets.QTextBrowser;
import io.qt.widgets.QWidget;
import org.jspecify.annotations.Nullable;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;

/**
 * The document is rendered by the rich text engine of qt rather than by a browser - {@link HtmlView} is documented
 * as html4 without css3 and without scripting, which is exactly what that engine covers.
 *
 * @author VISTALL
 * @since 2026-08-16
 */
public class DesktopQtHtmlViewImpl extends QtComponentDelegate<QTextBrowser> implements HtmlView {
    private static final Logger LOG = Logger.getInstance(DesktopQtHtmlViewImpl.class);

    /**
     * An {@code img} of the document naming an image of the platform is fetched through here rather than being
     * rewritten into the markup: the rich text engine asks for every resource it cannot resolve itself, so the id
     * stays an id and nothing has to be turned into a url first.
     */
    private class QtHtmlView extends QTextBrowser {
        QtHtmlView(QWidget parent) {
            super(parent);
        }

        @Override
        public Object loadResource(int type, QUrl name) {
            if (type == QTextDocument.ResourceType.ImageResource.value()) {
                QPixmap pixmap = resolveImage(name.toString());
                if (pixmap != null) {
                    return pixmap;
                }
            }

            return super.loadResource(type, name);
        }
    }

    private volatile @Nullable Function<String, Image> myImageResolver;

    private @Nullable String myPendingHtml;

    @Override
    protected QTextBrowser createQt(QWidget parent) {
        return new QtHtmlView(parent);
    }

    @Override
    protected void initialize(QTextBrowser component) {
        component.setReadOnly(true);

        // the view carries no frame of its own - whatever holds it draws the border, the way the awt one is put
        // inside a scroll pane which owns the frame
        component.setFrameShape(QFrame.Shape.NoFrame);

        // a link is a choice the platform makes, not a page to walk to: the browser must neither navigate nor hand
        // the url to the desktop, only report it
        component.setOpenLinks(false);
        component.setOpenExternalLinks(false);

        component.anchorClicked.connect(url ->
            getListenerDispatcher(HyperlinkEvent.class).onEvent(new HyperlinkEvent(this, url.toString(), null))
        );

        String pending = myPendingHtml;
        if (pending != null) {
            myPendingHtml = null;

            component.setHtml(pending);
        }
    }

    private @Nullable QPixmap resolveImage(String source) {
        if (!source.startsWith(IMAGE_SRC_PREFIX)) {
            return null;
        }

        Function<String, Image> resolver = myImageResolver;
        if (resolver == null) {
            return null;
        }

        String id = source.substring(IMAGE_SRC_PREFIX.length());

        try {
            Image image = resolver.apply(id);

            return image instanceof DesktopQtImage qtImage ? qtImage.toQPixmap() : null;
        }
        catch (Exception e) {
            LOG.warn("Failed to resolve the image " + id + " of an html view", e);
            return null;
        }
    }

    @Override
    public void setImageResolver(@Nullable Function<String, Image> imageResolver) {
        myImageResolver = imageResolver;
    }

    @Override
    public CompletableFuture<?> render(RenderData renderData) {
        String document = buildDocument(renderData);

        QTextBrowser component = myComponent;
        if (component == null) {
            // the widget of a component only exists once it is bound, and a caller renders into the view it just
            // built rather than waiting for it to be shown
            myPendingHtml = document;
            return CompletableFuture.completedFuture(null);
        }

        if (UIAccess.isUIThread()) {
            component.setHtml(document);
            return CompletableFuture.completedFuture(null);
        }

        return getUIAccess().giveAsync(() -> {
            if (myComponent != null) {
                myComponent.setHtml(document);
            }
            return null;
        });
    }

    /**
     * Every stylesheet is folded into the document rather than linked. The urls a caller hands over point into the
     * running platform - a {@code jar:file:} inside a plugin - which the rich text engine cannot fetch.
     */
    private static String buildDocument(RenderData renderData) {
        StringBuilder head = new StringBuilder();

        for (URL css : renderData.externalCsses()) {
            if (css == null) {
                continue;
            }

            try (InputStream stream = css.openStream()) {
                head.append("<style>\n").append(StreamUtil.readText(stream, StandardCharsets.UTF_8)).append("\n</style>\n");
            }
            catch (IOException e) {
                LOG.warn("Failed to read the stylesheet " + css + " of an html view", e);
            }
        }

        String inlineCss = renderData.inlineCss();
        if (!inlineCss.isEmpty()) {
            head.append("<style>\n").append(inlineCss).append("\n</style>\n");
        }

        if (head.isEmpty()) {
            return renderData.html();
        }

        String html = renderData.html();

        // a document which never opened a head has nowhere to put the sheet - the engine reads a leading style
        // block just as well, so it is prepended instead
        return html.contains("<head>") ? html.replace("<head>", "<head>" + head) : head + html;
    }

    /**
     * The rich text engine keeps no dom and drops the attributes of the source, so there is nothing left to look a
     * markdown offset up by - only the browser and the cobra dom of the awt frontend can answer this.
     */
    @Override
    public void scrollToMarkdownSrcOffset(int offset) {
    }
}
