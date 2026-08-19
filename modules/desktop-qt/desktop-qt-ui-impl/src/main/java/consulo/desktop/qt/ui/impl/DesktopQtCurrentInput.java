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

import consulo.ui.Point2D;
import consulo.ui.event.details.InputDetails;
import consulo.ui.event.details.KeyCode;
import consulo.ui.event.details.KeyboardInputDetails;
import consulo.ui.event.details.ModifiedInputDetails;
import consulo.ui.event.details.MouseInputDetails;
import io.qt.core.QCoreApplication;
import io.qt.core.QEvent;
import io.qt.core.QObject;
import io.qt.core.QPoint;
import io.qt.core.QTimer;
import io.qt.gui.QCursor;
import io.qt.gui.QKeyEvent;
import io.qt.gui.QMouseEvent;
import io.qt.widgets.QWidget;
import org.jspecify.annotations.Nullable;

import java.util.EnumSet;

/**
 * The qt counterpart of {@code EventQueue.getCurrentEvent()}. A signal carries no input event - a selection
 * change or a toggle reports only the fact of the change - so an application wide filter remembers the mouse
 * press or the key press being dispatched, and a handler running inside that dispatch asks for it here. The
 * record is dropped on the next turn of the event loop, so a change made programmatically - from a timer or a
 * queued call - answers null rather than the stale gesture that happened to come before it.
 * <p>
 * All of it lives on the qt thread - the filter runs there and so do the callers.
 *
 * @author VISTALL
 * @since 2026-08-19
 */
public final class DesktopQtCurrentInput {
    private record Snapshot(
        boolean mouse,
        int screenX,
        int screenY,
        EnumSet<ModifiedInputDetails.Modifier> modifiers,
        MouseInputDetails.@Nullable MouseButton button,
        @Nullable KeyCode key
    ) {
    }

    // the filter must stay referenced - qt drops the native peer of a collected QObject and the filter with it
    private static @Nullable QObject ourFilter;
    private static @Nullable Snapshot ourCurrent;
    private static long ourGeneration;

    public static void install() {
        if (ourFilter != null) {
            return;
        }

        QObject filter = new QObject() {
            @Override
            public boolean eventFilter(QObject watched, QEvent event) {
                QEvent.Type type = event.type();
                if (event instanceof QMouseEvent mouseEvent
                    && (type == QEvent.Type.MouseButtonPress
                    || type == QEvent.Type.MouseButtonRelease
                    || type == QEvent.Type.MouseButtonDblClick)) {
                    remember(snapshot(mouseEvent));
                }
                else if (event instanceof QKeyEvent keyEvent && type == QEvent.Type.KeyPress) {
                    remember(snapshot(keyEvent));
                }
                return false;
            }
        };

        QCoreApplication.instance().installEventFilter(filter);
        ourFilter = filter;
    }

    /**
     * The details of the input event being dispatched right now, placed against the widget, or null when
     * nothing user driven is in flight.
     */
    public static @Nullable InputDetails current(@Nullable QWidget widget) {
        Snapshot snapshot = ourCurrent;
        if (snapshot == null) {
            return null;
        }

        QPoint onScreen = new QPoint(snapshot.screenX(), snapshot.screenY());
        QPoint position = widget != null && !widget.isDisposed() ? widget.mapFromGlobal(onScreen) : onScreen;

        Point2D pos = new Point2D(position.x(), position.y());
        Point2D posOnScreen = new Point2D(onScreen.x(), onScreen.y());

        return snapshot.mouse()
            ? new MouseInputDetails(pos, posOnScreen, snapshot.modifiers(), snapshot.button())
            : new KeyboardInputDetails(pos, posOnScreen, snapshot.modifiers(), snapshot.key());
    }

    private static Snapshot snapshot(QMouseEvent event) {
        QPoint onScreen = event.globalPosition().toPoint();
        return new Snapshot(
            true,
            onScreen.x(),
            onScreen.y(),
            DesktopQtInputDetails.modifiers(event.modifiers()),
            DesktopQtInputDetails.button(event.button()),
            null
        );
    }

    private static Snapshot snapshot(QKeyEvent event) {
        QPoint onScreen = QCursor.pos();
        EnumSet<ModifiedInputDetails.Modifier> modifiers = DesktopQtInputDetails.modifiers(event.modifiers());
        return new Snapshot(
            false,
            onScreen.x(),
            onScreen.y(),
            modifiers,
            null,
            DesktopQtInputDetails.keyCode(event, modifiers)
        );
    }

    private static void remember(Snapshot snapshot) {
        ourCurrent = snapshot;

        long generation = ++ourGeneration;
        QTimer.singleShot(0, () -> {
            if (ourGeneration == generation) {
                ourCurrent = null;
            }
        });
    }
}
