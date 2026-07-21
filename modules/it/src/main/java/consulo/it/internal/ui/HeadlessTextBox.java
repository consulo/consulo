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

import consulo.disposer.Disposable;
import consulo.ui.Component;
import consulo.ui.TextBox;
import org.jspecify.annotations.Nullable;

/**
 * Dummy-but-creatable headless {@link TextBox}.
 *
 * @author VISTALL
 */
public class HeadlessTextBox extends HeadlessValueComponentBase<String> implements TextBox {
    private boolean myEditable = true;
    private @Nullable Component mySuffixComponent;

    public HeadlessTextBox(@Nullable String text) {
        super(text);
    }

    @Override
    public void selectAll() {
    }

    @Override
    public void setEditable(boolean editable) {
        myEditable = editable;
    }

    @Override
    public boolean isEditable() {
        return myEditable;
    }

    @Override
    public void setSuffixComponent(@Nullable Component suffixComponent) {
        mySuffixComponent = suffixComponent;
    }

    @Override
    public @Nullable Component getSuffixComponent() {
        return mySuffixComponent;
    }

    @Override
    public Disposable addValidator(Validator<String> validator) {
        return () -> {
        };
    }

    @Override
    public boolean validate() {
        return true;
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
    }

    @Override
    public boolean isFocusable() {
        return false;
    }
}
