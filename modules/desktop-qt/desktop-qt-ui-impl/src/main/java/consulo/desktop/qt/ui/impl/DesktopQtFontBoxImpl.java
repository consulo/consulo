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

import consulo.ui.FontBox;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.event.ValueComponentEvent;
import io.qt.widgets.QFontComboBox;
import io.qt.widgets.QWidget;
import org.jspecify.annotations.Nullable;

/**
 * Qt has this widget natively, including the per-row preview and the monospaced filter.
 *
 * @author VISTALL
 * @since 2026-09-03
 */
@SuppressWarnings({"unchecked", "rawtypes"})
public class DesktopQtFontBoxImpl extends QtComponentDelegate<QFontComboBox> implements FontBox {
    private @Nullable String myValue;
    private boolean myMonospacedOnly;
    private boolean myFireListeners = true;

    @Override
    protected QFontComboBox createQt(QWidget parent) {
        return new QFontComboBox(parent);
    }

    @Override
    protected void initialize(QFontComboBox component) {
        super.initialize(component);

        applyFilter(component);

        if (myValue != null) {
            component.setCurrentText(myValue);
        }
        else {
            myValue = component.currentText();
        }

        component.currentTextChanged.connect(text -> {
            myValue = text;

            if (myFireListeners) {
                fireListeners(component);
            }
        });
    }

    private void applyFilter(QFontComboBox component) {
        component.setFontFilters(myMonospacedOnly
            ? QFontComboBox.FontFilter.MonospacedFonts
            : QFontComboBox.FontFilter.AllFonts);
    }

    @Override
    public @Nullable String getValue() {
        QFontComboBox component = toQtComponent();
        return component == null ? myValue : component.currentText();
    }

    @RequiredUIAccess
    @Override
    public void setValue(@Nullable String value, boolean fireListeners) {
        myValue = value;

        QFontComboBox component = toQtComponent();
        if (component == null) {
            return;
        }

        // the widget answers a programmatic set with the same signal a choice raises, so the guard is
        // what keeps a reset writing the stored value back from reading as the user's choice
        myFireListeners = fireListeners;
        try {
            component.setCurrentText(value == null ? "" : value);
        }
        finally {
            myFireListeners = true;
        }
    }

    @Override
    public void setMonospacedOnly(boolean monospacedOnly) {
        if (myMonospacedOnly == monospacedOnly) {
            return;
        }

        myMonospacedOnly = monospacedOnly;

        QFontComboBox component = toQtComponent();
        if (component != null) {
            applyFilter(component);
        }
    }

    @Override
    public boolean isMonospacedOnly() {
        return myMonospacedOnly;
    }

    @RequiredUIAccess
    private void fireListeners(QFontComboBox component) {
        getListenerDispatcher(ValueComponentEvent.class)
            .onEvent(new ValueComponentEvent(this, getValue(), DesktopQtCurrentInput.current(component)));
    }
}
