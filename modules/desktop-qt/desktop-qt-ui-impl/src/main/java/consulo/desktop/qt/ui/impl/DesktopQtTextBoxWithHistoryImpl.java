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
import consulo.ui.TextBoxWithHistory;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.event.ValueComponentEvent;
import consulo.ui.event.details.ProgrammaticInputDetails;
import consulo.util.lang.StringUtil;
import io.qt.core.QSize;
import io.qt.core.Qt;
import io.qt.gui.QKeyEvent;
import io.qt.gui.QWheelEvent;
import io.qt.widgets.QComboBox;
import io.qt.widgets.QLineEdit;
import io.qt.widgets.QSizePolicy;
import io.qt.widgets.QStyle;
import io.qt.widgets.QStyleOptionComboBox;
import io.qt.widgets.QWidget;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * @author VISTALL
 * @since 2026-08-16
 */
@SuppressWarnings({"unchecked", "rawtypes"})
public class DesktopQtTextBoxWithHistoryImpl extends QtComponentDelegate<QComboBox> implements TextBoxWithHistory {
    private static final int MINIMUM_VISIBLE_CHARS = 8;
    private static final int PREFERRED_VISIBLE_CHARS = 17;

    private static final String NO_AUTO_SELECT_PLACEHOLDER = " ";

    private String myText;

    private List<String> myHistory = List.of();

    private boolean myEditable = true;

    private boolean myFireListeners = true;

    private boolean myRebuilding;

    private LocalizeValue myPlaceholder = LocalizeValue.empty();

    private final DesktopQtLineEditSuffix mySuffix = new DesktopQtLineEditSuffix(this);

    private final List<Validator<String>> myValidators = new ArrayList<>();

    public DesktopQtTextBoxWithHistoryImpl(String text) {
        myText = StringUtil.notNullize(text);
    }

    @Override
    protected QComboBox createQt(QWidget parent) {
        QComboBox comboBox = new QComboBox(parent) {
            @Override
            public QSize sizeHint() {
                return withVisibleChars(PREFERRED_VISIBLE_CHARS, super.sizeHint());
            }

            @Override
            public QSize minimumSizeHint() {
                return withVisibleChars(MINIMUM_VISIBLE_CHARS, super.minimumSizeHint());
            }

            private QSize withVisibleChars(int visibleChars, QSize hint) {
                QStyleOptionComboBox option = new QStyleOptionComboBox();
                initStyleOption(option);

                int contentWidth = fontMetrics().horizontalAdvance("x".repeat(visibleChars));
                QSize size = style().sizeFromContents(QStyle.ContentsType.CT_ComboBox, option, new QSize(contentWidth, hint.height()), this);
                return new QSize(size.width(), hint.height());
            }

            @Override
            public void showPopup() {
                if (myEditable && count() > 0) {
                    super.showPopup();
                }
            }

            @Override
            protected void keyPressEvent(QKeyEvent event) {
                int key = event.key();
                if (key == Qt.Key.Key_Down.value()) {
                    if (myEditable && count() > 0) {
                        showPopup();
                        event.accept();
                    }
                    else {
                        event.ignore();
                    }
                    return;
                }

                if (key == Qt.Key.Key_Up.value() || key == Qt.Key.Key_PageUp.value() || key == Qt.Key.Key_PageDown.value()) {
                    event.ignore();
                    return;
                }

                super.keyPressEvent(event);
            }

            @Override
            protected void wheelEvent(QWheelEvent event) {
                event.ignore();
            }
        };

        comboBox.setLineEdit(mySuffix.createLineEdit(comboBox));
        comboBox.setCompleter(null);
        comboBox.setInsertPolicy(QComboBox.InsertPolicy.NoInsert);
        comboBox.setPlaceholderText(NO_AUTO_SELECT_PLACEHOLDER);
        comboBox.setSizePolicy(QSizePolicy.Policy.Expanding, QSizePolicy.Policy.Fixed);
        return comboBox;
    }

    @Override
    protected void initialize(QComboBox component) {
        super.initialize(component);

        rebuild(component, false);

        QLineEdit lineEdit = component.lineEdit();
        if (lineEdit != null) {
            lineEdit.setReadOnly(!myEditable);
            lineEdit.setPlaceholderText(myPlaceholder.get());

            mySuffix.attach(lineEdit);
        }

        component.editTextChanged.connect(text -> {
            if (myRebuilding) {
                return;
            }

            myText = StringUtil.notNullize(text);

            if (myFireListeners) {
                getListenerDispatcher(ValueComponentEvent.class)
                    .onEvent(new ValueComponentEvent(this, myText, DesktopQtCurrentInput.current(component)));
            }
        });
    }

