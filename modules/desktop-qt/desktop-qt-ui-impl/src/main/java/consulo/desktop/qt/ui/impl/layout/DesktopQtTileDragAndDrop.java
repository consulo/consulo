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
package consulo.desktop.qt.ui.impl.layout;

import consulo.desktop.qt.ui.impl.QtComponentDelegate;
import consulo.desktop.qt.ui.impl.clipboard.DesktopQtDataTransfers;
import consulo.desktop.qt.ui.impl.clipboard.DesktopQtDragAndDropSupport;
import consulo.desktop.qt.ui.impl.clipboard.DesktopQtDragSession;
import consulo.desktop.qt.ui.impl.clipboard.DesktopQtDrop;
import consulo.desktop.qt.ui.impl.clipboard.DesktopQtDropContext;
import consulo.ui.Component;
import consulo.ui.DragAndDropTransferHandler;
import consulo.ui.clipboard.DataTransfer;
import io.qt.core.QEvent;
import io.qt.core.QObject;
import io.qt.core.QPoint;
import io.qt.core.QRect;
import io.qt.core.Qt;
import io.qt.gui.QDrag;
import io.qt.gui.QDragMoveEvent;
import io.qt.gui.QDropEvent;
import io.qt.gui.QGuiApplication;
import io.qt.gui.QMouseEvent;
import io.qt.gui.QPalette;
import io.qt.widgets.QApplication;
import io.qt.widgets.QWidget;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * Drag and drop of the children of a {@link DesktopQtTileLayoutImpl}.
 */
final class DesktopQtTileDragAndDrop {
    private static final int INDICATOR_WIDTH = 2;

    private final DesktopQtTileLayoutImpl myLayout;

    private @Nullable QPoint myPressPosition;
    private @Nullable Component myPressChild;

    private List<Component> myDraggedChildren = List.of();
    private DataTransfer myHoverTransfer = DataTransfer.EMPTY;

    private @Nullable QWidget myIndicator;

    DesktopQtTileDragAndDrop(DesktopQtTileLayoutImpl layout) {
        myLayout = layout;
    }

    void install(QWidget container) {
        container.setAcceptDrops(true);
        container.installEventFilter(new QObject(container) {
            @Override
            public boolean eventFilter(QObject watched, QEvent event) {
                return filter(container, event);
            }
        });
    }

    private boolean filter(QWidget container, QEvent event) {
        DragAndDropTransferHandler<Component> handler = myLayout.getDragAndDropHandler();
        if (handler == null) {
            return false;
        }

        switch (event.type()) {
            case MouseButtonPress -> {
                if (event instanceof QMouseEvent mouseEvent && mouseEvent.button() == Qt.MouseButton.LeftButton) {
                    myPressPosition = mouseEvent.position().toPoint();
                    myPressChild = childAt(myPressPosition);
                }
                return false;
            }
            case MouseButtonRelease -> {
                resetPress();
                return false;
            }
            case MouseMove -> {
                if (event instanceof QMouseEvent mouseEvent && shouldStartDrag(mouseEvent)) {
                    startDrag(container, handler);
                    return true;
                }
                return false;
            }
            case DragEnter -> {
                QDropEvent dropEvent = (QDropEvent) event;
                myHoverTransfer = DesktopQtDataTransfers.fromDropEvent(dropEvent);
                dropEvent.acceptProposedAction();
                return true;
            }
            case DragMove -> {
                dragMove(container, (QDragMoveEvent) event, handler);
                return true;
            }
            case DragLeave -> {
                finishHover();
                return true;
            }
            case Drop -> {
                drop((QDropEvent) event, handler);
                return true;
            }
            default -> {
                return false;
            }
        }
    }

    private boolean shouldStartDrag(QMouseEvent event) {
        QPoint pressPosition = myPressPosition;
        if (pressPosition == null || myPressChild == null || !event.buttons().testFlag(Qt.MouseButton.LeftButton)) {
            return false;
        }

        QPoint position = event.position().toPoint();
        int distance = Math.abs(position.x() - pressPosition.x()) + Math.abs(position.y() - pressPosition.y());
        return distance >= QApplication.startDragDistance();
    }

    private void resetPress() {
        myPressPosition = null;
        myPressChild = null;
    }

