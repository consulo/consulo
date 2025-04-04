/*
 * Copyright 2013-2016 consulo.io
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
import consulo.desktop.awt.ui.impl.validableComponent.DocumentSwingValidator;
import consulo.localize.LocalizeValue;
import consulo.ui.Component;
import consulo.ui.TextBox;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.color.ColorValue;
import consulo.ui.event.ValueComponentEvent;
import consulo.ui.ex.awt.JBTextField;
import consulo.ui.ex.awt.event.DocumentAdapter;
import consulo.ui.ex.awt.internal.AWTHasSuffixComponent;
import consulo.ui.ex.awtUnsafe.TargetAWT;
import consulo.util.lang.StringUtil;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import javax.swing.*;
import javax.swing.event.DocumentEvent;

/**
 * @author VISTALL
 * @since 19-Nov-16.
 */
public class DesktopTextBoxImpl extends DocumentSwingValidator<String, DesktopTextBoxImpl.MyJBTextField> implements TextBox, TextBoxWithTextField {
    private static class Listener extends DocumentAdapter {
        private DesktopTextBoxImpl myTextField;

        public Listener(DesktopTextBoxImpl textField) {
            myTextField = textField;
        }

        @Override
        @RequiredUIAccess
        protected void textChanged(DocumentEvent e) {
            myTextField.valueChanged();
        }
    }

    class MyJBTextField extends JBTextField implements FromSwingComponentWrapper {
        private ColorValue myForegroundColor;

        @Override
        public void updateUI() {
            super.updateUI();

            updateForegroundColor();
        }

        public void setForegroundColor(@Nullable ColorValue color) {
            myForegroundColor = color;

            updateForegroundColor();
        }

        private void updateForegroundColor() {
            if (myForegroundColor == null) {
                setForeground(null);
            }
            else {
                setForeground(TargetAWT.to(myForegroundColor));
            }
        }

        @Nonnull
        @Override
        public Component toUIComponent() {
            return DesktopTextBoxImpl.this;
        }
    }

    @RequiredUIAccess
    public DesktopTextBoxImpl(String text) {
        MyJBTextField field = new MyJBTextField();
        TextFieldPlaceholderFunction.install(field);
        initialize(field);
        addDocumentListenerForValidator(field.getDocument());

        field.getDocument().addDocumentListener(new Listener(this));
        setValue(text);
    }

    @Override
    public void setSuffixComponent(@Nullable Component suffixComponent) {
        AWTHasSuffixComponent.setSuffixComponent(toAWTComponent(), TargetAWT.to(suffixComponent));
    }

    @Nullable
    @Override
    public Component getSuffixComponent() {
        Object object = toAWTComponent().getClientProperty("JTextField.trailingComponent");
        if (object instanceof JComponent jComponent) {
            return TargetAWT.from(jComponent);
        }
        return null;
    }

    @Override
    public void setForegroundColor(@Nullable ColorValue foreground) {
        toAWTComponent().setForegroundColor(foreground);
    }

    @Nullable
    @Override
    public ColorValue getForegroundColor() {
        return toAWTComponent().myForegroundColor;
    }

    @Nonnull
    @Override
    public JTextField getTextField() {
        return toAWTComponent();
    }

    @Override
    public void setPlaceholder(@Nonnull LocalizeValue text) {
        MyJBTextField field = toAWTComponent();
        field.putClientProperty("JTextField.placeholderText", text == LocalizeValue.empty() ? null : text.getValue());
    }

    @Override
    public void setVisibleLength(int columns) {
        toAWTComponent().setColumns(columns);
    }

    @Override
    public void selectAll() {
        toAWTComponent().selectAll();
    }

    @Override
    public void select(int from, int to) {
        toAWTComponent().setCaretPosition(from);
        toAWTComponent().moveCaretPosition(to);
    }

    @Override
    public void moveCaretTo(int index) {
        toAWTComponent().setCaretPosition(index);
        toAWTComponent().moveCaretPosition(index);
    }

    @Override
    public void setEditable(boolean editable) {
        toAWTComponent().setEditable(editable);
    }

    @Override
    public boolean isEditable() {
        return toAWTComponent().isEditable();
    }

    @SuppressWarnings("unchecked")
    @RequiredUIAccess
    private void valueChanged() {
        dataObject().getDispatcher(ValueComponentEvent.class).onEvent(new ValueComponentEvent(this, getValue()));
    }

    @Override
    public String getValue() {
        return StringUtil.nullize(toAWTComponent().getText());
    }

    @RequiredUIAccess
    @Override
    public void setValue(String value, boolean fireListeners) {
        toAWTComponent().setText(value);
    }
}
