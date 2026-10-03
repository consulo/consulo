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
package consulo.it.internal.ui;

import consulo.localize.LocalizeValue;
import consulo.ui.MenuItem;
import consulo.ui.image.Image;
import org.jspecify.annotations.Nullable;

/**
 * @author VISTALL
 */
public class HeadlessMenuItem extends HeadlessComponentBase implements MenuItem {
    private final LocalizeValue myText;
    private @Nullable Image myIcon;
    private @Nullable Boolean myChecked;
    private LocalizeValue myShortcutText = LocalizeValue.empty();

    public HeadlessMenuItem(LocalizeValue text) {
        myText = text;
    }

    @Override
    public LocalizeValue getText() {
        return myText;
    }

    @Override
    public void setIcon(@Nullable Image icon) {
        myIcon = icon;
    }

    public @Nullable Image getIcon() {
        return myIcon;
    }

    @Override
    public void setChecked(@Nullable Boolean checked) {
        myChecked = checked;
    }

    public @Nullable Boolean getChecked() {
        return myChecked;
    }

    @Override
    public void setShortcutText(LocalizeValue shortcutText) {
        myShortcutText = shortcutText;
    }

    public LocalizeValue getShortcutText() {
        return myShortcutText;
    }
}
