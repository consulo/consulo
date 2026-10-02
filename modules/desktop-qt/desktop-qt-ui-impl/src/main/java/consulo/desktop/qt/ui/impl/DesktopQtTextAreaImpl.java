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
import io.qt.gui.QPalette;
import io.qt.gui.QTextOption;
import io.qt.widgets.QPlainTextEdit;
import io.qt.widgets.QWidget;
import consulo.ui.Component;
import io.qt.core.QEvent;
import io.qt.core.Qt;
import io.qt.widgets.QStyle;
import org.jspecify.annotations.Nullable;

/**
 * @author VISTALL
 * @since 2026-09-30
 */
public class DesktopQtTextAreaImpl extends QtComponentDelegate<QPlainTextEdit> implements TextArea {
    private static final class SidePlainTextEdit extends QPlainTextEdit implements DesktopQtSideMarginsHost {
        private final @Nullable DesktopQtTextAreaImpl myOwner;

        private SidePlainTextEdit(QWidget parent, DesktopQtTextAreaImpl owner) {
            super(parent);
            myOwner = owner;

            setAutoFillBackground(true);
            setBackgroundRole(QPalette.ColorRole.Base);
            viewport().setAutoFillBackground(false);
        }

        @Override
        public void setSideMargins(int left, int right) {
            setViewportMargins(left, 0, right, 0);
        }

        @Override
        public boolean event(QEvent event) {
            boolean result = super.event(event);

            DesktopQtTextAreaImpl owner = myOwner;
            if (owner != null && DesktopQtSideComponents.isLayoutEvent(event)) {
                owner.mySides.layoutSides(this);
                owner.applyRows();
            }
            return result;
        }
    }

    private final DesktopQtSideComponents mySides = new DesktopQtSideComponents(this);

    private int myNativeFrameWidth;
    private int myVisibleLength;
    private int myMinRows;
    private int myMaxRows;

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
        SidePlainTextEdit edit = new SidePlainTextEdit(parent, this);
        myNativeFrameWidth = edit.style().pixelMetric(QStyle.PixelMetric.PM_DefaultFrameWidth, null, edit);
        return edit;
    }

    @Override
    protected int getNativeFrameWidth() {
        return myNativeFrameWidth;
    }

    @Override
    @SuppressWarnings("unchecked")
    protected void initialize(QPlainTextEdit component) {
        super.initialize(component);

        component.setLineWrapMode(QPlainTextEdit.LineWrapMode.WidgetWidth);
        component.setWordWrapMode(QTextOption.WrapMode.WrapAtWordBoundaryOrAnywhere);
        component.setTabChangesFocus(true);
        component.setPlainText(myText);
        component.setReadOnly(!myEditable);
        component.setPlaceholderText(myPlaceholder.get());
        applyFont(component);

        mySides.attach(component);
        applyVisibleLength();
        applyRows();

        component.document().blockCountChanged.connect(count -> applyRows());

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
    public void replaceSelection(String text) {
        if (isAlive()) {
            myComponent.insertPlainText(text);
        }
        else {
            setValue(myText + text);
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

    @Override
    @RequiredUIAccess
    public void setVisibleLength(int columns) {
        myVisibleLength = columns;

        applyVisibleLength();
    }

    @Override
    @RequiredUIAccess
    public void setMinRows(int rows) {
        myMinRows = rows;

        applyRows();
    }

    @Override
    @RequiredUIAccess
    public void setMaxRows(int rows) {
        myMaxRows = rows;

        applyRows();
    }

    @Override
    public void setPrefixComponent(@Nullable Component prefixComponent) {
        mySides.setPrefix(prefixComponent, myComponent);
    }

    @Override
    public @Nullable Component getPrefixComponent() {
        return mySides.getPrefix();
    }

    @Override
    public void setSuffixComponent(@Nullable Component suffixComponent) {
        mySides.setSuffix(suffixComponent, myComponent);
    }

    @Override
    public @Nullable Component getSuffixComponent() {
        return mySides.getSuffix();
    }

    private void applyVisibleLength() {
        if (!isAlive() || myVisibleLength <= 0) {
            return;
        }

        QPlainTextEdit edit = myComponent;
        int textWidth = edit.fontMetrics().horizontalAdvance("m") * myVisibleLength;
        int documentMargin = (int) Math.ceil(edit.document().documentMargin() * 2);
        edit.setFixedWidth(textWidth + documentMargin + edit.frameWidth() * 2);
    }

    private void applyRows() {
        if (!isAlive() || myMinRows <= 0 && myMaxRows <= 0) {
            return;
        }

        QPlainTextEdit edit = myComponent;

        int lines = Math.max(1, edit.document().blockCount());
        int rows = lines;
        if (myMinRows > 0) {
            rows = Math.max(rows, myMinRows);
        }
        if (myMaxRows > 0) {
            rows = Math.min(rows, myMaxRows);
        }

        int chrome = edit.height() > 0 && edit.viewport().height() > 0
            ? edit.height() - edit.viewport().height()
            : edit.frameWidth() * 2;
        int documentMargin = (int) Math.ceil(edit.document().documentMargin() * 2);

        int height = rows * edit.fontMetrics().lineSpacing() + documentMargin + chrome;
        if (edit.minimumHeight() != height || edit.maximumHeight() != height) {
            edit.setFixedHeight(height);
        }

        edit.setVerticalScrollBarPolicy(lines > rows ? Qt.ScrollBarPolicy.ScrollBarAsNeeded : Qt.ScrollBarPolicy.ScrollBarAlwaysOff);
        edit.setHorizontalScrollBarPolicy(Qt.ScrollBarPolicy.ScrollBarAlwaysOff);
    }
}
