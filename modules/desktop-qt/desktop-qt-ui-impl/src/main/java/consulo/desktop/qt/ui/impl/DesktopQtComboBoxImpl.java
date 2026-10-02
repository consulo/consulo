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

import consulo.ui.ComboBox;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.event.ValueComponentEvent;
import consulo.ui.model.FlatDataModel;
import io.qt.widgets.QComboBox;
import io.qt.widgets.QWidget;
import org.jspecify.annotations.Nullable;

/**
 * @author VISTALL
 * @since 2026-08-16
 */
public class DesktopQtComboBoxImpl<E> extends DesktopQtComboBoxBaseImpl<E, QComboBox> implements ComboBox<E> {
    private int mySelectedIndex = 0;
    private boolean myFireListeners = true;

    public DesktopQtComboBoxImpl(FlatDataModel<E> model) {
        super(model);
    }

    @Override
    protected QComboBox createQt(QWidget parent) {
        return new DesktopQtComboBox(parent);
    }

    @Override
    protected void initialize(QComboBox component) {
        component.currentIndexChanged.connect(index -> {
            // emptying the widget reports a choice of nothing, which would take the stored index with it
            if (myRebuilding) {
                return;
            }

            mySelectedIndex = index;

            if (myFireListeners) {
                getListenerDispatcher(ValueComponentEvent.class)
                    .onEvent(new ValueComponentEvent(this, getValue(), DesktopQtCurrentInput.current(component)));
            }
        });

        rebuild(component);
    }

    @Override
    protected boolean isSelectedItem(int index, E element) {
        return index == mySelectedIndex;
    }

    @Override
    protected void afterRebuild(QComboBox component) {
        int count = component.count();
        if (count > 0 && mySelectedIndex >= count) {
            mySelectedIndex = count - 1;
        }

        component.setCurrentIndex(mySelectedIndex >= 0 && mySelectedIndex < count ? mySelectedIndex : -1);
    }

    @Override
    public void setValueByIndex(int index) {
        setValueByIndex(index, true);
    }

    private void setValueByIndex(int index, boolean fireListeners) {
        int oldIndex = mySelectedIndex;
        mySelectedIndex = index;

        QComboBox component = myComponent;
        if (component == null) {
            if (fireListeners && oldIndex != index) {
                getListenerDispatcher(ValueComponentEvent.class).onEvent(new ValueComponentEvent(this, getValue()));
            }
            return;
        }

        // the widget answers a set index with the same signal a click raises, so the guard is what keeps a
        // programmatic set - a reset writing the stored value back - from reading as a user's choice
        myFireListeners = fireListeners;
        try {
            component.setCurrentIndex(index);
        }
        finally {
            myFireListeners = true;
        }
    }

    @Override
    public @Nullable E getValue() {
        return mySelectedIndex >= 0 && mySelectedIndex < myModel.getSize() ? myModel.get(mySelectedIndex) : null;
    }

    @Override
    @RequiredUIAccess
    public void setValue(E value, boolean fireListeners) {
        setValueByIndex(myModel.indexOf(value), fireListeners);
    }
}
