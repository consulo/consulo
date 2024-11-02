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
import consulo.desktop.awt.facade.FromSwingComponentWrapper;
import consulo.desktop.awt.ui.impl.base.SwingComponentDelegate;
import consulo.ui.Component;
import consulo.ui.HtmlView;
import consulo.ui.ex.JBColor;
import consulo.util.io.CharsetToolkit;
import consulo.util.io.StreamUtil;
import consulo.util.lang.Pair;
import consulo.util.lang.Range;
import consulo.util.lang.StringUtil;
import consulo.util.lang.ref.Ref;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
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
import org.w3c.dom.Node;
import org.xml.sax.SAXException;

import javax.swing.*;
import java.awt.*;
import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.net.URL;
import java.util.List;

/**
 * @author VISTALL
 * @since 24/11/2021
 */
public class DesktopAWTHtmlViewImpl extends SwingComponentDelegate<DesktopAWTHtmlViewImpl.MyHtmlPanel> implements HtmlView {
    private static final int FOCUS_ELEMENT_DY = 100;

    public class MyHtmlPanel extends HtmlPanel implements FromSwingComponentWrapper {
        private final ConsuloHtmlRendererContext myContext;

        public MyHtmlPanel() {
            myContext = new ConsuloHtmlRendererContext(this);
        }

        @Override
        protected HtmlBlockPanel createHtmlBlockPanel(UserAgentContext ucontext, HtmlRendererContext rcontext) {
            return new ScrollPreservingHtmlBlockPanel(JBColor.WHITE, true, ucontext, rcontext, this);
        }

        @Nonnull
        @Override
        public Component toUIComponent() {
            return DesktopAWTHtmlViewImpl.this;
        }
    }

    public DesktopAWTHtmlViewImpl() {
        initialize(new MyHtmlPanel());
    }

    @Override
    public void render(@Nonnull RenderData renderData) {
        MyHtmlPanel panel = toAWTComponent();

        ConsuloHtmlRendererContext context = panel.myContext;

        String html = renderData.html();

        String inlineCss = renderData.inlineCss();
        if (renderData.externalCsses().length > 0 && renderData.externalCsses()[0] != null) {
            try {
                final URL url = renderData.externalCsses()[0];
                final String cssText = StreamUtil.readText(url.openStream(), CharsetToolkit.UTF8);
                inlineCss += "\n" + cssText;
            }
            catch (IOException ignore) {
            }
        }

        final String htmlToRender = html.replace("<head>", "<head>" + getCssLines(inlineCss));

        Application.get().executeOnPooledThread(() -> {
            try {
                final DocumentBuilderImpl builder = new DocumentBuilderImpl(context.getUserAgentContext(), context);
                try (
                    final Reader reader = new StringReader(htmlToRender)) {
                    final InputSourceImpl is = new InputSourceImpl(reader, "file://a.html");

                    final HTMLDocumentImpl document = (HTMLDocumentImpl) builder.parse(is);

                    document.finishModifications();

                    panel.setDocument(document, context);
                }
            }
            catch (final IOException | SAXException ioe) {
                throw new IllegalStateException("Unexpected condition.", ioe);
            }
        });
    }

    @Override
    public void scrollToMarkdownSrcOffset(int offset) {
        MyHtmlPanel panel = toAWTComponent();

        SwingUtilities.invokeLater(() -> {
            final NodeImpl root = panel.getRootNode();
            final Ref<Pair<Node, Integer>> resultY = new Ref<>();
            root.visit(new NodeVisitor() {
                @Override
                public void visit(Node node) {
                    Node child = node.getFirstChild();
                    while (child != null) {
                        final Range<Integer> range = nodeToSrcRange(child);
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

                final RBlockViewport viewport = ((RBlock) panel.getBlockRenderable()).getRBlockViewport();
                final Rectangle renderBounds = panel.getBlockRenderable().getBounds();

                if (viewport.getY() + viewport.getHeight() - renderBounds.getHeight() > 0) {
                    panel.scrollBy(0, -FOCUS_ELEMENT_DY);
                }

                panel.repaint();
            }
        });
    }

    @Nullable
    private static Range<Integer> nodeToSrcRange(@Nonnull Node node) {
        if (!node.hasAttributes()) {
            return null;
        }
        final Node attribute = node.getAttributes().getNamedItem("src");
        if (attribute == null) {
            return null;
        }
        final List<String> startEnd = StringUtil.split(attribute.getNodeValue(), "..");
        if (startEnd.size() != 2) {
            return null;
        }
        return new Range<>(Integer.parseInt(startEnd.get(0)), Integer.parseInt(startEnd.get(1)));
    }

    @Nonnull
    private static String getCssLines(@Nullable String inlineCss, @Nonnull String... fileUris) {
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
