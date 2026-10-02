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

import consulo.dataContext.DataContext;
import consulo.dataContext.DataManager;
import consulo.ui.Component;
import consulo.ui.UIAccess;
import consulo.ui.event.details.KeyCode;
import consulo.ui.event.details.ModifiedInputDetails;
import consulo.ui.ex.action.ActionManager;
import consulo.ui.ex.action.AnAction;
import consulo.ui.ex.impl.internal.action.UnifiedCustomShortcutDispatcher;
import consulo.ui.ex.keymap.Keymap;
import consulo.ui.ex.keymap.KeymapManager;
import io.qt.core.QCoreApplication;
import io.qt.core.QEvent;
import io.qt.core.QObject;
import io.qt.core.Qt;
import io.qt.gui.QKeyEvent;
import io.qt.gui.QKeySequence;
import io.qt.widgets.QAbstractSpinBox;
import io.qt.widgets.QApplication;
import io.qt.widgets.QKeySequenceEdit;
import io.qt.widgets.QLineEdit;
import io.qt.widgets.QMenu;
import io.qt.widgets.QPlainTextEdit;
import io.qt.widgets.QTextEdit;
import io.qt.widgets.QWidget;
import org.jspecify.annotations.Nullable;

import javax.swing.*;
import java.awt.event.InputEvent;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

/**
 * @author VISTALL
 * @since 2026-10-02
 */
public final class DesktopQtCustomShortcutFilter {
    private static final Set<Qt.Key> TEXT_EDITING_KEYS = Set.of(
        Qt.Key.Key_Left,
        Qt.Key.Key_Right,
        Qt.Key.Key_Up,
        Qt.Key.Key_Down,
        Qt.Key.Key_Home,
        Qt.Key.Key_End,
        Qt.Key.Key_PageUp,
        Qt.Key.Key_PageDown,
        Qt.Key.Key_Backspace,
        Qt.Key.Key_Delete,
        Qt.Key.Key_Insert
    );

    private static final Set<QKeySequence.StandardKey> TEXT_EDITING_SEQUENCES = Set.of(
        QKeySequence.StandardKey.Copy,
        QKeySequence.StandardKey.Cut,
        QKeySequence.StandardKey.Paste,
        QKeySequence.StandardKey.Undo,
        QKeySequence.StandardKey.Redo,
        QKeySequence.StandardKey.SelectAll
    );

    private static @Nullable QObject ourFilter;
    private static boolean ourRedelivering;
    private static CompletableFuture<?> ourStrokeQueue = CompletableFuture.completedFuture(null);

    private DesktopQtCustomShortcutFilter() {
    }

    public static void install() {
        if (ourFilter != null) {
            return;
        }

        QObject filter = new QObject() {
            @Override
            public boolean eventFilter(QObject watched, QEvent event) {
                QEvent.Type type = event.type();
                if (ourRedelivering || type != QEvent.Type.KeyPress && type != QEvent.Type.ShortcutOverride) {
                    return false;
                }

                if (!(event instanceof QKeyEvent keyEvent) || !(watched instanceof QWidget widget) || widget != QApplication.focusWidget()) {
                    return false;
                }

                KeyStroke keyStroke = toKeyStroke(keyEvent);
                if (keyStroke == null) {
                    return false;
                }

                List<Component> components = componentsOf(widget);
                List<AnAction> actions = UnifiedCustomShortcutDispatcher.collectActions(components, keyStroke);
                if (actions.isEmpty()) {
                    return type == QEvent.Type.KeyPress && performKeymapActions(widget, keyEvent, keyStroke, components);
                }

                event.accept();

                if (type == QEvent.Type.KeyPress) {
                    UnifiedCustomShortcutDispatcher.performFirstEnabled(actions, DataManager.getInstance().getDataContext(components.get(0)));
                }
                return true;
            }
        };

        QCoreApplication.instance().installEventFilter(filter);
        ourFilter = filter;
    }

    private static boolean performKeymapActions(QWidget widget, QKeyEvent event, KeyStroke keyStroke, List<Component> components) {
        if (components.isEmpty() || !acceptsKeymap(widget, event, keyStroke)) {
            return false;
        }

        List<AnAction> actions = collectKeymapActions(keyStroke);
        if (actions.isEmpty()) {
            return false;
        }

        event.accept();

        QKeyEvent copy = new QKeyEvent(
            event.type(),
            event.key(),
            event.modifiers(),
            event.nativeScanCode(),
            event.nativeVirtualKey(),
            event.nativeModifiers(),
            event.text(),
            event.isAutoRepeat(),
            (short) event.count()
        );
        DataContext context = DataManager.getInstance().getDataContext(components.get(0));
        UIAccess uiAccess = UIAccess.current();
        ourStrokeQueue = ourStrokeQueue
            .exceptionally(throwable -> null)
            .thenComposeAsync(ignored -> UnifiedCustomShortcutDispatcher.performFirstEnabled(actions, context), uiAccess)
            .whenCompleteAsync((performed, throwable) -> {
                if (!Boolean.TRUE.equals(performed)) {
                    redeliver(widget, copy);
                }
            }, uiAccess);
        return true;
    }

