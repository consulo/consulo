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
package consulo.desktop.awt.ui.impl.base;

import consulo.ui.internal.SearchHighlightUtil;
import consulo.ui.style.ComponentColors;
import consulo.ui.util.ColorValueUtil;
import org.jspecify.annotations.Nullable;

import javax.swing.plaf.basic.BasicHTML;

/**
 * The search highlight of a swing text: the text is turned into html with the matched runs drawn on the search match
 * colour, the way {@code SearchUtil.markup} marks the options of a settings page. A text which is html already keeps
 * its markup and has only its shown text marked.
 *
 * @author VISTALL
 * @since 2026-10-10
 */
public final class DesktopSearchHighlight {
    private static final String CLOSE_TAG = "</span>";

    private DesktopSearchHighlight() {
    }

    private static String openTag() {
        return "<span style=\"background-color:#" + ColorValueUtil.toHex(ComponentColors.SEARCH_MATCH_BACKGROUND)
            + ";color:#" + ColorValueUtil.toHex(ComponentColors.SEARCH_MATCH_FOREGROUND) + "\">";
    }

    /**
     * @return the text to show with the matches of the query marked, or {@code null} if the query matches nothing and
     * the text is to be shown as it is
     */
    public static @Nullable String toHtml(String text, @Nullable String query) {
        if (query == null) {
            return null;
        }

        if (BasicHTML.isHTMLString(text)) {
            String marked = SearchHighlightUtil.markupHtml(text, query, openTag(), CLOSE_TAG);
            return marked.equals(text) ? null : marked;
        }

        if (!SearchHighlightUtil.matches(text, query)) {
            return null;
        }

        return "<html><nobr>" + SearchHighlightUtil.markupText(text, query, openTag(), CLOSE_TAG) + "</nobr></html>";
    }
}
