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
package consulo.desktop.qt.ui.impl.clipboard;

import consulo.platform.Platform;
import consulo.ui.Component;
import consulo.ui.DragAndDropTransferHandler;
import consulo.ui.TransferHandler;
import consulo.ui.clipboard.DataTransfer;
import io.qt.core.QItemSelectionModel;
import io.qt.core.QModelIndex;
import io.qt.core.QPoint;
import io.qt.core.QRect;
import io.qt.core.Qt;
import io.qt.gui.QCursor;
import io.qt.gui.QDrag;
import io.qt.gui.QDragEnterEvent;
import io.qt.gui.QDragMoveEvent;
import io.qt.gui.QDropEvent;
import io.qt.gui.QGuiApplication;
import io.qt.gui.QPainter;
import io.qt.gui.QPixmap;
import io.qt.widgets.QAbstractItemView;
import io.qt.widgets.QStyle;
import io.qt.widgets.QStyleOption;
import io.qt.widgets.QWidget;
import org.jspecify.annotations.Nullable;

import java.util.List;

/**
 * @author VISTALL
 * @since 2026-10-10
 */
public final class DesktopQtDragAndDropSupport<Item> {
    private static final double ourGhostOpacity = 0.6;

    private final Component myComponent;
    private final DesktopQtTransferTarget<Item> myTarget;

    private @Nullable DragAndDropTransferHandler<Item> myHandler;

    private List<Item> myDraggedItems = List.of();
    private DataTransfer myHoverTransfer = DataTransfer.EMPTY;
    private @Nullable QRect myIndicator;

    public DesktopQtDragAndDropSupport(Component component, DesktopQtTransferTarget<Item> target) {
        myComponent = component;
        myTarget = target;
    }

    public static DragAndDropTransferHandler.DropPosition positionOf(QRect rect, QPoint position) {
        int boundary = rect.height() >= 30 ? 10 : rect.height() / 3;
        if (position.y() < rect.top() + boundary) {
            return DragAndDropTransferHandler.DropPosition.ABOVE;
        }
        if (position.y() >= rect.top() + rect.height() - boundary) {
            return DragAndDropTransferHandler.DropPosition.BELOW;
        }
        return DragAndDropTransferHandler.DropPosition.INTO;
    }

    public static QRect lineAbove(QRect rect) {
        return new QRect(rect.left(), rect.top(), rect.width(), 0);
    }

    public static QRect lineBelow(QRect rect) {
        return new QRect(rect.left(), rect.bottom(), rect.width(), 0);
    }

    public void setHandler(@Nullable TransferHandler<Item> handler, @Nullable QAbstractItemView view) {
        myHandler = handler instanceof DragAndDropTransferHandler<Item> dragAndDrop ? dragAndDrop : null;

        if (view != null && !view.isDisposed()) {
            install(view);
        }
    }

    public void install(QAbstractItemView view) {
        boolean enabled = myHandler != null;
        view.setDragDropMode(enabled ? QAbstractItemView.DragDropMode.DragDrop : QAbstractItemView.DragDropMode.NoDragDrop);
        view.setDropIndicatorShown(false);
        view.viewport().setAcceptDrops(enabled);

        if (!enabled) {
            myHoverTransfer = DataTransfer.EMPTY;
            showIndicator(view, null);
        }
    }

    public void startDrag(QAbstractItemView view) {
        DragAndDropTransferHandler<Item> handler = myHandler;
        if (handler == null) {
            return;
        }

        List<Item> items = myTarget.getTransferItems();
        if (items.isEmpty()) {
            return;
        }

        boolean move = !isCopyGesture(QGuiApplication.keyboardModifiers());
        DataTransfer transfer = handler.createDragTransfer(myComponent, items, move);
        if (transfer == null || transfer.isEmpty()) {
            return;
        }

        QDrag drag = new QDrag(view);
        drag.setMimeData(DesktopQtDataTransfers.toMimeData(transfer));
        applyPixmap(view, drag);

        myDraggedItems = items;
        try {
            drag.exec(new Qt.DropActions(Qt.DropAction.CopyAction, Qt.DropAction.MoveAction),
                move ? Qt.DropAction.MoveAction : Qt.DropAction.CopyAction);
        }
        finally {
            myDraggedItems = List.of();
        }
    }

    public void dragEnter(QAbstractItemView view, QDragEnterEvent event) {
        if (myHandler == null) {
            event.ignore();
            return;
        }

        myHoverTransfer = DesktopQtDataTransfers.fromDropEvent(event);
        event.acceptProposedAction();
    }

