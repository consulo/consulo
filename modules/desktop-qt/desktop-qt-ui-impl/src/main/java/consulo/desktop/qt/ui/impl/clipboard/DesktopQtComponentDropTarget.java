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

import consulo.ui.Component;
import consulo.ui.DropHandler;
import consulo.ui.clipboard.DataTransfer;
import io.qt.core.QEvent;
import io.qt.core.QObject;
import io.qt.gui.QDropEvent;
import io.qt.widgets.QWidget;
import org.jspecify.annotations.Nullable;

import java.util.function.Supplier;

/**
 * @author VISTALL
 * @since 2026-10-12
 */
public final class DesktopQtComponentDropTarget extends QObject {
    public static void install(QWidget widget, Component component, Supplier<@Nullable DropHandler> handler) {
        widget.setAcceptDrops(handler.get() != null);
        widget.installEventFilter(new DesktopQtComponentDropTarget(widget, component, handler));
    }

    private final Component myComponent;
    private final Supplier<@Nullable DropHandler> myHandler;

    private DesktopQtComponentDropTarget(QWidget widget, Component component, Supplier<@Nullable DropHandler> handler) {
        super(widget);
        myComponent = component;
        myHandler = handler;
    }

    @Override
    public boolean eventFilter(QObject watched, QEvent event) {
        switch (event.type()) {
            case DragEnter, DragMove -> {
                QDropEvent dropEvent = (QDropEvent) event;
                DropHandler handler = myHandler.get();
                if (handler != null && handler.drop(myComponent, DesktopQtDataTransfers.fromDropEvent(dropEvent), true)) {
                    dropEvent.acceptProposedAction();
                }
                else {
                    dropEvent.ignore();
                }
                return true;
            }
            case Drop -> {
                QDropEvent dropEvent = (QDropEvent) event;
                DropHandler handler = myHandler.get();
                DataTransfer transfer = DesktopQtDataTransfers.fromDropEvent(dropEvent);
                if (handler == null || !handler.drop(myComponent, transfer, true)) {
                    dropEvent.ignore();
                    return true;
                }

                dropEvent.acceptProposedAction();
                DesktopQtDragSession.runAfterDrag(() -> handler.drop(myComponent, transfer, false));
                return true;
            }
            default -> {
                return false;
            }
        }
    }
}
