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

import consulo.ui.IntSlider;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.event.ValueComponentEvent;
import io.qt.core.Qt;
import io.qt.widgets.QSlider;
import io.qt.widgets.QWidget;
import org.jspecify.annotations.Nullable;

/**
 * @author VISTALL
 * @since 2026-08-23
 */
@SuppressWarnings({"unchecked", "rawtypes"})
public class DesktopQtIntSliderImpl extends QtComponentDelegate<QSlider> implements IntSlider {
    private int myMin;
    private int myMax;
    private int myValue;

    private boolean myFireListeners = true;

    public DesktopQtIntSliderImpl(int min, int max, int value) {
        myMin = min;
        myMax = max;
        myValue = value;
    }

    @Override
    protected QSlider createQt(QWidget parent) {
        return new QSlider(Qt.Orientation.Horizontal, parent);
    }

    @Override
    protected void initialize(QSlider component) {
        super.initialize(component);

        component.setRange(myMin, myMax);
        component.setValue(myValue);

        component.valueChanged.connect(value -> {
            myValue = value;

            if (myFireListeners) {
                getListenerDispatcher(ValueComponentEvent.class)
                    .onEvent(new ValueComponentEvent(this, myValue, DesktopQtCurrentInput.current(component)));
            }
        });
    }

    @Override
    public Integer getValue() {
        return myValue;
    }

    @RequiredUIAccess
    @Override
    public void setValue(@Nullable Integer value, boolean fireListeners) {
        myValue = value == null ? myMin : value;

        if (myComponent != null) {
            myFireListeners = fireListeners;
            try {
                myComponent.setValue(myValue);
            }
            finally {
                myFireListeners = true;
            }
        }
    }

    @Override
    public void setRange(int min, int max) {
        myMin = min;
        myMax = max;

        if (myComponent != null) {
            myComponent.setRange(min, max);
        }
    }
}
