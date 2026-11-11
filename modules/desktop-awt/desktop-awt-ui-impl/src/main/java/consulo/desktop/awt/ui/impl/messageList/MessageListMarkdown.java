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

import consulo.ui.ex.awt.UIUtil;
import consulo.ui.ex.awt.util.ColorUtil;
import consulo.ui.ex.awtUnsafe.TargetAWT;
import consulo.ui.style.ComponentColors;
import org.commonmark.Extension;
import org.commonmark.ext.gfm.strikethrough.StrikethroughExtension;
import org.commonmark.ext.gfm.tables.TablesExtension;
import org.commonmark.node.FencedCodeBlock;
import org.commonmark.node.IndentedCodeBlock;
import org.commonmark.node.Node;
import org.commonmark.parser.Parser;
import org.commonmark.renderer.NodeRenderer;
import org.commonmark.renderer.html.HtmlNodeRendererContext;
import org.commonmark.renderer.html.HtmlRenderer;

import java.awt.*;
import java.util.List;
import java.util.Set;

/**
 * Turns the text of a message into the html Cobra renders, with the look of a message.
 *
 * @author VISTALL
 * @since 2026-10-13
 */
final class MessageListMarkdown {
    private static final List<Extension> EXTENSIONS = List.of(TablesExtension.create(), StrikethroughExtension.create());
    private static final Parser PARSER = Parser.builder().extensions(EXTENSIONS).build();
    private static final HtmlRenderer RENDERER = HtmlRenderer.builder()
        .extensions(EXTENSIONS)
        .escapeHtml(true)
        .nodeRendererFactory(CodeBlockRenderer::new)
        .build();

    /**
     * Code blocks as wrapping text - Cobra takes {@code pre-wrap} for {@code pre}, so a {@code pre} line longer than the
     * message would be cut.
     */
    private static final class CodeBlockRenderer implements NodeRenderer {
        private final HtmlNodeRendererContext myContext;

        CodeBlockRenderer(HtmlNodeRendererContext context) {
            myContext = context;
        }

        @Override
        public Set<Class<? extends Node>> getNodeTypes() {
            return Set.of(FencedCodeBlock.class, IndentedCodeBlock.class);
        }

        @Override
        public void render(Node node) {
            String literal = node instanceof FencedCodeBlock fenced ? fenced.getLiteral() : ((IndentedCodeBlock) node).getLiteral();
            if (literal.endsWith("\n")) {
                literal = literal.substring(0, literal.length() - 1);
            }
            myContext.getWriter().line();
            myContext.getWriter().raw("<div class='code'>" + toWrappingHtml(literal) + "</div>");
            myContext.getWriter().line();
        }
    }

    /**
     * The html of the text for Cobra, which draws html as a browser does.
     */
    static String toCobraHtml(String text, boolean markdown) {
        if (markdown) {
            return RENDERER.render(PARSER.parse(text));
        }
        return "<p class='plain'>" + toWrappingHtml(text) + "</p>";
    }

    /**
     * Preformatted text as html that wraps: the line breaks and the indents stay, the lines longer than the message
     * wrap at their spaces - what {@code white-space: pre-wrap} does, which Cobra does not.
     */
    static String toWrappingHtml(String text) {
        StringBuilder html = new StringBuilder(text.length() + 16);
        boolean lineStart = true;
        boolean afterSpace = false;
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            switch (c) {
                case '\n' -> {
                    html.append("<br>");
                    lineStart = true;
                    afterSpace = false;
                    continue;
                }
                case '\r' -> {
                    continue;
                }
                // a space breaks the line, the ones after it and the indent keep their width
                case ' ' -> html.append(lineStart || afterSpace ? "&nbsp;" : " ");
                case '\t' -> html.append("&nbsp;&nbsp;&nbsp;&nbsp;");
                case '<' -> html.append("&lt;");
                case '>' -> html.append("&gt;");
                case '&' -> html.append("&amp;");
                case '"' -> html.append("&quot;");
                default -> html.append(c);
            }
            boolean space = c == ' ' || c == '\t';
            afterSpace = space;
            lineStart = lineStart && space;
        }
        return html.toString();
    }

    /**
     * The look of a message for Cobra - the font and colours of the ide, code, tables and quotes as a chat shows them.
     */
    static String cobraStyleSheet(Color foreground) {
        Font font = UIUtil.getLabelFont();
        String text = ColorUtil.toHtmlColor(foreground);
        String codeBackground = ColorUtil.toHtmlColor(TargetAWT.to(ComponentColors.HOVER_BACKGROUND));
        String border = ColorUtil.toHtmlColor(TargetAWT.to(ComponentColors.BORDER));
        String muted = ColorUtil.toHtmlColor(UIUtil.getContextHelpForeground());
        String link = ColorUtil.toHtmlColor(TargetAWT.to(ComponentColors.LINK_FOREGROUND));
        return "html { overflow: hidden; margin: 0; padding: 0; }"
            + " body { overflow: hidden; margin: 0; padding: 0; font-family: '" + font.getFamily() + "';"
            + " font-size: " + font.getSize() + "px; color: " + text + "; }"
            + "p { margin: 0 0 6px 0; } p.plain { margin: 0; }"
            + "h1, h2, h3, h4 { margin: 6px 0 4px 0; } h1 { font-size: 1.4em; } h2 { font-size: 1.25em; } h3 { font-size: 1.1em; }"
            + "ul, ol { margin: 2px 0 6px 0; padding-left: 22px; } li { margin: 1px 0; }"
            + "a { color: " + link + "; }"
            + "code { font-family: monospace; background-color: " + codeBackground + "; padding: 0 3px; }"
            + "div.code { font-family: monospace; background-color: " + codeBackground + "; padding: 8px; margin: 4px 0; }"
            + "table { border-collapse: collapse; margin: 4px 0; }"
            + "th, td { border: 1px solid " + border + "; padding: 4px 8px; text-align: left; }"
            + "th { background-color: " + codeBackground + "; }"
            + "blockquote { margin: 4px 0; padding-left: 10px; border-left: 3px solid " + border + "; color: " + muted + "; }"
            + "del { text-decoration: line-through; }"
            + "img { max-width: 100%; }";
    }

    private MessageListMarkdown() {
    }
}
