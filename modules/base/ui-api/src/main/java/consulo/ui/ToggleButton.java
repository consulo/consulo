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
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.internal.UIInternal;

/**
 * Button which keeps a pressed state - what a toolbar draws for a toggle action. A click flips the value and
 * notifies both the click listeners of the button and the value listeners.
 *
 * @author VISTALL
 * @since 2026-08-07
 */
public interface ToggleButton extends Button, ValueComponent<Boolean> {
    @RequiredUIAccess
    static ToggleButton create(LocalizeValue text) {
        return create(text, false);
    }

    @RequiredUIAccess
    static ToggleButton create(LocalizeValue text, boolean selected) {
        ToggleButton button = UIInternal.get()._Components_toggleButton(text);
        button.setValue(selected, false);
        return button;
    }

    @Override
    Boolean getValue();
}
