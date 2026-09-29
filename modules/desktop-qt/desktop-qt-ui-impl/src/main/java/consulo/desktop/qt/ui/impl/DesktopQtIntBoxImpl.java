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
package consulo.desktop.qt.ui.impl;

import consulo.disposer.Disposable;
import consulo.localize.LocalizeValue;
import consulo.ui.IntBox;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.event.ValueComponentEvent;
import io.qt.widgets.QSpinBox;
import io.qt.widgets.QWidget;
import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * @author VISTALL
 * @since 2026-08-16
 */
public class DesktopQtIntBoxImpl extends QtComponentDelegate<QSpinBox> implements IntBox {
    private final List<Validator<Integer>> myValidators = new CopyOnWriteArrayList<>();

    private int myValue;
    private int myMin = Integer.MIN_VALUE;
    private int myMax = Integer.MAX_VALUE;
    private int myStep = 1;

    private boolean myFireListeners = true;

    public DesktopQtIntBoxImpl(int initValue) {
        myValue = initValue;
    }

    @Override
    protected QSpinBox createQt(QWidget parent) {
        return new QSpinBox(parent);
    }

    @Override
    @SuppressWarnings("unchecked")
    protected void initialize(QSpinBox component) {
        super.initialize(component);

        component.setRange(myMin, myMax);
        component.setSingleStep(myStep);
        component.setValue(myValue);
        myValue = component.value();

        component.valueChanged.connect(value -> {
            myValue = value;

            if (myFireListeners) {
                getListenerDispatcher(ValueComponentEvent.class)
                    .onEvent(new ValueComponentEvent(this, value, DesktopQtCurrentInput.current(component)));
            }
        });
    }

    @Override
    public void setPlaceholder(@Nullable LocalizeValue text) {
    }

    @Override
    @RequiredUIAccess
    public void setRange(int min, int max) {
        myMin = min;
        myMax = max;

        QSpinBox spinBox = myComponent;
        if (spinBox != null) {
            myFireListeners = false;
            try {
                spinBox.setRange(min, max);
            }
            finally {
                myFireListeners = true;
            }
            myValue = spinBox.value();
        }
        else {
            myValue = clamp(myValue);
        }
    }

    @Override
    public void setStep(int step) {
        myStep = step;

        QSpinBox spinBox = myComponent;
        if (spinBox != null) {
            spinBox.setSingleStep(step);
        }
    }

    @Override
    public Disposable addValidator(Validator<Integer> validator) {
        myValidators.add(validator);
        return () -> myValidators.remove(validator);
    }

    @Override
    @RequiredUIAccess
    public boolean validate() {
        Integer value = getValue();
        for (Validator<Integer> validator : myValidators) {
            if (validator.validateValue(value) != null) {
                return false;
            }
        }
        return true;
    }

    @Override
    public Integer getValue() {
        return myValue;
    }

    @Override
    @RequiredUIAccess
    @SuppressWarnings("unchecked")
    public void setValue(@Nullable Integer value, boolean fireListeners) {
        int newValue = clamp(value == null ? 0 : value);

        QSpinBox spinBox = myComponent;
        if (spinBox != null) {
            myFireListeners = fireListeners;
            try {
                spinBox.setValue(newValue);
            }
            finally {
                myFireListeners = true;
            }
            return;
        }

        boolean changed = myValue != newValue;
        myValue = newValue;
        if (fireListeners && changed) {
            getListenerDispatcher(ValueComponentEvent.class).onEvent(new ValueComponentEvent(this, newValue));
        }
    }

    private int clamp(int value) {
        return Math.min(Math.max(value, myMin), myMax);
    }
}
