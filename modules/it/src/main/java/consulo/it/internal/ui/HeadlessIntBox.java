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
import consulo.localize.LocalizeValue;
import consulo.ui.IntBox;

/**
 * Dummy-but-creatable headless {@link IntBox}.
 *
 * @author VISTALL
 */
public class HeadlessIntBox extends HeadlessValueComponentBase<Integer> implements IntBox {
    public HeadlessIntBox(int value) {
        super(value);
    }

    @Override
    public void setPlaceholder(LocalizeValue text) {
    }

    @Override
    public void setRange(int min, int max) {
    }

    @Override
    public Disposable addValidator(Validator<Integer> validator) {
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