    private void rebuild(QComboBox component, boolean keepCaret) {
        QLineEdit lineEdit = component.lineEdit();
        int cursorPosition = lineEdit == null ? 0 : lineEdit.cursorPosition();
        int selectionStart = lineEdit == null ? -1 : lineEdit.selectionStart();
        int selectionLength = lineEdit == null ? 0 : lineEdit.selectionLength();

        myRebuilding = true;
        try {
            int oldCount = component.count();
            component.insertItems(oldCount, myHistory);
            if (oldCount > 0) {
                component.model().removeRows(0, oldCount, component.rootModelIndex());
            }

            if (component.currentIndex() != -1) {
                component.setCurrentIndex(-1);
            }

            if (lineEdit != null && !myText.equals(lineEdit.text())) {
                lineEdit.setText(myText);

                if (keepCaret) {
                    restoreCaret(lineEdit, cursorPosition, selectionStart, selectionLength);
                }
            }
        }
        finally {
            myRebuilding = false;
        }
    }

    private static void restoreCaret(QLineEdit lineEdit, int cursorPosition, int selectionStart, int selectionLength) {
        int length = lineEdit.text().length();
        if (selectionStart >= 0 && selectionLength > 0 && selectionStart + selectionLength <= length) {
            if (cursorPosition == selectionStart) {
                lineEdit.setSelection(selectionStart + selectionLength, -selectionLength);
            }
            else {
                lineEdit.setSelection(selectionStart, selectionLength);
            }
        }
        else {
            lineEdit.setCursorPosition(Math.min(cursorPosition, length));
        }
    }

    private @Nullable QLineEdit lineEdit() {
        QComboBox component = myComponent;
        if (component == null || component.isDisposed()) {
            return null;
        }

        QLineEdit lineEdit = component.lineEdit();
        return lineEdit == null || lineEdit.isDisposed() ? null : lineEdit;
    }

    @RequiredUIAccess
    @Override
    public TextBoxWithHistory setHistory(List<String> history) {
        List<String> items = new ArrayList<>(history.size());
        for (String item : history) {
            items.add(StringUtil.notNullize(item));
        }
        myHistory = items;

        QComboBox component = myComponent;
        if (component != null && !component.isDisposed()) {
            rebuild(component, true);
        }
        return this;
    }

    @RequiredUIAccess
    @Override
    public void setPlaceholder(LocalizeValue text) {
        myPlaceholder = text == null ? LocalizeValue.empty() : text;

        QLineEdit lineEdit = lineEdit();
        if (lineEdit != null) {
            lineEdit.setPlaceholderText(myPlaceholder.get());
        }
    }

    @RequiredUIAccess
    @Override
    public void selectAll() {
        QLineEdit lineEdit = lineEdit();
        if (lineEdit != null) {
            lineEdit.selectAll();
        }
    }

    @RequiredUIAccess
    @Override
    public void setEditable(boolean editable) {
        myEditable = editable;

        QLineEdit lineEdit = lineEdit();
        if (lineEdit != null) {
            lineEdit.setReadOnly(!editable);
        }

        QComboBox component = myComponent;
        if (!editable && component != null && !component.isDisposed()) {
            component.hidePopup();
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
        String newText = StringUtil.notNullize(value);
        boolean changed = !newText.equals(myText);
        myText = newText;

        QComboBox component = myComponent;
        if (component == null || component.isDisposed()) {
            if (fireListeners && changed) {
                getListenerDispatcher(ValueComponentEvent.class)
                    .onEvent(new ValueComponentEvent(this, myText, ProgrammaticInputDetails.INSTANCE));
            }
            return;
        }

        myFireListeners = fireListeners;
        try {
            component.setEditText(myText);
        }
        finally {
            myFireListeners = true;
        }
    }

    @RequiredUIAccess
    @Override
    public void setSuffixComponent(@Nullable Component suffixComponent) {
        mySuffix.set(suffixComponent, lineEdit());
    }

    @Override
    public @Nullable Component getSuffixComponent() {
        return mySuffix.get();
    }
}
