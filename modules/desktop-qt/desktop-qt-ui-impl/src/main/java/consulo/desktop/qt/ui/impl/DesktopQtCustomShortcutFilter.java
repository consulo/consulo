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

import consulo.dataContext.DataManager;
import consulo.ui.Component;
import consulo.ui.event.details.KeyCode;
import consulo.ui.event.details.ModifiedInputDetails;
import consulo.ui.ex.action.AnAction;
import consulo.ui.ex.impl.internal.action.UnifiedCustomShortcutDispatcher;
import io.qt.core.QCoreApplication;
import io.qt.core.QEvent;
import io.qt.core.QObject;
import io.qt.gui.QKeyEvent;
import io.qt.widgets.QApplication;
import io.qt.widgets.QWidget;
import org.jspecify.annotations.Nullable;

import javax.swing.*;
import java.awt.event.InputEvent;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;

/**
 * @author VISTALL
 * @since 2026-10-02
 */
public final class DesktopQtCustomShortcutFilter {
    private static @Nullable QObject ourFilter;

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
                if (type != QEvent.Type.KeyPress && type != QEvent.Type.ShortcutOverride) {
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
                    return false;
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
