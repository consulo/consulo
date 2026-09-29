/*
 * Copyright 2013-2020 consulo.io
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

import com.vaadin.flow.component.textfield.IntegerField;
import consulo.disposer.Disposable;
import consulo.localize.LocalizeValue;
import consulo.ui.Component;
import consulo.ui.IntBox;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.event.ValueComponentEvent;
import consulo.web.ui.impl.internal.base.FromVaadinComponentWrapper;
import consulo.web.ui.impl.internal.base.VaadinComponentDelegate;
import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * @author VISTALL
 * @since 2020-05-10
 */
public class WebIntBoxImpl extends VaadinComponentDelegate<WebIntBoxImpl.Vaadin> implements IntBox {
    public class Vaadin extends IntegerField implements FromVaadinComponentWrapper {
        @Override
        public @Nullable Component toUIComponent() {
            return WebIntBoxImpl.this;
        }
    }

    private final List<Validator<Integer>> myValidators = new CopyOnWriteArrayList<>();

    private int myMin = Integer.MIN_VALUE;
    private int myMax = Integer.MAX_VALUE;

    private boolean myFireListeners = true;

    @RequiredUIAccess
    public WebIntBoxImpl(int value) {
        Vaadin field = getVaadinComponent();
        field.setStepButtonsVisible(true);
        field.setValue(value);
        field.addValueChangeListener(event -> onValueChanged(event.getValue()));
    }

    @Override
    public Vaadin createVaadinComponent() {
        return new Vaadin();
    }

    @SuppressWarnings("unchecked")
    @RequiredUIAccess
    private void onValueChanged(@Nullable Integer value) {
        if (value != null) {
            int clamped = clamp(value);
            if (clamped != value) {
                getVaadinComponent().setValue(clamped);
                return;
            }
        }

        if (myFireListeners) {
            getListenerDispatcher(ValueComponentEvent.class).onEvent(new ValueComponentEvent(this, value));
        }
    }

    private int clamp(int value) {
        return Math.min(Math.max(value, myMin), myMax);
    }

    @Override
    public @Nullable Integer getValue() {
        return getVaadinComponent().getValue();
    }

    @Override
    @RequiredUIAccess
    public void setValue(@Nullable Integer value, boolean fireListeners) {
        myFireListeners = fireListeners;
        try {
            getVaadinComponent().setValue(value);
        }
        finally {
            myFireListeners = true;
        }
    }

    @Override
    public void setPlaceholder(LocalizeValue text) {
        getVaadinComponent().setPlaceholder(text.isEmpty() ? null : text.get());
    }

    @Override
    @RequiredUIAccess
    public void setRange(int min, int max) {
        myMin = min;
        myMax = max;

        Vaadin field = getVaadinComponent();
        field.setMin(min);
        field.setMax(max);

        Integer value = field.getValue();
        if (value != null && clamp(value) != value) {
            setValue(clamp(value), false);
        }
    }

    @Override
    public void setStep(int step) {
        getVaadinComponent().setStep(step);
    }

    @Override
    public Disposable addValidator(Validator<Integer> validator) {
        myValidators.add(validator);
        return () -> myValidators.remove(validator);
    }

    @Override
    @RequiredUIAccess
    public boolean validate() {
        Vaadin field = getVaadinComponent();
        Integer value = getValue();
        for (Validator<Integer> validator : myValidators) {
            ValidationInfo info = validator.validateValue(value);
            if (info != null) {
                field.setErrorMessage(info.getMessage());
                field.setInvalid(true);
                return false;
            }
        }

        field.setInvalid(false);
        field.setErrorMessage(null);
        return true;
    }
}
