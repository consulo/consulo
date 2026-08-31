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
package consulo.web.ui.impl.internal;

import com.vaadin.flow.component.slider.IntegerSlider;
import consulo.ui.Component;
import consulo.ui.IntSlider;
import consulo.ui.UIAccess;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.event.ValueComponentEvent;
import consulo.web.ui.impl.internal.base.FromVaadinComponentWrapper;
import consulo.web.ui.impl.internal.base.VaadinComponentDelegate;
import org.jspecify.annotations.Nullable;

/**
 * @author VISTALL
 * @since 2026-08-23
 */
public class WebIntSliderImpl extends VaadinComponentDelegate<WebIntSliderImpl.Vaadin> implements IntSlider {
    public class Vaadin extends IntegerSlider implements FromVaadinComponentWrapper {
        @Override
        public @Nullable Component toUIComponent() {
            return WebIntSliderImpl.this;
        }
    }

    private int myMin;
    private int myMax;

    @SuppressWarnings("unchecked")
    public WebIntSliderImpl(int min, int max, int value) {
        myMin = min;
        myMax = max;

        Vaadin component = getVaadinComponent();
        component.setMin(min);
        component.setMax(max);
        component.setValue(value);

        component.addValueChangeListener(event -> {
            if (event.isFromClient()) {
                getListenerDispatcher(ValueComponentEvent.class).onEvent(new ValueComponentEvent<>(this, event.getValue()));
            }
        });
    }

    @Override
    public Vaadin createVaadinComponent() {
        return new Vaadin();
    }

    @Override
    public Integer getValue() {
        return getVaadinComponent().getValue();
    }

    @RequiredUIAccess
    @Override
    @SuppressWarnings("unchecked")
    public void setValue(@Nullable Integer value, boolean fireListeners) {
        UIAccess.assertIsUIThread();

        int newValue = value == null ? myMin : value;

        getVaadinComponent().setValue(newValue);

        if (fireListeners) {
            getListenerDispatcher(ValueComponentEvent.class).onEvent(new ValueComponentEvent<>(this, newValue));
        }
    }

    @Override
    public void setRange(int min, int max) {
        myMin = min;
        myMax = max;

        getVaadinComponent().setMin(min);
        getVaadinComponent().setMax(max);
    }
}
