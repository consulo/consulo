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
package consulo.ui;

import consulo.ui.annotation.RequiredUIAccess;
import org.jspecify.annotations.Nullable;

/**
 * A component whose own text can show where a search query matches it, the way a settings page marks the options a
 * search found on it.
 * <p>
 * The query is split into words at whitespace. Every place where one of the words occurs in the shown text, compared
 * without regard to case, is drawn highlighted; places which overlap or touch are drawn as one. A word matches inside
 * a longer word as well as at its start, so "size" marks both "Tab size" and "Resize". The mnemonic marker of the text
 * takes no part in matching, and neither does the markup of a text which is markup: only the text it shows is matched.
 * <p>
 * The highlight belongs to the component, not to its text: it stays when the text changes and is drawn over the new
 * text, until it is cleared with {@code null}. A text the query does not match is drawn as it would be without one.
 *
 * @author VISTALL
 * @since 2026-10-10
 */
public interface HasSearchHighlight extends Component {
    /**
     * @param query the words to highlight; {@code null}, or a query without words, clears the highlight
     */
    @RequiredUIAccess
    void setSearchHighlight(@Nullable String query);

    /**
     * @return the query highlighted now, or {@code null} if the highlight is cleared
     */
    @Nullable
    String getSearchHighlight();
}
