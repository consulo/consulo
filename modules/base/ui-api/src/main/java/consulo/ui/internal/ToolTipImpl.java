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
package consulo.ui.internal;

import consulo.localize.LocalizeValue;
import consulo.ui.ToolTip;

/**
 * @author VISTALL
 * @since 2026-10-09
 */
public class ToolTipImpl implements ToolTip {
    private LocalizeValue myTitle = LocalizeValue.empty();
    private LocalizeValue myShortcut = LocalizeValue.empty();
    private LocalizeValue myDescription = LocalizeValue.empty();

    @Override
    public ToolTip setTitle(LocalizeValue title) {
        myTitle = title;
        return this;
    }

    @Override
    public ToolTip setShortcut(LocalizeValue shortcut) {
        myShortcut = shortcut;
        return this;
    }

    @Override
    public ToolTip setDescription(LocalizeValue description) {
        myDescription = description;
        return this;
    }

    public LocalizeValue getTitle() {
        return myTitle;
    }

    public LocalizeValue getShortcut() {
        return myShortcut;
    }

    public LocalizeValue getDescription() {
        return myDescription;
    }

    public boolean isEmpty() {
        return myTitle.isEmpty() && myShortcut.isEmpty() && myDescription.isEmpty();
    }
}
