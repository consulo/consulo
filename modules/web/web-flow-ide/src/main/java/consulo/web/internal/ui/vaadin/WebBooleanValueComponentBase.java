/*
 * Copyright 2013-2019 consulo.io
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
package consulo.web.internal.ui.vaadin;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.HasValue;
import consulo.ui.UIAccess;
import consulo.ui.ValueComponent;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.event.ValueComponentEvent;
import consulo.web.internal.ui.base.FromVaadinComponentWrapper;
import consulo.web.internal.ui.base.VaadinComponentDelegate;
import jakarta.annotation.Nullable;

/**
 * @author VISTALL
 * @since 2019-02-19
 */
public abstract class WebBooleanValueComponentBase<E extends Component & HasValue<?, Boolean> & FromVaadinComponentWrapper> extends VaadinComponentDelegate<E> implements ValueComponent<Boolean> {
    public WebBooleanValueComponentBase(boolean value) {
        getVaadinComponent().setValue(value);
    }

    @Nullable
    @Override
    public Boolean getValue() {
        return getVaadinComponent().getValue();
    }

    @RequiredUIAccess
    @Override
    public void setValue(Boolean value, boolean fireListeners) {
        UIAccess.assertIsUIThread();

        if (value == null) {
            throw new IllegalArgumentException();
        }

        if (getVaadinComponent().getValue() == value) {
            return;
        }

        setValueImpl(value, fireListeners);
    }

    @RequiredUIAccess
    @SuppressWarnings("unchecked")
    protected void setValueImpl(@Nullable Boolean value, boolean fireEvents) {
        getVaadinComponent().setValue(value);

        if (fireEvents) {
            getListenerDispatcher(ValueComponentEvent.class).onEvent(new ValueComponentEvent<>(this, value));
        }
    }
}