    private void startDrag(QWidget container, DragAndDropTransferHandler<Component> handler) {
        Component child = myPressChild;
        QPoint pressPosition = myPressPosition;
        resetPress();

        QWidget widget = child == null ? null : widgetOf(child);
        if (widget == null || pressPosition == null) {
            return;
        }

        boolean move = !DesktopQtDragAndDropSupport.isCopyGesture(QGuiApplication.keyboardModifiers());
        DataTransfer transfer = handler.createDragTransfer(myLayout, List.of(child), move);
        if (transfer == null || transfer.isEmpty()) {
            return;
        }

        QDrag drag = new QDrag(container);
        drag.setMimeData(DesktopQtDataTransfers.toMimeData(transfer));
        drag.setPixmap(widget.grab());
        drag.setHotSpot(new QPoint(pressPosition.x() - widget.x(), pressPosition.y() - widget.y()));

        myDraggedChildren = List.of(child);
        try {
            DesktopQtDragSession.exec(drag,
                new Qt.DropActions(Qt.DropAction.CopyAction, Qt.DropAction.MoveAction),
                move ? Qt.DropAction.MoveAction : Qt.DropAction.CopyAction);
        }
        finally {
            myDraggedChildren = List.of();
        }
    }

    private void dragMove(QWidget container, QDragMoveEvent event, DragAndDropTransferHandler<Component> handler) {
        DesktopQtDrop<Component> drop = resolveDrop(event.position().toPoint());
        if (drop == null || !handler.drop(myLayout, new DesktopQtDropContext<>(drop, myHoverTransfer, myDraggedChildren, true))) {
            event.ignore();
            showIndicator(container, null);
            return;
        }

        event.acceptProposedAction();
        showIndicator(container, drop.indicator());
    }

    private void drop(QDropEvent event, DragAndDropTransferHandler<Component> handler) {
        finishHover();

        DesktopQtDrop<Component> drop = resolveDrop(event.position().toPoint());
        if (drop == null) {
            event.ignore();
            return;
        }

        DesktopQtDropContext<Component> context =
            new DesktopQtDropContext<>(drop, DesktopQtDataTransfers.fromDropEvent(event), myDraggedChildren, true);
        if (!handler.drop(myLayout, context)) {
            event.ignore();
            return;
        }

        event.acceptProposedAction();

        DesktopQtDropContext<Component> performing = context.toPerforming();
        DesktopQtDragSession.runAfterDrag(() -> handler.drop(myLayout, performing));
    }

    private @Nullable DesktopQtDrop<Component> resolveDrop(QPoint position) {
        List<Component> children = children();
        if (children.isEmpty()) {
            return null;
        }

        Component child = childAt(position);
        boolean before = false;
        if (child == null) {
            child = children.get(children.size() - 1);
        }

        QWidget widget = widgetOf(child);
        if (widget == null) {
            return null;
        }

        QRect rect = widget.geometry();
        if (rect.contains(position)) {
            before = position.x() < rect.center().x();
        }

        int x = before ? rect.left() : rect.right() + 1 - INDICATOR_WIDTH;
        QRect indicator = new QRect(Math.max(0, x), rect.top(), INDICATOR_WIDTH, rect.height());
        return new DesktopQtDrop<>(child,
            before ? DragAndDropTransferHandler.DropPosition.ABOVE : DragAndDropTransferHandler.DropPosition.BELOW,
            indicator);
    }

    private List<Component> children() {
        List<Component> children = new ArrayList<>();
        myLayout.forEachChild(children::add);
        return children;
    }

    private @Nullable Component childAt(QPoint position) {
        for (Component child : children()) {
            QWidget widget = widgetOf(child);
            if (widget != null && widget.isVisible() && widget.geometry().contains(position)) {
                return child;
            }
        }
        return null;
    }

    private static @Nullable QWidget widgetOf(Component child) {
        if (child instanceof QtComponentDelegate<?> delegate) {
            QWidget widget = delegate.toQtComponent();
            return widget == null || widget.isDisposed() ? null : widget;
        }
        return null;
    }

    private void finishHover() {
        myHoverTransfer = DataTransfer.EMPTY;
        QWidget indicator = myIndicator;
        if (indicator != null && !indicator.isDisposed()) {
            indicator.hide();
        }
    }

    private void showIndicator(QWidget container, @Nullable QRect rect) {
        QWidget indicator = myIndicator;
        if (rect == null) {
            if (indicator != null && !indicator.isDisposed()) {
                indicator.hide();
            }
            return;
        }

        if (indicator == null || indicator.isDisposed()) {
            indicator = new QWidget(container);
            indicator.setAttribute(Qt.WidgetAttribute.WA_TransparentForMouseEvents);
            indicator.setStyleSheet("background-color: " + container.palette().color(QPalette.ColorRole.Highlight).name() + ";");
            myIndicator = indicator;
        }

        indicator.setGeometry(rect);
        indicator.raise();
        indicator.show();
    }
}
