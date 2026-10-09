/*
 * Copyright 2013-2021 consulo.io
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
package consulo.desktop.awt.ui.impl.htmlView;

import consulo.application.Application;
import consulo.desktop.awt.ui.impl.facade.FromSwingComponentWrapper;
import consulo.desktop.awt.ui.impl.base.SwingComponentDelegate;
import consulo.desktop.awt.ui.impl.event.DesktopAWTInputDetails;
import consulo.ui.Component;
import consulo.ui.HtmlView;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.event.HtmlViewDoubleClickEvent;
import consulo.ui.event.HyperlinkEvent;
import consulo.ui.ex.JBColor;
import consulo.ui.ex.awt.JBUI;
import consulo.ui.image.Image;
import consulo.ui.impl.HtmlDocuments;
import consulo.util.io.CharsetToolkit;
import consulo.util.io.StreamUtil;
import consulo.util.lang.Pair;
import consulo.util.lang.Range;
import consulo.util.lang.StringUtil;
import consulo.util.lang.ref.SimpleReference;
import org.jspecify.annotations.Nullable;
import org.cobraparser.html.HtmlRendererContext;
import org.cobraparser.html.domimpl.HTMLDocumentImpl;
import org.cobraparser.html.domimpl.NodeImpl;
import org.cobraparser.html.domimpl.NodeVisitor;
import org.cobraparser.html.gui.HtmlBlockPanel;
import org.cobraparser.html.gui.HtmlPanel;
import org.cobraparser.html.parser.DocumentBuilderImpl;
import org.cobraparser.html.parser.InputSourceImpl;
import org.cobraparser.html.renderer.RBlock;
import org.cobraparser.html.renderer.RBlockViewport;
import org.cobraparser.ua.UserAgentContext;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.xml.sax.SAXException;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.net.URL;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;

/**
 * @author VISTALL
 * @since 2021-11-24
 */
public class DesktopAWTHtmlViewImpl extends SwingComponentDelegate<DesktopAWTHtmlViewImpl.MyHtmlPanel> implements HtmlView {
    private static final int FOCUS_ELEMENT_DY = 100;
    private static final int CONTENT_WIDTH = 500;
    private static final int CONTENT_HEIGHT = 500;

    private volatile @Nullable Function<String, Image> myImageResolver;

    private boolean mySizeToContent;

    public class MyHtmlPanel extends HtmlPanel implements FromSwingComponentWrapper {
        private final ConsuloHtmlRendererContext myContext;

        public MyHtmlPanel() {
            myContext = new ConsuloHtmlRendererContext(this, DesktopAWTHtmlViewImpl.this::resolveImage, DesktopAWTHtmlViewImpl.this::onLink);
        }

        @Override
        protected HtmlBlockPanel createHtmlBlockPanel(UserAgentContext uContext, HtmlRendererContext rContext) {
            HtmlBlockPanel blockPanel = new ScrollPreservingHtmlBlockPanel(JBColor.WHITE, true, uContext, rContext, this);
            blockPanel.addMouseListener(new MouseAdapter() {
                @Override
                public void mouseClicked(MouseEvent e) {
                    if (e.getClickCount() == 2 && SwingUtilities.isLeftMouseButton(e)) {
                        getListenerDispatcher(HtmlViewDoubleClickEvent.class).onEvent(
                            new HtmlViewDoubleClickEvent(DesktopAWTHtmlViewImpl.this, DesktopAWTInputDetails.convert(MyHtmlPanel.this, e))
                        );
                    }
                }
            });
            return blockPanel;
        }

        @Override
        public Dimension getPreferredSize() {
            Dimension size = super.getPreferredSize();
            if (!mySizeToContent || isPreferredSizeSet()) {
                return size;
            }

            int maxHeight = JBUI.scale(CONTENT_HEIGHT);
            if (size.height <= maxHeight) {
                return size;
            }
            return new Dimension(size.width + scrollBarWidth(), maxHeight);
        }

        private int scrollBarWidth() {
            HtmlBlockPanel blockPanel = htmlBlockPanel;
            return blockPanel != null && blockPanel.getRootRenderable() instanceof RBlock block ? block.getVScrollBarWidth() : 0;
        }

        @Override
        public Component toUIComponent() {
            return DesktopAWTHtmlViewImpl.this;
        }
    }

    @Override
    protected MyHtmlPanel createComponent() {
        return new MyHtmlPanel();
    }

    @Override
    public CompletableFuture<?> render(RenderData renderData) {
        CompletableFuture<?> future = new CompletableFuture<>();

        MyHtmlPanel panel = toAWTComponent();

        ConsuloHtmlRendererContext context = panel.myContext;

        String html = renderData.html();

        String inlineCss = renderData.inlineCss();
        if (renderData.externalCsses().length > 0 && renderData.externalCsses()[0] != null) {
            try {
                URL url = renderData.externalCsses()[0];
                String cssText = StreamUtil.readText(url.openStream(), CharsetToolkit.UTF8);
                inlineCss += "\n" + cssText;
            }
            catch (IOException ignore) {
            }
        }

        String htmlToRender = HtmlDocuments.withHead(html, getCssLines(inlineCss));

        Application.get().executeOnPooledThread(() -> {
            try {
                DocumentBuilderImpl builder = new DocumentBuilderImpl(context.getUserAgentContext(), context);
                try (
                    final Reader reader = new StringReader(htmlToRender)) {
                    InputSourceImpl is = new InputSourceImpl(reader, "file://a.html");

                    HTMLDocumentImpl document = (HTMLDocumentImpl) builder.parse(is);

                    document.finishModifications();

                    panel.setDocument(document, context);

                    SwingUtilities.invokeLater(() -> {
                        if (mySizeToContent) {
                            panel.revalidate();
                        }
                        future.complete(null);
                    });
                }
            }
            catch (IOException | SAXException ioe) {
                future.completeExceptionally(ioe);
                throw new IllegalStateException("Unexpected condition.", ioe);
            }
        });

        return future;
    }