    public void dragMove(QAbstractItemView view, QDragMoveEvent event) {
        QPoint position = event.position().toPoint();
        myTarget.dragHovered(position);

        DesktopQtDrop<Item> drop = myHandler == null ? null : myTarget.resolveDrop(position);
        if (drop == null || !accepts(drop, myHoverTransfer)) {
            event.ignore();
            showIndicator(view, null);
            return;
        }

        event.acceptProposedAction();
        showIndicator(view, drop.indicator());
    }

    public void dragLeave(QAbstractItemView view) {
        finishHover(view);
    }

    public void drop(QAbstractItemView view, QDropEvent event) {
        finishHover(view);

        DragAndDropTransferHandler<Item> handler = myHandler;
        DesktopQtDrop<Item> drop = handler == null ? null : myTarget.resolveDrop(event.position().toPoint());
        if (handler == null || drop == null) {
            event.ignore();
            return;
        }

        DesktopQtDropContext<Item> context = new DesktopQtDropContext<>(drop, DesktopQtDataTransfers.fromDropEvent(event), myDraggedItems, true);
        if (!handler.drop(myComponent, context) || !handler.drop(myComponent, context.toPerforming())) {
            event.ignore();
            return;
        }

        event.acceptProposedAction();
    }

    public void contentsScrolled(QAbstractItemView view) {
        showIndicator(view, null);
    }

    public boolean shouldAutoScroll(QAbstractItemView view, QPoint position) {
        if (myHandler == null || !view.hasAutoScroll()) {
            return false;
        }

        QRect area = view.viewport().rect();
        int margin = view.autoScrollMargin();
        return position.y() - area.top() < margin
            || area.bottom() - position.y() < margin
            || position.x() - area.left() < margin
            || area.right() - position.x() < margin;
    }

    public void paintIndicator(QAbstractItemView view) {
        QRect indicator = myIndicator;
        if (indicator == null) {
            return;
        }

        QPainter painter = new QPainter(view.viewport());
        try {
            QStyleOption option = new QStyleOption();
            option.initFrom(view);
            option.setRect(indicator);
            view.style().drawPrimitive(QStyle.PrimitiveElement.PE_IndicatorItemViewItemDrop, option, painter, view);
        }
        finally {
            painter.end();
        }
    }

    private boolean accepts(DesktopQtDrop<Item> drop, DataTransfer transfer) {
        DragAndDropTransferHandler<Item> handler = myHandler;
        return handler != null && handler.drop(myComponent, new DesktopQtDropContext<>(drop, transfer, myDraggedItems, true));
    }

    private void finishHover(QAbstractItemView view) {
        myHoverTransfer = DataTransfer.EMPTY;
        myTarget.dragHovered(null);
        showIndicator(view, null);
    }

    private void showIndicator(QAbstractItemView view, @Nullable QRect indicator) {
        QRect current = myIndicator;
        if (current == null ? indicator == null : current.equals(indicator)) {
            return;
        }

        myIndicator = indicator;
        if (!view.isDisposed()) {
            view.viewport().update();
        }
    }

    public static boolean isCopyGesture(Qt.KeyboardModifiers modifiers) {
        Qt.KeyboardModifier copy = Platform.current().os().isMac() ? Qt.KeyboardModifier.AltModifier : Qt.KeyboardModifier.ControlModifier;
        return modifiers.testFlag(copy);
    }

    private static void applyPixmap(QAbstractItemView view, QDrag drag) {
        QItemSelectionModel selectionModel = view.selectionModel();
        if (selectionModel == null) {
            return;
        }

        QRect bounds = null;
        for (QModelIndex index : selectionModel.selectedIndexes()) {
            QRect rect = view.visualRect(index);
            bounds = bounds == null ? rect : bounds.united(rect);
        }

        QWidget viewport = view.viewport();
        QRect visible = bounds == null ? null : bounds.intersected(viewport.rect());
        if (visible == null || visible.isEmpty()) {
            return;
        }

        QPixmap grabbed = viewport.grab(visible);
        QPixmap ghost = new QPixmap(grabbed.size());
        ghost.setDevicePixelRatio(grabbed.devicePixelRatio());
        ghost.fill(Qt.GlobalColor.transparent);

        QPainter painter = new QPainter(ghost);
        try {
            painter.setOpacity(ourGhostOpacity);
            painter.drawPixmap(0, 0, grabbed);
        }
        finally {
            painter.end();
        }

        drag.setPixmap(ghost);

        QPoint cursor = viewport.mapFromGlobal(QCursor.pos());
        drag.setHotSpot(new QPoint(cursor.x() - visible.x(), cursor.y() - visible.y()));
    }
}
