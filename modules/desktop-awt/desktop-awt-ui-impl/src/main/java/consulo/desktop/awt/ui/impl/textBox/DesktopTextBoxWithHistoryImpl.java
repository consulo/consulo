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

import consulo.desktop.awt.ui.impl.event.DesktopAWTInputDetails;
import consulo.desktop.awt.ui.impl.facade.FromSwingComponentWrapper;
import consulo.desktop.awt.ui.impl.validableComponent.DocumentSwingValidator;
import consulo.localize.LocalizeValue;
import consulo.ui.Component;
import consulo.ui.TextBoxWithHistory;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.event.ValueComponentEvent;
import consulo.ui.ex.awt.TextFieldWithHistory;
import consulo.ui.ex.awt.event.DocumentAdapter;
import consulo.ui.ex.awt.internal.AWTHasSuffixComponent;
import consulo.ui.ex.awtUnsafe.TargetAWT;
import consulo.util.lang.StringUtil;
import org.jspecify.annotations.Nullable;

import javax.swing.ComboBoxEditor;
import javax.swing.event.DocumentEvent;
import javax.swing.text.Document;
import java.util.List;

/**
 * @author VISTALL
 * @since 2020-08-24
 */
public class DesktopTextBoxWithHistoryImpl extends DocumentSwingValidator<String, DesktopTextBoxWithHistoryImpl.MyTextFieldWithHistory> implements TextBoxWithHistory {
    public class MyTextFieldWithHistory extends TextFieldWithHistory implements FromSwingComponentWrapper {
        private boolean myKeepEditorText;

        @Override
        public Component toUIComponent() {
            return DesktopTextBoxWithHistoryImpl.this;
        }

        @Override
        public void setPopupVisible(boolean visible) {
            if (visible && !getTextEditor().isEditable()) {
                return;
            }

            super.setPopupVisible(visible);
        }

        @Override
        public void configureEditor(ComboBoxEditor editor, Object item) {
            if (myKeepEditorText) {
                return;
            }

            replaceText(this, myFireListeners, () -> super.configureEditor(editor, item));
        }

        public void setHistoryKeepingText(List<String> history) {
            myKeepEditorText = true;
            try {
                setHistory(history);
            }
            finally {
                myKeepEditorText = false;
            }
        }
    }

    private final String myInitText;

    private boolean myFireListeners = true;

    public DesktopTextBoxWithHistoryImpl(String text) {
        myInitText = text;
    }

    @Override
    protected MyTextFieldWithHistory createComponent() {
        MyTextFieldWithHistory component = new MyTextFieldWithHistory();
        component.setHistorySize(-1);
        component.setText(myInitText);

        Document document = component.getTextEditor().getDocument();

        addDocumentListenerForValidator(document);

        document.addDocumentListener(new DocumentAdapter() {
            @Override
            @RequiredUIAccess
            protected void textChanged(DocumentEvent e) {
                if (myFireListeners) {
                    valueChanged();
                }
            }
        });

        return component;
    }

    @SuppressWarnings("unchecked")
    @RequiredUIAccess
    private void valueChanged() {
        dataObject().getDispatcher(ValueComponentEvent.class)
            .onEvent(new ValueComponentEvent(this, getValue(), DesktopAWTInputDetails.currentEvent(toAWTComponent())));
    }

    @RequiredUIAccess
    private void replaceText(MyTextFieldWithHistory component, boolean fireListeners, Runnable replace) {
        String oldText = component.getText();

        boolean previous = myFireListeners;
        myFireListeners = false;
        try {
            replace.run();
        }
        finally {
            myFireListeners = previous;
        }

        if (fireListeners && !oldText.equals(component.getText())) {
            valueChanged();
        }
    }

    @RequiredUIAccess
    @Override
    public TextBoxWithHistory setHistory(List<String> history) {
        toAWTComponent().setHistoryKeepingText(history);
        return this;
    }

    @RequiredUIAccess
    @Override
    public void selectAll() {
        toAWTComponent().selectText();
    }

    @RequiredUIAccess
    @Override
    public void setEditable(boolean editable) {
        MyTextFieldWithHistory component = toAWTComponent();
        component.getTextEditor().setEditable(editable);

        if (!editable) {
            component.hidePopup();
        }
    }

    @Override
    public boolean isEditable() {
        return toAWTComponent().getTextEditor().isEditable();
    }

    @Override
    public String getValue() {
        return toAWTComponent().getText();
    }

    @RequiredUIAccess
    @Override
    public void setValue(String value, boolean fireListeners) {
        MyTextFieldWithHistory component = toAWTComponent();

        replaceText(component, fireListeners, () -> component.setText(StringUtil.notNullize(value)));
    }

    @RequiredUIAccess
    @Override
    public void setPlaceholder(LocalizeValue text) {
        toAWTComponent().putClientProperty("JTextField.placeholderText", text.getNullIfEmpty());
    }

    @RequiredUIAccess
    @Override
    public void setSuffixComponent(@Nullable Component suffixComponent) {
        AWTHasSuffixComponent.setSuffixComponent(toAWTComponent(), TargetAWT.to(suffixComponent));
    }

    @Override
    public @Nullable Component getSuffixComponent() {
        return TargetAWT.from(AWTHasSuffixComponent.getSuffixComponent(toAWTComponent()));
    }
}
