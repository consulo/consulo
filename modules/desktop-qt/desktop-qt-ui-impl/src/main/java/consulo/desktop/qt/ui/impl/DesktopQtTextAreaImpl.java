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

import consulo.desktop.qt.ui.impl.font.DesktopQtFontImpl;
import consulo.localize.LocalizeValue;
import consulo.ui.TextArea;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.event.ValueComponentEvent;
import consulo.ui.event.details.ProgrammaticInputDetails;
import consulo.ui.font.Font;
import consulo.util.lang.StringUtil;
import io.qt.gui.QFont;
import io.qt.gui.QTextOption;
import io.qt.widgets.QPlainTextEdit;
import io.qt.widgets.QWidget;
import org.jspecify.annotations.Nullable;

/**
 * @author VISTALL
 * @since 2026-09-30
 */
public class DesktopQtTextAreaImpl extends QtComponentDelegate<QPlainTextEdit> implements TextArea {
    private String myText;
    private boolean myEditable = true;
    private boolean myFireListeners = true;
    private LocalizeValue myPlaceholder = LocalizeValue.empty();
    private @Nullable Font myFont;

    public DesktopQtTextAreaImpl(String text) {
        myText = StringUtil.notNullize(text);
    }

    @Override
    protected QPlainTextEdit createQt(QWidget parent) {
        return new QPlainTextEdit(parent);
    }

    @Override
    @SuppressWarnings("unchecked")
    protected void initialize(QPlainTextEdit component) {
        super.initialize(component);

        component.setLineWrapMode(QPlainTextEdit.LineWrapMode.WidgetWidth);
        component.setWordWrapMode(QTextOption.WrapMode.WrapAtWordBoundaryOrAnywhere);
        component.setPlainText(myText);
        component.setReadOnly(!myEditable);
        component.setPlaceholderText(myPlaceholder.get());
        applyFont(component);

        component.textChanged.connect(() -> {
            myText = component.toPlainText();
            if (myFireListeners) {
                getListenerDispatcher(ValueComponentEvent.class)
                    .onEvent(new ValueComponentEvent(this, myText, DesktopQtCurrentInput.current(component)));
            }
        });
    }

    private boolean isAlive() {
        return myComponent != null && !myComponent.isDisposed();
    }

    private void applyFont(QPlainTextEdit component) {
        component.setFont(myFont instanceof DesktopQtFontImpl qtFont ? qtFont.toQFont() : new QFont());
    }

    @Override
    public String getValue() {
        return myText;
    }

    @Override
    @RequiredUIAccess
    public void setValue(@Nullable String value, boolean fireListeners) {
        String newText = StringUtil.notNullize(value);
        boolean changed = !newText.equals(myText);
        myText = newText;

        if (!isAlive()) {
            if (fireListeners && changed) {
                getListenerDispatcher(ValueComponentEvent.class)
                    .onEvent(new ValueComponentEvent(this, myText, ProgrammaticInputDetails.INSTANCE));
            }
            return;
        }

        myFireListeners = fireListeners;
        try {
            myComponent.setPlainText(myText);
        }
        finally {
            myFireListeners = true;
        }
    }

    @Override
    public void setPlaceholder(LocalizeValue text) {
        myPlaceholder = text;
        if (isAlive()) {
            myComponent.setPlaceholderText(text.get());
        }
    }

    @Override
    @RequiredUIAccess
    public void setEditable(boolean editable) {
        myEditable = editable;
        if (isAlive()) {
            myComponent.setReadOnly(!editable);
        }
    }

    @Override
    public boolean isEditable() {
        return myEditable;
    }

    @Override
    @RequiredUIAccess
    public void selectAll() {
        if (isAlive()) {
            myComponent.selectAll();
        }
    }

    @Override
    @RequiredUIAccess
    public void setFont(@Nullable Font font) {
        myFont = font;
        if (isAlive()) {
            applyFont(myComponent);
        }
    }
}
