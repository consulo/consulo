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

import consulo.desktop.qt.ui.impl.clipboard.DesktopQtDataTransfers;
import consulo.desktop.qt.ui.impl.clipboard.DesktopQtDragAndDropSupport;
import consulo.desktop.qt.ui.impl.clipboard.DesktopQtDragSession;
import consulo.desktop.qt.ui.impl.clipboard.DesktopQtDrop;
import consulo.desktop.qt.ui.impl.clipboard.DesktopQtDropContext;
import consulo.ui.DragAndDropTransferHandler;
import consulo.ui.Tab;
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
import io.qt.widgets.QTabBar;
import io.qt.widgets.QWidget;
import org.jspecify.annotations.Nullable;

import java.util.List;

/**
 * @author VISTALL
 * @since 2026-10-12
 */
final class DesktopQtTabDragAndDrop {
    private static final int INDICATOR_WIDTH = 2;

    private final DesktopQtTabbedLayoutImpl myLayout;

    private @Nullable QPoint myPressPosition;
    private int myPressIndex = -1;

    private List<Tab> myDraggedTabs = List.of();
    private DataTransfer myHoverTransfer = DataTransfer.EMPTY;

    private @Nullable QWidget myIndicator;

    DesktopQtTabDragAndDrop(DesktopQtTabbedLayoutImpl layout) {
        myLayout = layout;
    }

    void install(QTabBar tabBar) {
        tabBar.setAcceptDrops(true);
        tabBar.installEventFilter(new QObject(tabBar) {
            @Override
            public boolean eventFilter(QObject watched, QEvent event) {
                return filter(tabBar, event);
            }
        });
    }