    @Override
    public void setImageResolver(@Nullable Function<String, Image> imageResolver) {
        myImageResolver = imageResolver;
    }

    @Override
    @RequiredUIAccess
    public void setSizeToContent(boolean sizeToContent) {
        mySizeToContent = sizeToContent;

        MyHtmlPanel panel = toAWTComponent();
        panel.setPreferredWidth(sizeToContent ? JBUI.scale(CONTENT_WIDTH) : -1);
        panel.revalidate();
    }

    @Override
    @RequiredUIAccess
    public void scrollToFragment(String fragment) {
        MyHtmlPanel panel = toAWTComponent();
        NodeImpl root = panel.getRootNode();
        if (root == null || fragment.isEmpty()) {
            return;
        }

        SimpleReference<Node> target = new SimpleReference<>();
        root.visit(node -> {
            if (target.get() == null && node instanceof Element element && isFragmentTarget(element, fragment)) {
                target.set(node);
            }
        });

        Node node = target.get();
        if (node != null) {
            panel.scrollTo(node);
        }
    }

    private static boolean isFragmentTarget(Element element, String fragment) {
        if (fragment.equals(element.getAttribute("id"))) {
            return true;
        }
        return "A".equalsIgnoreCase(element.getTagName()) && fragment.equals(element.getAttribute("name"));
    }

    private boolean onLink(String href, MouseEvent event) {
        if (hasListeners(HyperlinkEvent.class)) {
            getListenerDispatcher(HyperlinkEvent.class).onEvent(
                new HyperlinkEvent(this, href, DesktopAWTInputDetails.convert(toAWTComponent(), event))
            );
            return true;
        }

        if (href.startsWith("#")) {
            scrollToFragment(href.substring(1));
            return true;
        }
        return false;
    }

    private @Nullable Image resolveImage(String id) {
        Function<String, Image> resolver = myImageResolver;
        return resolver == null ? null : resolver.apply(id);
    }

    @Override
    public void scrollToMarkdownSrcOffset(int offset) {
        MyHtmlPanel panel = toAWTComponent();

        SwingUtilities.invokeLater(() -> {
            NodeImpl root = panel.getRootNode();
            if (root == null) {
                return;
            }

            final SimpleReference<Pair<Node, Integer>> resultY = new SimpleReference<>();
            root.visit(new NodeVisitor() {
                @Override
                public void visit(Node node) {
                    Node child = node.getFirstChild();
                    while (child != null) {
                        Range<Integer> range = nodeToSrcRange(child);
                        if (range != null && child instanceof NodeImpl) {
                            int currentDist = Math.min(Math.abs(range.getFrom() - offset), Math.abs(range.getTo() - 1 - offset));
                            if (resultY.get() == null || resultY.get().getSecond() > currentDist) {
                                resultY.set(Pair.create(child, currentDist));
                            }
                        }

                        if (range == null || range.getTo() <= offset) {
                            child = child.getNextSibling();
                            continue;
                        }

                        if (range.getFrom() > offset) {
                            break;
                        }
                        if (range.getTo() > offset) {
                            visit(child);
                            break;
                        }
                    }
                }
            });

            if (resultY.get() != null) {
                panel.scrollTo(resultY.get().getFirst());

                RBlockViewport viewport = ((RBlock) panel.getBlockRenderable()).getRBlockViewport();
                Rectangle renderBounds = panel.getBlockRenderable().getBounds();

                if (viewport.getY() + viewport.getHeight() - renderBounds.getHeight() > 0) {
                    panel.scrollBy(0, -FOCUS_ELEMENT_DY);
                }

                panel.repaint();
            }
        });
    }

    private static @Nullable Range<Integer> nodeToSrcRange(Node node) {
        if (!node.hasAttributes()) {
            return null;
        }
        Node attribute = node.getAttributes().getNamedItem("src");
        if (attribute == null) {
            return null;
        }
        List<String> startEnd = StringUtil.split(attribute.getNodeValue(), "..");
        if (startEnd.size() != 2) {
            return null;
        }
        return new Range<>(Integer.parseInt(startEnd.get(0)), Integer.parseInt(startEnd.get(1)));
    }

    
    private static String getCssLines(@Nullable String inlineCss, String... fileUris) {
        StringBuilder result = new StringBuilder();

        for (String uri : fileUris) {
            if (uri == null) {
                continue;
            }
            result.append("<link rel=\"stylesheet\" href=\"").append(uri).append("\" />\n");
        }
        if (inlineCss != null) {
            result.append("<style>\n").append(inlineCss).append("\n</style>\n");
        }
        return result.toString();
    }
}
