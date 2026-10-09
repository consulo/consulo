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

import consulo.localize.LocalizeValue;
import consulo.ui.internal.ToolTipImpl;

/**
 * @author VISTALL
 * @see Component#setToolTip(ToolTip)
 * @since 2026-10-09
 */
public interface ToolTip {
    static ToolTip create() {
        return new ToolTipImpl();
    }

    /**
     * Sets tooltip title. If it's longer than 2 lines (fitting in 250 pixels each) then
     * the text is automatically stripped to the word boundary and dots are added to the end.
     *
     * @param title text for title.
     * @return {@code this}
     */
    ToolTip setTitle(LocalizeValue title);

    /**
     * Sets text for the shortcut placeholder.
     *
     * @param shortcut text for shortcut.
     * @return {@code this}
     */
    ToolTip setShortcut(LocalizeValue shortcut);

    /**
     * Sets description text.
     *
     * @param description text for description.
     * @return {@code this}
     */
    ToolTip setDescription(LocalizeValue description);
}
