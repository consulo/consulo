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
package consulo.desktop.qt.editor.impl.internal;

import io.qt.core.QEvent;
import io.qt.core.QObject;
import io.qt.core.QRect;
import io.qt.gui.QColor;
import io.qt.gui.QPaintEvent;
import io.qt.gui.QPalette;
import io.qt.gui.QResizeEvent;
import io.qt.widgets.QApplication;
import io.qt.widgets.QComboBox;
import io.qt.widgets.QHBoxLayout;
import io.qt.widgets.QSizePolicy;
import io.qt.widgets.QStyle;
import io.qt.widgets.QStyleOptionComboBox;
import io.qt.widgets.QStylePainter;
import io.qt.widgets.QWidget;
import org.jspecify.annotations.Nullable;

final class DesktopQtEditorComboBoxFrame extends QComboBox implements DesktopQtEditorHost {
    private static final int MINIMUM_VISIBLE_CHARS = 8;

    private final QWidget myEditField;
    private final QHBoxLayout myLayout;
    private final QObject myFocusFilter;

    private @Nullable Runnable myPaletteListener;

    DesktopQtEditorComboBoxFrame(QWidget parent) {
        super(parent);
        setSizePolicy(QSizePolicy.Policy.Expanding, QSizePolicy.Policy.Fixed);
        setSizeAdjustPolicy(SizeAdjustPolicy.AdjustToMinimumContentsLengthWithIcon);
        setMinimumContentsLength(MINIMUM_VISIBLE_CHARS);

        myEditField = new QWidget(this);
        myLayout = new QHBoxLayout(myEditField);
        myLayout.setContentsMargins(0, 0, 0, 0);
        myLayout.setSpacing(0);

        myFocusFilter = new QObject(this) {
            @Override
            public boolean eventFilter(QObject watched, QEvent event) {
                QEvent.Type type = event.type();
                if (type == QEvent.Type.FocusIn || type == QEvent.Type.FocusOut) {
                    update();
                }
                return false;
            }
        };
    }

    @Override
    public QHBoxLayout getBoxLayout() {
        return myLayout;
    }

    @Override
    public void setOneLine(boolean oneLine) {
    }

    @Override
    public void watchFocus(QWidget widget) {
        widget.installEventFilter(myFocusFilter);
    }

    @Override
    public void setPaletteListener(@Nullable Runnable listener) {
        myPaletteListener = listener;
    }

    @Override
    public QColor getBaseColor() {
        return palette().color(QPalette.ColorRole.Base);
    }

    private void layoutEditField() {
        QStyleOptionComboBox option = new QStyleOptionComboBox();
        initStyleOption(option);
        QRect editField = style().subControlRect(QStyle.ComplexControl.CC_ComboBox, option, QStyle.SubControl.SC_ComboBoxEditField, this);
        myEditField.setGeometry(editField);
    }

    private boolean isEditorFocused() {
        QWidget focused = QApplication.focusWidget();
        return focused != null && (focused == myEditField || myEditField.isAncestorOf(focused));
    }

    @Override
    protected void initStyleOption(QStyleOptionComboBox option) {
        super.initStyleOption(option);

        option.setEditable(true);
        option.setCurrentText("");
        if (isEditorFocused()) {
            option.setState(option.state().combined(QStyle.StateFlag.State_HasFocus));
        }
    }

    @Override
    protected void resizeEvent(QResizeEvent event) {
        super.resizeEvent(event);
        layoutEditField();
    }

    @Override
    protected void changeEvent(QEvent event) {
        super.changeEvent(event);

        QEvent.Type type = event.type();
        if (type == QEvent.Type.StyleChange) {
            layoutEditField();
        }

        if (type == QEvent.Type.StyleChange || type == QEvent.Type.PaletteChange) {
            Runnable listener = myPaletteListener;
            if (listener != null) {
                listener.run();
            }
        }
    }

    @Override
    protected void paintEvent(QPaintEvent event) {
        QStylePainter painter = new QStylePainter(this);
        try {
            QStyleOptionComboBox option = new QStyleOptionComboBox();
            initStyleOption(option);
            painter.drawComplexControl(QStyle.ComplexControl.CC_ComboBox, option);
        }
        finally {
            painter.end();
        }
    }
}
