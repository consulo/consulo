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
import io.qt.core.QPoint;
import io.qt.core.QRect;
import io.qt.core.QSize;
import io.qt.core.Qt;
import io.qt.gui.QIcon;
import io.qt.gui.QKeyEvent;
import io.qt.gui.QMouseEvent;
import io.qt.gui.QResizeEvent;
import io.qt.gui.QScreen;
import io.qt.gui.QWheelEvent;
import io.qt.widgets.QAbstractItemView;
import io.qt.widgets.QStyleOptionComboBox;
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

    private static final String ELLIPSIS = "\u2026";

    private final QAbstractItemView myView;
    private final QWidget myViewport;
    private final QObject myPopupFilter;
    private @Nullable IntConsumer myToggleHandler;
    private String mySelectionText = "";
    private String myPlaceholder = "";
    private int myPressedRow = -1;

    public DesktopQtMultiSelectComboBox(QWidget parent) {
        super(parent);

        myView = view();
        myViewport = myView.viewport();

        myPopupFilter = new QObject(this) {
            @Override
            public boolean eventFilter(QObject watched, QEvent event) {
                return filterPopupEvent(watched, event);
            }
        };

        myView.installEventFilter(myPopupFilter);
        myViewport.installEventFilter(myPopupFilter);
    }

    public void uninstallPopupFilter() {
        myToggleHandler = null;

        if (!myViewport.isDisposed()) {
            myViewport.removeEventFilter(myPopupFilter);
        }

        if (!myView.isDisposed()) {
            myView.removeEventFilter(myPopupFilter);
        }
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
            updateGeometry();
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

        String text = mySelectionText.isEmpty() ? myPlaceholder : mySelectionText;
        if (text.isEmpty()) {
            return hint;
        }

        return new QSize(adaptiveLabelWidth(text), hint.height());
    }

    @Override
    public QSize minimumSizeHint() {
        QSize minimum = super.minimumSizeHint();
        return new QSize(Math.min(minimum.width(), sizeHint().width()), minimum.height());
    }

    @Override
    protected int minimumContentWidth() {
        return fontMetrics().horizontalAdvance(ELLIPSIS);
    }

    @Override
    protected void decorateStyleOption(QStyleOptionComboBox option) {
        option.setCurrentIcon(new QIcon());
        option.setCurrentText(mySelectionText);
    }

    @Override
    protected String labelPlaceholder() {
        return mySelectionText.isEmpty() ? myPlaceholder : "";
    }

    @Override
    public void showPopup() {
        QAbstractItemView view = myView;
        if (view.isDisposed()) {
            return;
        }

        view.setMinimumWidth(popupContentWidth(view));

        super.showPopup();
    }

    @Override
    protected void resizeEvent(QResizeEvent event) {
        super.resizeEvent(event);

        fitVisiblePopupWidth();
    }

    private int popupContentWidth(QAbstractItemView view) {
        int width = view.sizeHintForColumn(0) + 2 * view.frameWidth();
        if (count() > maxVisibleItems()) {
            width += view.verticalScrollBar().sizeHint().width();
        }
        return width;
    }

    private void fitVisiblePopupWidth() {
        QAbstractItemView view = myView;
        if (view.isDisposed() || !view.isVisible()) {
            return;
        }

        QWidget container = view.window();
        if (container == null || container == window() || !container.isVisible()) {
            return;
        }

        int popupWidth = Math.max(width(), Math.max(view.minimumWidth(), container.minimumWidth()));
        if (popupWidth == container.width()) {
            return;
        }

        QPoint origin = mapToGlobal(new QPoint(0, 0));
        int x = layoutDirection() == Qt.LayoutDirection.RightToLeft ? origin.x() + width() - popupWidth : origin.x();
        QRect geometry = new QRect(x, container.y(), popupWidth, container.height());

        QScreen screen = container.screen();
        if (screen != null) {
            QRect available = screen.availableGeometry();
            if (geometry.right() > available.right()) {
                geometry.moveRight(available.right());
            }
            if (geometry.left() < available.left()) {
                geometry.moveLeft(available.left());
            }
        }

        container.setGeometry(geometry);
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
        QEvent.Type type = event.type();
        if (!isPopupInput(type)) {
            return false;
        }

        QAbstractItemView view = myView;
        if (view.isDisposed() || myViewport.isDisposed()) {
            return false;
        }

        if (watched == myViewport && event instanceof QMouseEvent mouseEvent) {
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

    private static boolean isPopupInput(QEvent.Type type) {
        return type == QEvent.Type.MouseButtonPress
            || type == QEvent.Type.MouseButtonRelease
            || type == QEvent.Type.MouseButtonDblClick
            || type == QEvent.Type.KeyPress;
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
