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

import consulo.ui.ex.awt.util.ColorUtil;
import consulo.ui.ex.awtUnsafe.TargetAWT;
import consulo.ui.style.ComponentColors;
import consulo.util.lang.StringUtil;
import org.commonmark.Extension;
import org.commonmark.ext.gfm.strikethrough.StrikethroughExtension;
import org.commonmark.ext.gfm.tables.TablesExtension;
import org.commonmark.parser.Parser;
import org.commonmark.renderer.html.HtmlRenderer;

import java.util.List;

/**
 * Turns the text of a message into the html the swing view shows.
 *
 * @author VISTALL
 * @since 2026-10-13
 */
final class MessageListMarkdown {
    private static final List<Extension> EXTENSIONS = List.of(TablesExtension.create(), StrikethroughExtension.create());
    private static final Parser PARSER = Parser.builder().extensions(EXTENSIONS).build();
    private static final HtmlRenderer RENDERER = HtmlRenderer.builder().extensions(EXTENSIONS).escapeHtml(true).build();

    static String toHtml(String text, boolean markdown) {
        if (markdown) {
            return RENDERER.render(PARSER.parse(text))
                .replace("<del>", "<strike>")
                .replace("</del>", "</strike>")
                .replace("<table>", "<table cellspacing='0' cellpadding='6'>");
        }
        return StringUtil.escapeXmlEntities(text).replace("\n", "<br>");
    }

    /**
     * What the swing html view does not draw on its own - code, tables, strikethrough - in the colours of the style.
     */
    static String styleSheet() {
        String codeBackground = ColorUtil.toHtmlColor(TargetAWT.to(ComponentColors.HOVER_BACKGROUND));
        String border = ColorUtil.toHtmlColor(TargetAWT.to(ComponentColors.BORDER));
        return "code { font-family: monospace; background-color: " + codeBackground + "; }"
            + "pre { font-family: monospace; background-color: " + codeBackground + "; padding: 8px; margin-top: 4px; margin-bottom: 4px; }"
            + "table { border-color: " + border + "; border-style: solid; border-width: 1px; }"
            + "th { text-align: left; background-color: " + codeBackground + "; border-bottom: 1px solid " + border + "; }"
            + "td { border-bottom: 1px solid " + border + "; }"
            + "p { margin-top: 0; margin-bottom: 6px; }";
    }

    private MessageListMarkdown() {
    }
}
