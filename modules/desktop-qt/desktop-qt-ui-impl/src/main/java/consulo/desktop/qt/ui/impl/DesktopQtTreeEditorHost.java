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
import io.qt.core.QMetaObject;
import io.qt.core.QObject;
import io.qt.core.Qt;
import io.qt.gui.QKeyEvent;
import io.qt.widgets.QApplication;
import io.qt.widgets.QHBoxLayout;
import io.qt.widgets.QWidget;
import org.jspecify.annotations.Nullable;

/**
 * @author VISTALL
 * @since 2026-10-17
 */
final class DesktopQtTreeEditorHost extends QWidget {
    private final Runnable myStop;
    private final Runnable myCancel;

    private @Nullable QWidget myLastFocus;
    private QMetaObject.@Nullable Connection myFocusConnection;

    DesktopQtTreeEditorHost(QWidget parent, Runnable stop, Runnable cancel) {
        super(parent);
        myStop = stop;
        myCancel = cancel;

        QHBoxLayout layout = new QHBoxLayout(this);
        layout.setContentsMargins(0, 0, 0, 0);
        layout.setSpacing(0);

        setAutoFillBackground(false);
    }

    void setEditor(QWidget editor) {
        QHBoxLayout layout = (QHBoxLayout) layout();
        if (layout != null) {
            layout.addWidget(editor, 1);
        }
    }

    void focusEditor() {
        QWidget target = firstFocusable(this);
        if (target != null) {
            target.setFocus(Qt.FocusReason.OtherFocusReason);
        }
    }

    void trackFocus() {
        QApplication application = QApplication.instance();
        if (application == null || myFocusConnection != null) {
            return;
        }

        myFocusConnection = application.focusChanged.connect((from, to) -> {
            if (!isDisposed() && hasFocusInside(to)) {
                myLastFocus = to;
            }
        });
    }

    void untrackFocus() {
        QMetaObject.Connection connection = myFocusConnection;
        QApplication application = QApplication.instance();
        myFocusConnection = null;
        myLastFocus = null;
        if (connection != null && application != null) {
            application.focusChanged.disconnect(connection);
        }
    }

    void restoreFocus() {
        QWidget last = myLastFocus;
        if (last != null && !last.isDisposed() && hasFocusInside(last)) {
            last.setFocus(Qt.FocusReason.OtherFocusReason);
        }
        else {
            focusEditor();
        }
    }

    boolean hasFocusInside(@Nullable QWidget focused) {
        for (QWidget current = focused; current != null; current = current.parentWidget()) {
            if (current == this) {
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean event(@Nullable QEvent event) {
        if (event != null && event.type() == QEvent.Type.ShortcutOverride && event instanceof QKeyEvent keyEvent && isEditingKey(keyEvent)) {
            event.accept();
            return true;
        }
        return super.event(event);
    }

    @Override
    protected void keyPressEvent(@Nullable QKeyEvent event) {
        if (event != null && isEditingKey(event)) {
            event.accept();
            if (event.key() == Qt.Key.Key_Escape.value()) {
                myCancel.run();
            }
            else {
                myStop.run();
            }
            return;
        }
        super.keyPressEvent(event);
    }

    private static boolean isEditingKey(QKeyEvent event) {
        int modifiers = event.modifiers().value() & ~Qt.KeyboardModifier.KeypadModifier.value();
        if (modifiers != 0) {
            return false;
        }

        int key = event.key();
        return key == Qt.Key.Key_Return.value() || key == Qt.Key.Key_Enter.value() || key == Qt.Key.Key_Escape.value();
    }

    private static @Nullable QWidget firstFocusable(QWidget widget) {
        for (QObject child : widget.children()) {
            if (!(child instanceof QWidget childWidget) || childWidget.isHidden()) {
                continue;
            }

            if (childWidget.focusPolicy() != Qt.FocusPolicy.NoFocus) {
                return childWidget;
            }

            QWidget nested = firstFocusable(childWidget);
            if (nested != null) {
                return nested;
            }
        }
        return null;
    }
}