    private boolean filter(QTabBar tabBar, QEvent event) {
        DragAndDropTransferHandler<Tab> handler = myLayout.getDragAndDropHandler();
        if (handler == null) {
            return false;
        }

        switch (event.type()) {
            case MouseButtonPress -> {
                if (event instanceof QMouseEvent mouseEvent && mouseEvent.button() == Qt.MouseButton.LeftButton) {
                    myPressPosition = mouseEvent.position().toPoint();
                    myPressIndex = tabBar.tabAt(myPressPosition);
                }
                return false;
            }
            case MouseButtonRelease -> {
                resetPress();
                return false;
            }
            case MouseMove -> {
                if (event instanceof QMouseEvent mouseEvent && shouldStartDrag(mouseEvent)) {
                    startDrag(tabBar, handler);
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
                dragMove(tabBar, (QDragMoveEvent) event, handler);
                return true;
            }
            case DragLeave -> {
                finishHover();
                return true;
            }
            case Drop -> {
                drop(tabBar, (QDropEvent) event, handler);
                return true;
            }
            default -> {
                return false;
            }
        }
    }

    private boolean shouldStartDrag(QMouseEvent event) {
        QPoint pressPosition = myPressPosition;
        if (pressPosition == null || myPressIndex == -1 || !event.buttons().testFlag(Qt.MouseButton.LeftButton)) {
            return false;
        }

        QPoint position = event.position().toPoint();
        int distance = Math.abs(position.x() - pressPosition.x()) + Math.abs(position.y() - pressPosition.y());
        return distance >= QApplication.startDragDistance();
    }

    private void resetPress() {
        myPressPosition = null;
        myPressIndex = -1;
    }

    private void startDrag(QTabBar tabBar, DragAndDropTransferHandler<Tab> handler) {
        int index = myPressIndex;
        QPoint pressPosition = myPressPosition;
        resetPress();

        DesktopQtTabImpl tab = myLayout.tabAtIndex(index);
        if (tab == null || pressPosition == null) {
            return;
        }

        boolean move = !DesktopQtDragAndDropSupport.isCopyGesture(QGuiApplication.keyboardModifiers());
        DataTransfer transfer = handler.createDragTransfer(myLayout, List.of(tab), move);
        if (transfer == null || transfer.isEmpty()) {
            return;
        }

        QDrag drag = new QDrag(tabBar);
        drag.setMimeData(DesktopQtDataTransfers.toMimeData(transfer));

        QRect rect = tabBar.tabRect(index);
        drag.setPixmap(tabBar.grab(rect));
        drag.setHotSpot(new QPoint(pressPosition.x() - rect.x(), pressPosition.y() - rect.y()));

        myDraggedTabs = List.of(tab);
        try {
            DesktopQtDragSession.exec(drag,
                new Qt.DropActions(Qt.DropAction.CopyAction, Qt.DropAction.MoveAction),
                move ? Qt.DropAction.MoveAction : Qt.DropAction.CopyAction);
        }
        finally {
            myDraggedTabs = List.of();
        }
    }

    private void dragMove(QTabBar tabBar, QDragMoveEvent event, DragAndDropTransferHandler<Tab> handler) {
        DesktopQtDrop<Tab> drop = resolveDrop(tabBar, event.position().toPoint());
        if (drop == null || !handler.drop(myLayout, new DesktopQtDropContext<>(drop, myHoverTransfer, myDraggedTabs, true))) {
            event.ignore();
            showIndicator(tabBar, null);
            return;
        }

        event.acceptProposedAction();
        showIndicator(tabBar, drop.indicator());
    }

    private void drop(QTabBar tabBar, QDropEvent event, DragAndDropTransferHandler<Tab> handler) {
        finishHover();

        DesktopQtDrop<Tab> drop = resolveDrop(tabBar, event.position().toPoint());
        if (drop == null) {
            event.ignore();
            return;
        }

        DesktopQtDropContext<Tab> context = new DesktopQtDropContext<>(drop, DesktopQtDataTransfers.fromDropEvent(event), myDraggedTabs, true);
        if (!handler.drop(myLayout, context)) {
            event.ignore();
            return;
        }

        event.acceptProposedAction();

        DesktopQtDropContext<Tab> performing = context.toPerforming();
        DesktopQtDragSession.runAfterDrag(() -> handler.drop(myLayout, performing));
    }

    private @Nullable DesktopQtDrop<Tab> resolveDrop(QTabBar tabBar, QPoint position) {
        int count = tabBar.count();
        if (count == 0) {
            return null;
        }

        boolean horizontal = isHorizontal(tabBar);

        int index = tabBar.tabAt(position);
        boolean before;
        if (index == -1) {
            index = count - 1;
            before = false;
        }
        else {
            QRect rect = tabBar.tabRect(index);
            before = horizontal ? position.x() < rect.center().x() : position.y() < rect.center().y();
        }

        DesktopQtTabImpl tab = myLayout.tabAtIndex(index);
        if (tab == null) {
            return null;
        }

        QRect rect = tabBar.tabRect(index);
        QRect indicator;
        if (horizontal) {
            int x = before ? rect.left() : rect.right() + 1 - INDICATOR_WIDTH;
            indicator = new QRect(Math.max(0, x), rect.top(), INDICATOR_WIDTH, rect.height());
        }
        else {
            int y = before ? rect.top() : rect.bottom() + 1 - INDICATOR_WIDTH;
            indicator = new QRect(rect.left(), Math.max(0, y), rect.width(), INDICATOR_WIDTH);
        }

        return new DesktopQtDrop<>(tab,
            before ? DragAndDropTransferHandler.DropPosition.ABOVE : DragAndDropTransferHandler.DropPosition.BELOW,
            indicator);
    }

    private static boolean isHorizontal(QTabBar tabBar) {
        return switch (tabBar.shape()) {
            case RoundedNorth, RoundedSouth, TriangularNorth, TriangularSouth -> true;
            default -> false;
        };
    }

    private void finishHover() {
        myHoverTransfer = DataTransfer.EMPTY;
        QWidget indicator = myIndicator;
        if (indicator != null && !indicator.isDisposed()) {
            indicator.hide();
        }
    }

    private void showIndicator(QTabBar tabBar, @Nullable QRect rect) {
        QWidget indicator = myIndicator;
        if (rect == null) {
            if (indicator != null && !indicator.isDisposed()) {
                indicator.hide();
            }
            return;
        }

        if (indicator == null || indicator.isDisposed()) {
            indicator = new QWidget(tabBar);
            indicator.setAttribute(Qt.WidgetAttribute.WA_TransparentForMouseEvents);
            indicator.setStyleSheet("background-color: " + tabBar.palette().color(QPalette.ColorRole.Highlight).name() + ";");
            myIndicator = indicator;
        }

        indicator.setGeometry(rect);
        indicator.raise();
        indicator.show();
    }
}
