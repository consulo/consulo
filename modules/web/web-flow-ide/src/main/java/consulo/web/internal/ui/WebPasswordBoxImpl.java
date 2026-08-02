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
package consulo.web.internal.ui;

import com.vaadin.flow.component.textfield.PasswordField;
import consulo.disposer.Disposable;
import consulo.ui.Component;
import consulo.ui.PasswordBox;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.web.internal.ui.base.FromVaadinComponentWrapper;
import consulo.web.internal.ui.base.VaadinComponentDelegate;
import org.jspecify.annotations.Nullable;

/**
 * @author VISTALL
 * @since 2026-08-02
 */
public class WebPasswordBoxImpl extends VaadinComponentDelegate<WebPasswordBoxImpl.Vaadin> implements PasswordBox {
    public class Vaadin extends PasswordField implements FromVaadinComponentWrapper {

        @Override
        public @Nullable Component toUIComponent() {
            return WebPasswordBoxImpl.this;
        }

    }

    public WebPasswordBoxImpl(String password) {
        setValue(password);
    }

    @Override
    public Disposable addValidator(Validator<String> validator) {
        return () -> {
        };
    }

    @RequiredUIAccess
    @Override
    public boolean validate() {
        return true;
    }

    @Nullable
    @Override
    public String getValue() {
        return toVaadinComponent().getValue();
    }

    @RequiredUIAccess
    @Override
    public void setValue(String value, boolean fireListeners) {
        toVaadinComponent().setValue(value);
    }

    @Override
    public Vaadin createVaadinComponent() {
        return new Vaadin();
    }
}
