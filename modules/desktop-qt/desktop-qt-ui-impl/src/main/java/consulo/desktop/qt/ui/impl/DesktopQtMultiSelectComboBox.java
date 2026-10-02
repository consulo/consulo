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

import io.qt.core.QEvent;
import io.qt.core.QModelIndex;
import io.qt.core.QObject;
import io.qt.core.QSize;
import io.qt.core.Qt;
import io.qt.gui.QIcon;
import io.qt.gui.QKeyEvent;
import io.qt.gui.QMouseEvent;
import io.qt.gui.QPalette;
import io.qt.gui.QWheelEvent;
import io.qt.widgets.QAbstractItemView;
import io.qt.widgets.QStyle;
import io.qt.widgets.QStyleOptionComboBox;
import io.qt.widgets.QStyledItemDelegate;
import io.qt.widgets.QWidget;
import org.jspecify.annotations.Nullable;

import java.util.Set;
import java.util.function.IntConsumer;

/**
 * @author VISTALL
 * @since 2026-10-03
 */
public class DesktopQtMultiSelectComboBox extends DesktopQtComboBox {
    private static final Set<Integer> POPUP_KEYS = Set.of(
        Qt.Key.Key_Up.value(),
        Qt.Key.Key_Down.value(),
        Qt.Key.Key_PageUp.value(),
        Qt.Key.Key_PageDown.value(),
        Qt.Key.Key_Home.value(),
        Qt.Key.Key_End.value(),
        Qt.Key.Key_Space.value(),
        Qt.Key.Key_F4.value()
    );

    private static final Set<Integer> TOGGLE_KEYS = Set.of(
        Qt.Key.Key_Space.value(),
        Qt.Key.Key_Return.value(),
        Qt.Key.Key_Enter.value(),
        Qt.Key.Key_Select.value()
    );

    private final QObject myPopupFilter;
    private @Nullable IntConsumer myToggleHandler;
    private String mySelectionText = "";
    private String myPlaceholder = "";
    private int myPressedRow = -1;

    public DesktopQtMultiSelectComboBox(QWidget parent) {
        super(parent);

        setItemDelegate(new QStyledItemDelegate(this));

        myPopupFilter = new QObject(this) {
            @Override
            public boolean eventFilter(QObject watched, QEvent event) {
                return filterPopupEvent(watched, event);
            }
        };

        QAbstractItemView view = view();
        view.installEventFilter(myPopupFilter);
        view.viewport().installEventFilter(myPopupFilter);
    }

    public void setToggleHandler(@Nullable IntConsumer toggleHandler) {
        myToggleHandler = toggleHandler;
    }

    public void setItemChecked(int row, boolean checked) {
        Qt.CheckState state = checked ? Qt.CheckState.Checked : Qt.CheckState.Unchecked;
        setItemData(row, state.value(), Qt.ItemDataRole.CheckStateRole);
    }

    public void setSelectionText(String selectionText) {
        if (!mySelectionText.equals(selectionText)) {
            mySelectionText = selectionText;
            update();
        }
    }

    public void setPlaceholder(String placeholder) {
        if (!myPlaceholder.equals(placeholder)) {
            myPlaceholder = placeholder;
            updateGeometry();
            update();
        }
    }

    @Override
    public QSize sizeHint() {
        QSize hint = super.sizeHint();
        if (myPlaceholder.isEmpty()) {
            return hint;
        }

        QStyleOptionComboBox option = new QStyleOptionComboBox();
        initStyleOption(option);

        int textWidth = fontMetrics().horizontalAdvance(myPlaceholder);
        QSize needed = style().sizeFromContents(QStyle.ContentsType.CT_ComboBox, option, new QSize(textWidth, hint.height()), this);
        return new QSize(Math.max(hint.width(), needed.width()), hint.height());
    }

    @Override
    protected void decorateStyleOption(QStyleOptionComboBox option) {
        option.setCurrentIcon(new QIcon());

        if (!mySelectionText.isEmpty() || myPlaceholder.isEmpty()) {
            option.setCurrentText(mySelectionText);
            return;
        }

        QPalette palette = option.palette();
        palette.setBrush(QPalette.ColorRole.ButtonText, palette.placeholderText());
        option.setPalette(palette);
        option.setCurrentText(myPlaceholder);
    }

    @Override
    public void showPopup() {
        QAbstractItemView view = view();

        int width = view.sizeHintForColumn(0) + 2 * view.frameWidth();
        if (count() > maxVisibleItems()) {
            width += view.verticalScrollBar().sizeHint().width();
        }
        view.setMinimumWidth(width);

        super.showPopup();
    }

    @Override
    protected void keyPressEvent(QKeyEvent event) {
        if (POPUP_KEYS.contains(event.key())) {
            showPopup();
            event.accept();
            return;
        }

        event.ignore();
    }

    @Override
    protected void wheelEvent(QWheelEvent event) {
        event.ignore();
    }

    private boolean filterPopupEvent(QObject watched, QEvent event) {
        QAbstractItemView view = view();
        if (view == null || view.isDisposed()) {
            return false;
        }

        QEvent.Type type = event.type();
        if (watched == view.viewport() && event instanceof QMouseEvent mouseEvent) {
            if (type == QEvent.Type.MouseButtonPress) {
                myPressedRow = rowAt(view, mouseEvent);
                return false;
            }

            if (type == QEvent.Type.MouseButtonDblClick) {
                myPressedRow = rowAt(view, mouseEvent);
                return true;
            }

            if (type == QEvent.Type.MouseButtonRelease) {
                int row = rowAt(view, mouseEvent);
                int pressedRow = myPressedRow;
                myPressedRow = -1;

                if (mouseEvent.button() == Qt.MouseButton.LeftButton && row >= 0 && row == pressedRow) {
                    toggle(row);
                }
                return true;
            }
        }

        if (watched == view && type == QEvent.Type.KeyPress && event instanceof QKeyEvent keyEvent
            && TOGGLE_KEYS.contains(keyEvent.key())) {
            QModelIndex current = view.currentIndex();
            if (current != null && current.isValid()) {
                toggle(current.row());
            }
            return true;
        }

        return false;
    }

    private void toggle(int row) {
        IntConsumer toggleHandler = myToggleHandler;
        if (toggleHandler != null) {
            toggleHandler.accept(row);
        }
    }

    private static int rowAt(QAbstractItemView view, QMouseEvent event) {
        QModelIndex index = view.indexAt(event.position().toPoint());
        return index != null && index.isValid() ? index.row() : -1;
    }
}
