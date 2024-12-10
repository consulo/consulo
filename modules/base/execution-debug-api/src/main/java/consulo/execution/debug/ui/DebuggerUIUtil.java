/*
 * Copyright 2013-2024 consulo.io
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
package consulo.execution.debug.ui;

import consulo.application.ui.wm.IdeFocusManager;
import consulo.codeEditor.Editor;
import consulo.codeEditor.LogicalPosition;
import consulo.project.Project;
import consulo.ui.ex.RelativePoint;
import consulo.ui.ex.action.ActionManager;
import consulo.ui.ex.action.util.ShortcutUtil;
import consulo.ui.ex.popup.ComponentPopupBuilder;
import consulo.ui.ex.popup.JBPopup;
import consulo.ui.ex.popup.JBPopupFactory;
import consulo.ui.ex.popup.ListPopup;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;

/**
 * @author VISTALL
 * @since 2024-12-10
 */
public class DebuggerUIUtil {
    public static final String FULL_VALUE_POPUP_DIMENSION_KEY = "XDebugger.FullValuePopup";

    public static JBPopup createValuePopup(Project project, JComponent component, @Nullable  Runnable cancelCallback) {
        ComponentPopupBuilder builder = JBPopupFactory.getInstance().createComponentPopupBuilder(component, null);
        builder.setResizable(true)
            .setMovable(true)
            .setDimensionServiceKey(project, FULL_VALUE_POPUP_DIMENSION_KEY, false)
            .setRequestFocus(false);

        if (cancelCallback != null) {
            builder.setCancelCallback(() -> {
                cancelCallback.run();
                return true;
            });
        }
        return builder.createPopup();
    }

    public static void focusEditorOnCheck(final JCheckBox checkbox, final JComponent component) {
        final Runnable runnable = () -> IdeFocusManager.getGlobalInstance().doForceFocusWhenFocusSettlesDown(component);
        checkbox.addActionListener(e -> {
            if (checkbox.isSelected()) {
                SwingUtilities.invokeLater(runnable);
            }
        });
    }

    public static void registerExtraHandleShortcuts(final ListPopup popup, String... actionNames) {
        for (String name : actionNames) {
            KeyStroke stroke = ShortcutUtil.getKeyStroke(ActionManager.getInstance().getAction(name).getShortcutSet());
            if (stroke != null) {
                popup.registerAction("handleSelection " + stroke, stroke, new AbstractAction() {
                    @Override
                    public void actionPerformed(ActionEvent e) {
                        popup.handleSelect(true);
                    }
                });
            }
        }
    }

    public static void showPopupForEditorLine(@Nonnull JBPopup popup, @Nonnull Editor editor, int line) {
        RelativePoint point = getPositionForPopup(editor, line);
        if (point != null) {
            popup.show(point);
        }
        else {
            Project project = editor.getProject();
            if (project != null) {
                popup.showCenteredInCurrentWindow(project);
            }
            else {
                popup.showInFocusCenter();
            }
        }
    }

    @Nullable
    public static RelativePoint getPositionForPopup(@Nonnull Editor editor, int line) {
        Point p = editor.logicalPositionToXY(new LogicalPosition(line + 1, 0));
        return editor.getScrollingModel().getVisibleArea().contains(p) ? new RelativePoint(editor.getContentComponent(), p) : null;
    }
}
