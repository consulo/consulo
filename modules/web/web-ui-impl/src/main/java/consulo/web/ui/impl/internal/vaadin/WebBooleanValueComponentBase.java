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
package consulo.web.ui.impl.internal.vaadin;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.HasValue;
import consulo.ui.UIAccess;
import consulo.ui.ValueComponent;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.event.ValueComponentEvent;
import consulo.web.ui.impl.internal.base.FromVaadinComponentWrapper;
import consulo.web.ui.impl.internal.base.VaadinComponentDelegate;
import org.jspecify.annotations.Nullable;

import java.util.Objects;

/**
 * @author VISTALL
 * @since 2019-02-19
 */
public abstract class WebBooleanValueComponentBase<E extends Component & HasValue<?, Boolean> & FromVaadinComponentWrapper> extends VaadinComponentDelegate<E> implements ValueComponent<Boolean> {
    /**
     * A value written from java raises the browser's own event as well, and that event is the one reported -
     * without this the write would be reported twice, and a write which asked not to be reported once.
     */
    private boolean myFireListeners = true;

    @SuppressWarnings({"unchecked", "rawtypes"})
    public WebBooleanValueComponentBase(boolean value) {
        E component = getVaadinComponent();

        component.setValue(value);

        // the listener is what a press in the browser arrives through - reporting only the writes made from java
        // left a checkbox the user ticked telling nobody, and the pages which enable a field from one never saw it
        ((HasValue) component).addValueChangeListener(event -> {
            if (myFireListeners) {
                getListenerDispatcher(ValueComponentEvent.class)
                    .onEvent(new ValueComponentEvent<>(this, (Boolean) ((HasValue.ValueChangeEvent) event).getValue()));
            }
        });
    }

    @Override
    public @Nullable Boolean getValue() {
        return getVaadinComponent().getValue();
    }

    @RequiredUIAccess
    @Override
    public void setValue(Boolean value, boolean fireListeners) {
        UIAccess.assertIsUIThread();

        if (value == null) {
            throw new IllegalArgumentException();
        }

        if (Objects.equals(getVaadinComponent().getValue(), value)) {
            return;
        }

        setValueImpl(value, fireListeners);
    }

    @RequiredUIAccess
    protected void setValueImpl(@Nullable Boolean value, boolean fireEvents) {
        myFireListeners = fireEvents;
        try {
            getVaadinComponent().setValue(value);
        }
        finally {
            myFireListeners = true;
        }
    }
}
