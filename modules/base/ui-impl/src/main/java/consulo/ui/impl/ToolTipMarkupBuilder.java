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
package consulo.ui.impl;

import consulo.application.util.HtmlChunk;
import consulo.localize.LocalizeValue;
import consulo.ui.internal.ToolTipImpl;

import java.util.regex.Pattern;

/**
 * @author VISTALL
 * @since 2026-10-09
 */
public abstract class ToolTipMarkupBuilder {
    private static final Pattern PARAGRAPH_SPLITTER = Pattern.compile("<p/?>");

    public static boolean isPlain(ToolTipImpl toolTip) {
        return toolTip.getShortcut().isEmpty() && !hasDescription(toolTip);
    }

    public String build(ToolTipImpl toolTip) {
        LocalizeValue title = toolTip.getTitle();
        LocalizeValue shortcut = toolTip.getShortcut();
        LocalizeValue description = toolTip.getDescription();

        boolean hasTitle = title.isNotEmpty();

        HtmlChunk.Element root = root();

        if (hasTitle) {
            HtmlChunk.Element header = header().addText(title);
            if (shortcut.isNotEmpty()) {
                header = header.addText(LocalizeValue.space()).child(shortcut(HtmlChunk.span()).addText(shortcut));
            }
            root = root.child(header);
        }

        if (hasDescription(toolTip)) {
            for (String paragraph : PARAGRAPH_SPLITTER.split(description.get())) {
                if (!paragraph.isEmpty()) {
                    root = root.child(paragraph(hasTitle).addRaw(paragraph));
                }
            }
        }

        if (!hasTitle && shortcut.isNotEmpty()) {
            root = root.child(shortcut(HtmlChunk.div()).addText(shortcut));
        }

        return root.toString().replace('\n', ' ');
    }

    protected abstract HtmlChunk.Element root();

    protected abstract HtmlChunk.Element header();

    protected abstract HtmlChunk.Element shortcut(HtmlChunk.Element element);

    protected abstract HtmlChunk.Element paragraph(boolean underTitle);

    private static boolean hasDescription(ToolTipImpl toolTip) {
        return toolTip.getDescription().isNotEmpty() && !toolTip.getTitle().equals(toolTip.getDescription());
    }
}
