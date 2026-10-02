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
package consulo.desktop.awt.ui.impl.textBox;

import consulo.desktop.awt.ui.impl.DesktopFontImpl;
import consulo.desktop.awt.ui.impl.base.SwingComponentDelegate;
import consulo.desktop.awt.ui.impl.event.DesktopAWTInputDetails;
import consulo.desktop.awt.ui.impl.facade.FromSwingComponentWrapper;
import consulo.desktop.awt.ui.impl.util.AWTFocusAdapterAsBlurListener;
import consulo.desktop.awt.ui.impl.util.AWTFocusAdapterAsFocusListener;
import consulo.localize.LocalizeValue;
import consulo.ui.Component;
import consulo.ui.TextArea;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.event.BlurEvent;
import consulo.ui.event.FocusEvent;
import consulo.ui.event.ValueComponentEvent;
import consulo.ui.ex.awt.JBScrollPane;
import consulo.ui.ex.awt.JBTextArea;
import consulo.ui.ex.awt.event.DocumentAdapter;
import consulo.ui.font.Font;
import consulo.util.lang.StringUtil;
import org.jspecify.annotations.Nullable;

import javax.swing.UIManager;
import javax.swing.event.DocumentEvent;

/**
 * @author VISTALL
 * @since 2026-09-30
 */
public class DesktopTextAreaImpl extends SwingComponentDelegate<DesktopTextAreaImpl.MyScrollPane> implements TextArea {
    class MyScrollPane extends JBScrollPane implements FromSwingComponentWrapper {
        private final JBTextArea myTextArea;

        MyScrollPane(JBTextArea textArea) {
            super(textArea);
            myTextArea = textArea;
        }

        @Override
        public Component toUIComponent() {
            return DesktopTextAreaImpl.this;
        }
    }

    private final String myText;

    private boolean myFireListeners = true;

    @RequiredUIAccess
    public DesktopTextAreaImpl(String text) {
        myText = text;
    }

    @Override
    protected MyScrollPane createComponent() {
        JBTextArea textArea = new JBTextArea(myText);
        textArea.setLineWrap(true);
        textArea.setWrapStyleWord(true);
        textArea.getDocument().addDocumentListener(new DocumentAdapter() {
            @Override
            @RequiredUIAccess
            protected void textChanged(DocumentEvent e) {
                valueChanged();
            }
        });
        return new MyScrollPane(textArea);
    }

    @Override
    protected void init(MyScrollPane component) {
        super.init(component);

        component.myTextArea.addFocusListener(new AWTFocusAdapterAsFocusListener(this, getListenerDispatcher(FocusEvent.class)));
        component.myTextArea.addFocusListener(new AWTFocusAdapterAsBlurListener(this, getListenerDispatcher(BlurEvent.class)));
    }

    private JBTextArea textArea() {
        return toAWTComponent().myTextArea;
    }

    @SuppressWarnings("unchecked")
    @RequiredUIAccess
    private void valueChanged() {
        if (!myFireListeners) {
            return;
        }

        getListenerDispatcher(ValueComponentEvent.class)
            .onEvent(new ValueComponentEvent(this, getValue(), DesktopAWTInputDetails.currentEvent(textArea())));
    }

    @Override
    public String getValue() {
        return StringUtil.notNullize(textArea().getText());
    }

    @Override
    @RequiredUIAccess
    public void setValue(@Nullable String value, boolean fireListeners) {
        myFireListeners = fireListeners;
        try {
            textArea().setText(StringUtil.notNullize(value));
        }
        finally {
            myFireListeners = true;
        }
    }

    @Override
    public void setPlaceholder(LocalizeValue text) {
        textArea().getEmptyText().setText(text.get());
    }

    @Override
    @RequiredUIAccess
    public void setEditable(boolean editable) {
        textArea().setEditable(editable);
    }

    @Override
    public boolean isEditable() {
        return textArea().isEditable();
    }

    @Override
    @RequiredUIAccess
    public void selectAll() {
        textArea().selectAll();
    }

    @Override
    @RequiredUIAccess
    public void replaceSelection(String text) {
        textArea().replaceSelection(text);
    }

    @Override
    @RequiredUIAccess
    public void setFont(@Nullable Font font) {
        textArea().setFont(font instanceof DesktopFontImpl desktopFont ? desktopFont.getFont() : UIManager.getFont("TextArea.font"));
    }

    @Override
    public boolean hasFocus() {
        return textArea().hasFocus();
    }

    @Override
    public boolean isFocusable() {
        return textArea().isFocusable();
    }

    @Override
    public void setFocusable(boolean focusable) {
        textArea().setFocusable(focusable);
    }

    @Override
    public void focus() {
        textArea().requestFocus();
    }
}
