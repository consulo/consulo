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
import consulo.ui.PasswordBox;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.util.lang.StringUtil;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * @author VISTALL
 * @since 2026-10-03
 */
public class HeadlessPasswordBox extends HeadlessValueComponentBase<String> implements PasswordBox {
    private final List<Validator<String>> myValidators = new ArrayList<>();
    private boolean myFocusable = true;

    public HeadlessPasswordBox(@Nullable String password) {
        super(StringUtil.notNullize(password));
    }

    @Override
    protected String normalize(@Nullable String value) {
        return StringUtil.notNullize(value);
    }

    @Override
    public String getValue() {
        return normalize(super.getValue());
    }

    @Override
    public Disposable addValidator(Validator<String> validator) {
        myValidators.add(validator);
        return () -> myValidators.remove(validator);
    }

    @Override
    @RequiredUIAccess
    public boolean validate() {
        String value = getValue();
        for (Validator<String> validator : List.copyOf(myValidators)) {
            if (validator.validateValue(value) != null) {
                return false;
            }
        }
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
        myFocusable = focusable;
    }

    @Override
    public boolean isFocusable() {
        return myFocusable;
    }
}
