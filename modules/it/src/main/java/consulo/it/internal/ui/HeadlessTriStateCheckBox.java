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
import consulo.ui.CheckBoxStyle;
import consulo.ui.TriStateCheckBox;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.util.lang.ThreeState;
import org.jspecify.annotations.Nullable;

/**
 * @author VISTALL
 * @since 2026-10-03
 */
public class HeadlessTriStateCheckBox extends HeadlessValueComponentBase<ThreeState> implements TriStateCheckBox {
    private LocalizeValue myLabelText = LocalizeValue.empty();
    private boolean myUnsureEnabled = true;
    private boolean myFocusable = true;

    public HeadlessTriStateCheckBox() {
        super(ThreeState.UNSURE);
    }

    @Override
    protected ThreeState normalize(@Nullable ThreeState value) {
        return value == null ? ThreeState.UNSURE : value;
    }

    @Override
    public ThreeState getValue() {
        return normalize(super.getValue());
    }

    @Override
    public boolean isUnsureEnabled() {
        return myUnsureEnabled;
    }

    @Override
    @RequiredUIAccess
    public void setUnsureEnabled(boolean unsureEnabled) {
        myUnsureEnabled = unsureEnabled;
    }

    @Override
    public LocalizeValue getLabelText() {
        return myLabelText;
    }

    @Override
    @RequiredUIAccess
    public void setLabelText(LocalizeValue labelText) {
        myLabelText = labelText;
    }

    @Override
    public void addStyle(CheckBoxStyle style) {
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
