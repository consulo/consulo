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

import consulo.localize.LocalizeValue;
import consulo.ui.CheckBox;
import consulo.ui.CheckBoxStyle;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.event.ValueComponentEvent;
import io.qt.widgets.QCheckBox;
import io.qt.widgets.QWidget;

import java.util.EnumSet;
import java.util.Set;

/**
 * @author VISTALL
 * @since 2026-08-16
 */
@SuppressWarnings({"unchecked", "rawtypes"})
public class DesktopQtCheckBoxImpl extends QtComponentDelegate<QCheckBox> implements CheckBox {
    private LocalizeValue myText = LocalizeValue.empty();

    private boolean myValue;

    private boolean myFireListeners = true;

    private final Set<CheckBoxStyle> myStyles = EnumSet.noneOf(CheckBoxStyle.class);

    @Override
    protected QCheckBox createQt(QWidget parent) {
        return new QCheckBox(parent);
    }

    @Override
    protected void initialize(QCheckBox component) {
        super.initialize(component);

        component.setText(QtMnemonic.withMnemonic(myText));
        component.setChecked(myValue);

        for (CheckBoxStyle style : myStyles) {
            applyStyle(style);
        }

        component.toggled.connect(checked -> {
            myValue = checked;

            if (myFireListeners) {
                getListenerDispatcher(ValueComponentEvent.class).onEvent(new ValueComponentEvent(this, myValue));
            }
        });
    }

    @Override
    public Boolean getValue() {
        return myValue;
    }

    @RequiredUIAccess
    @Override
    public void setValue(Boolean value, boolean fireListeners) {
        myValue = value != null && value;

        if (myComponent != null) {
            myFireListeners = fireListeners;
            try {
                myComponent.setChecked(myValue);
            }
            finally {
                myFireListeners = true;
            }
        }
    }

    @Override
    public LocalizeValue getLabelText() {
        return myText;
    }

    @RequiredUIAccess
    @Override
    public void setLabelText(LocalizeValue labelText) {
        myText = labelText;

        if (myComponent != null) {
            myComponent.setText(QtMnemonic.withMnemonic(myText));
        }
    }

    @Override
    public void addStyle(CheckBoxStyle style) {
        myStyles.add(style);

        applyStyle(style);
    }

    private void applyStyle(CheckBoxStyle style) {
        if (myComponent == null) {
            return;
        }

        switch (style) {
            case TRANSPARENT_BACKGROUND -> myComponent.setAutoFillBackground(false);
        }
    }
}