    private static void redeliver(QWidget widget, QKeyEvent event) {
        if (widget.isDisposed()) {
            return;
        }

        ourRedelivering = true;
        try {
            QCoreApplication.sendEvent(widget, event);
        }
        finally {
            ourRedelivering = false;
        }
    }

    private static List<AnAction> collectKeymapActions(KeyStroke keyStroke) {
        Keymap keymap = KeymapManager.getInstance().getActiveKeymap();
        if (keymap == null) {
            return List.of();
        }

        boolean modalContext = QApplication.activeModalWidget() != null;
        ActionManager actionManager = ActionManager.getInstance();
        List<AnAction> actions = new ArrayList<>();
        for (String actionId : keymap.getActionIds(keyStroke)) {
            AnAction action = actionManager.getAction(actionId);
            if (action != null && (!modalContext || action.isEnabledInModalContext())) {
                actions.add(action);
            }
        }
        actions.sort(Comparator.comparing(AnAction::getExecuteWeight).reversed());
        return actions;
    }

    private static boolean acceptsKeymap(QWidget widget, QKeyEvent event, KeyStroke keyStroke) {
        if (QApplication.activePopupWidget() != null) {
            return false;
        }

        for (QWidget current = widget; current != null && !current.isDisposed(); current = current.parentWidget()) {
            if (current instanceof DesktopQtKeymapWidget || current instanceof QMenu || current.windowType() == Qt.WindowType.Popup) {
                return false;
            }
        }

        if (!isTextInput(widget)) {
            return true;
        }

        if (isFunctionKey(event)) {
            return true;
        }

        int modifiers = keyStroke.getModifiers();
        boolean commandModifier = (modifiers & (InputEvent.CTRL_DOWN_MASK | InputEvent.ALT_DOWN_MASK | InputEvent.META_DOWN_MASK)) != 0;
        return commandModifier && !isTextEditingKey(event);
    }

    private static boolean isTextInput(QWidget widget) {
        return widget instanceof QLineEdit
            || widget instanceof QTextEdit
            || widget instanceof QPlainTextEdit
            || widget instanceof QAbstractSpinBox
            || widget instanceof QKeySequenceEdit;
    }

    private static boolean isFunctionKey(QKeyEvent event) {
        int key = event.key();
        return key >= Qt.Key.Key_F1.value() && key <= Qt.Key.Key_F24.value();
    }

    private static boolean isTextEditingKey(QKeyEvent event) {
        for (QKeySequence.StandardKey standardKey : TEXT_EDITING_SEQUENCES) {
            if (event.matches(standardKey)) {
                return true;
            }
        }
        for (Qt.Key key : TEXT_EDITING_KEYS) {
            if (event.key() == key.value()) {
                return true;
            }
        }
        return false;
    }

    private static List<Component> componentsOf(QWidget widget) {
        List<Component> components = new ArrayList<>();
        for (QWidget current = widget; current != null && !current.isDisposed(); current = current.parentWidget()) {
            Component component = TargetQt.from(current);
            if (component != null) {
                components.add(component);
            }
        }
        return components;
    }

    private static @Nullable KeyStroke toKeyStroke(QKeyEvent event) {
        EnumSet<ModifiedInputDetails.Modifier> modifiers = DesktopQtInputDetails.modifiers(event.modifiers());
        KeyCode keyCode = DesktopQtInputDetails.keyCode(event, modifiers);
        if (keyCode == null) {
            return null;
        }

        int awtModifiers = 0;
        if (modifiers.contains(ModifiedInputDetails.Modifier.SHIFT)) {
            awtModifiers |= InputEvent.SHIFT_DOWN_MASK;
        }
        if (modifiers.contains(ModifiedInputDetails.Modifier.CTRL)) {
            awtModifiers |= InputEvent.CTRL_DOWN_MASK;
        }
        if (modifiers.contains(ModifiedInputDetails.Modifier.ALT)) {
            awtModifiers |= InputEvent.ALT_DOWN_MASK;
        }
        if (modifiers.contains(ModifiedInputDetails.Modifier.META)) {
            awtModifiers |= InputEvent.META_DOWN_MASK;
        }
        return KeyStroke.getKeyStroke(keyCode.key(), awtModifiers);
    }
}
