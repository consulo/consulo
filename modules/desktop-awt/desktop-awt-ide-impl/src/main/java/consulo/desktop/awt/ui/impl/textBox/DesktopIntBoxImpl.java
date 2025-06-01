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
package consulo.desktop.awt.ui.impl.textBox;

import consulo.desktop.awt.facade.FromSwingComponentWrapper;
import consulo.desktop.awt.ui.impl.validableComponent.SwingValidableComponent;
import consulo.localize.LocalizeValue;
import consulo.ui.Component;
import consulo.ui.IntBox;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.event.ValueComponentEvent;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import javax.swing.*;
import java.util.Objects;

/**
 * @author VISTALL
 * @since 2020-04-19
 */
public class DesktopIntBoxImpl extends SwingValidableComponent<Integer, DesktopIntBoxImpl.MyJSpinner> implements IntBox {
    class MyJSpinner extends JSpinner implements FromSwingComponentWrapper {
        public void setRange(int value, int min, int max) {
            setModel(new SpinnerNumberModel(value, min, max, 1));
        }

        @Nonnull
        @Override
        public Component toUIComponent() {
            return DesktopIntBoxImpl.this;
        }
    }

    public DesktopIntBoxImpl(int value) {
        MyJSpinner field = new MyJSpinner();
        field.setRange(value, Integer.MIN_VALUE, Integer.MAX_VALUE);
        field.addChangeListener(e -> {
            MyJSpinner source = (MyJSpinner) e.getSource();
            valueChanged(getValue(source));
        });
        initialize(field);
    }

    private static int getValue(JSpinner spinner) {
        Object value = spinner.getValue();
        if (value instanceof Number number) {
            return number.intValue();
        }
        return 0;
    }

    @Override
    @RequiredUIAccess
    public void setRange(int min, int max) {
        toAWTComponent().setRange(getValue(), min, max);
    }

    @Override
    public void setPlaceholder(@Nullable LocalizeValue text) {
    }

    @SuppressWarnings("unchecked")
    @RequiredUIAccess
    private void valueChanged(int value) {
        dataObject().getDispatcher(ValueComponentEvent.class).onEvent(new ValueComponentEvent(this, value));
    }

    @Override
    public Integer getValue() {
        return getValue(toAWTComponent());
    }

    @Override
    @RequiredUIAccess
    public void setValue(Integer value, boolean fireListeners) {
        Integer valueNonNull = Objects.requireNonNull(value, "Value must be not null");
        toAWTComponent().setValue(valueNonNull);
    }
}
