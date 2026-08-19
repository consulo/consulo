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
import consulo.ui.Component;
import consulo.ui.TextBox;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.event.ValueComponentEvent;
import consulo.util.lang.StringUtil;
import io.qt.widgets.QLineEdit;
import io.qt.widgets.QWidget;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * @author VISTALL
 * @since 2026-08-16
 */
@SuppressWarnings({"unchecked", "rawtypes"})
public class DesktopQtTextBoxImpl extends QtComponentDelegate<QLineEdit> implements TextBox {
    private String myText;

    private boolean myEditable = true;

    private boolean myFireListeners = true;

    private LocalizeValue myPlaceholder = LocalizeValue.empty();

    private int myVisibleLength = -1;

    private final List<Validator<String>> myValidators = new ArrayList<>();

    public DesktopQtTextBoxImpl(String text) {
        myText = StringUtil.notNullize(text);
    }

    @Override
    protected QLineEdit createQt(QWidget parent) {
        return new QLineEdit(parent);
    }

    @Override
    protected void initialize(QLineEdit component) {
        super.initialize(component);

        component.setText(myText);
        component.setReadOnly(!myEditable);

        applyPlaceholder();
        applyVisibleLength();

        component.textChanged.connect(text -> {
            myText = StringUtil.notNullize(text);

            if (myFireListeners) {
                getListenerDispatcher(ValueComponentEvent.class)
                    .onEvent(new ValueComponentEvent(this, myText, DesktopQtCurrentInput.current(component)));
            }
        });
    }

    @Override
    public void setPlaceholder(LocalizeValue text) {
        myPlaceholder = text == null ? LocalizeValue.empty() : text;

        applyPlaceholder();
    }

    private void applyPlaceholder() {
        if (myComponent != null) {
            myComponent.setPlaceholderText(myPlaceholder.get());
        }
    }

    @Override
    public void setVisibleLength(int columns) {
        myVisibleLength = columns;

        applyVisibleLength();
    }

    /**
     * The api asks for a width in characters, which qt has no notion of - the average advance of the font is what
     * the awt text field measures a column by, and the frame the style draws around the text is added on top.
     */
    private void applyVisibleLength() {
        if (myComponent == null || myVisibleLength <= 0) {
            return;
        }

        int columnWidth = myComponent.fontMetrics().horizontalAdvance("m");

        myComponent.setMinimumWidth(columnWidth * myVisibleLength + myComponent.textMargins().left()
            + myComponent.textMargins().right() + 8);
    }

    @Override
    public void selectAll() {
        if (myComponent != null) {
            myComponent.selectAll();
        }
    }

    @Override
    public void select(int from, int to) {
        if (myComponent != null) {
            myComponent.setSelection(from, to - from);
        }
    }

    @Override
    public void moveCaretTo(int index) {
        if (myComponent != null) {
            myComponent.setCursorPosition(index);
        }
    }

    @Override
    public void setEditable(boolean editable) {
        myEditable = editable;

        if (myComponent != null) {
            myComponent.setReadOnly(!editable);
        }
    }

    @Override
    public boolean isEditable() {
        return myEditable;
    }

    @Override
    public Disposable addValidator(Validator<String> validator) {
        myValidators.add(validator);

        return () -> myValidators.remove(validator);
    }

    @RequiredUIAccess
    @Override
    public boolean validate() {
        for (Validator<String> validator : myValidators) {
            if (validator.validateValue(getValue()) != null) {
                return false;
            }
        }

        return true;
    }

    @Override
    public @Nullable String getValue() {
        return myText;
    }

    @RequiredUIAccess
    @Override
    public void setValue(String value, boolean fireListeners) {
        myText = StringUtil.notNullize(value);

        if (myComponent != null) {
            myFireListeners = fireListeners;
            try {
                myComponent.setText(myText);
            }
            finally {
                myFireListeners = true;
            }
        }
    }

    @Override
    public void setSuffixComponent(@Nullable Component suffixComponent) {
    }

    @Override
    public @Nullable Component getSuffixComponent() {
        return null;
    }
}
