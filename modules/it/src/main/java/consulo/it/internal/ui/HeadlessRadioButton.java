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
import consulo.ui.RadioButton;
import consulo.ui.annotation.RequiredUIAccess;

/**
 * @author VISTALL
 * @since 2026-10-03
 */
public class HeadlessRadioButton extends HeadlessBooleanValueComponentBase implements RadioButton {
    private LocalizeValue myLabelText;
    private boolean myFocusable = true;

    public HeadlessRadioButton(LocalizeValue text, boolean selected) {
        super(selected);
        myLabelText = text;
    }

    @Override
    public LocalizeValue getLabelText() {
        return myLabelText;
    }

    @Override
    @RequiredUIAccess
    public void setLabelText(LocalizeValue text) {
        myLabelText = text;
    }

    @Override
    public boolean hasFocus() {
        return false;
    }

    @Override
    public void focus() {
    }

    @Override
    public void setFocusable(boolean focusable) {
        myFocusable = focusable;
    }

    @Override
    public boolean isFocusable() {
        return myFocusable;
    }
}
