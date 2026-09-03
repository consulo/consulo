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
import io.qt.widgets.QSpinBox;
import io.qt.widgets.QWidget;
import org.jspecify.annotations.Nullable;

/**
 * @author VISTALL
 * @since 2026-08-16
 */
public class DesktopQtIntBoxImpl extends QtComponentDelegate<QSpinBox> implements IntBox {
    private int myInitValue;
    private int myInitMin;
    private int myInitMax = Integer.MAX_VALUE;
    private int myInitStep = 1;

    public DesktopQtIntBoxImpl(int initValue) {
        myInitValue = initValue;
    }

    @Override
    public void setPlaceholder(@Nullable LocalizeValue text) {
    }

    @Override
    public void setRange(int min, int max) {
        QSpinBox spinBox = toQtComponent();
        if (spinBox != null) {
            spinBox.setMinimum(min);
            spinBox.setMaximum(max);
        }
        else {
            myInitMin = min;
            myInitMax = max;
        }
    }

    @Override
    public void setStep(int step) {
        QSpinBox spinBox = toQtComponent();
        if (spinBox != null) {
            spinBox.setSingleStep(step);
        }
        else {
            myInitStep = step;
        }
    }

    @Override
    public Disposable addValidator(Validator<Integer> validator) {
        return null;
    }

    @Override
    @RequiredUIAccess
    public boolean validate() {
        return true;
    }

    @Override
    public @Nullable Integer getValue() {
        QSpinBox spinBox = toQtComponent();
        return spinBox == null ? myInitValue : spinBox.value();
    }

    @Override
    @RequiredUIAccess
    public void setValue(Integer value, boolean fireListeners) {
        QSpinBox spinBox = toQtComponent();
        if (spinBox != null) {
            spinBox.setValue(value == null ? 0 : value);
        }
        else {
            myInitValue = value == null ? 0 : value;
        }
    }

    @Override
    protected QSpinBox createQt(QWidget parent) {
        return new QSpinBox(parent);
    }

    @Override
    protected void initialize(QSpinBox component) {
        super.initialize(component);

        component.setMinimum(myInitMin);
        component.setMaximum(myInitMax);
        component.setSingleStep(myInitStep);
        component.setValue(myInitValue);
    }
}
