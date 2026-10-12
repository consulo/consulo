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
package consulo.fileEditor.impl.internal.unified;

import consulo.disposer.Disposable;
import consulo.ide.localize.IdeLocalize;
import consulo.localize.LocalizeValue;
import consulo.project.Project;
import consulo.project.ui.impl.internal.wm.action.ActivateToolWindowAction;
import consulo.project.ui.wm.ToolWindowId;
import consulo.project.ui.wm.ToolWindowManager;
import consulo.project.ui.wm.ToolWindowManagerListener;
import consulo.ui.Component;
import consulo.ui.HorizontalAlignment;
import consulo.ui.Label;
import consulo.ui.LabelStyle;
import consulo.ui.Space;
import consulo.ui.StaticPosition;
import consulo.ui.VerticalAlignment;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.action.IdeActions;
import consulo.ui.ex.action.Shortcut;
import consulo.ui.ex.action.util.ShortcutUtil;
import consulo.ui.ex.keymap.KeymapManager;
import consulo.ui.ex.keymap.util.KeymapUtil;
import consulo.ui.ex.toolWindow.ToolWindow;
import consulo.ui.layout.TableLayout;
import consulo.ui.layout.VerticalLayout;
import consulo.ui.style.ComponentColors;
import consulo.util.lang.Pair;
import org.jspecify.annotations.Nullable;

import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.function.BooleanSupplier;
import javax.swing.*;

/**
 * @author VISTALL
 * @since 2026-10-12
 */
public class EditorEmptyTextPanel {
    private static final KeyStroke SHIFT_DEFAULT = KeyStroke.getKeyStroke(KeyEvent.VK_SHIFT, 0);

    private final Project myProject;
    private final VerticalLayout myRoot;

    @RequiredUIAccess
    public EditorEmptyTextPanel(Project project, Disposable parentDisposable, BooleanSupplier visible) {
        myProject = project;

        myRoot = VerticalLayout.create(Space.NONE, HorizontalAlignment.CENTER, VerticalAlignment.CENTER);
        myRoot.borderBuilder().topSet().apply();
        myRoot.setBackgroundColor(ComponentColors.EDITOR_AREA_BACKGROUND);

        project.getMessageBus().connect(parentDisposable).subscribe(ToolWindowManagerListener.class, new ToolWindowManagerListener() {
            @Override
            public void stateChanged(ToolWindowManager toolWindowManager) {
                if (visible.getAsBoolean()) {
                    refresh();
                }
            }
        });

        refresh();
    }

    public Component getComponent() {
        return myRoot;
    }

    @RequiredUIAccess
    public void refresh() {
        TableLayout lines = TableLayout.create(StaticPosition.CENTER);

        List<Pair<LocalizeValue, @Nullable LocalizeValue>> texts = collectLines();
        for (int i = 0; i < texts.size(); i++) {
            Pair<LocalizeValue, @Nullable LocalizeValue> line = texts.get(i);
            lines.add(createLabel(line.getFirst()), TableLayout.cell(i, 0));

            LocalizeValue shortcut = line.getSecond();
            if (shortcut != null) {
                Label shortcutLabel = createLabel(shortcut);
                shortcutLabel.paddingBuilder().leftSet(Space.LARGE).apply();
                lines.add(shortcutLabel, TableLayout.cell(i, 1));
            }
        }

        myRoot.removeAll();
        myRoot.add(lines);

        afterRefresh();
    }

    @RequiredUIAccess
    protected void afterRefresh() {
    }

    @RequiredUIAccess
    private static Label createLabel(LocalizeValue text) {
        Label label = Label.create(text);
        label.addStyle(LabelStyle.FONT_X_LARGE);
        label.setForegroundColor(ComponentColors.EDITOR_AREA_FOREGROUND);
        label.paddingBuilder().topSet(Space.MEDIUM).bottomSet(Space.SMALL).apply();
        return label;
    }

    private List<Pair<LocalizeValue, @Nullable LocalizeValue>> collectLines() {
        List<Pair<LocalizeValue, @Nullable LocalizeValue>> lines = new ArrayList<>();
        appendAction(lines, IdeLocalize.emptyTextSearchEverywhere(), getSearchEverywhereShortcut());
        if (!isToolWindowVisible(ToolWindowId.PROJECT_VIEW)) {
            String activateActionId = ActivateToolWindowAction.getActionIdForToolWindow(ToolWindowId.PROJECT_VIEW);
            appendAction(lines, IdeLocalize.emptyTextProjectView(), KeymapUtil.getFirstKeyboardShortcutTextValue(activateActionId));
        }
        appendAction(lines, IdeLocalize.emptyTextGoToFile(), KeymapUtil.getFirstKeyboardShortcutTextValue("GotoFile"));
        appendAction(lines, IdeLocalize.emptyTextRecentFiles(), KeymapUtil.getFirstKeyboardShortcutTextValue(IdeActions.ACTION_RECENT_FILES));
        appendAction(lines, IdeLocalize.emptyTextNavigationBar(), KeymapUtil.getFirstKeyboardShortcutTextValue("ShowNavBar"));
        lines.add(Pair.create(IdeLocalize.emptyTextDropFilesToOpen(), null));
        return lines;
    }

    private static void appendAction(List<Pair<LocalizeValue, @Nullable LocalizeValue>> lines, LocalizeValue action, LocalizeValue shortcut) {
        if (shortcut.isEmpty()) {
            return;
        }
        lines.add(Pair.create(action, shortcut));
    }

    private static LocalizeValue getSearchEverywhereShortcut() {
        Shortcut[] shortcuts = KeymapManager.getInstance().getActiveKeymap().getShortcuts(IdeActions.ACTION_SEARCH_EVERYWHERE);
        if (shortcuts.length == 0) {
            return IdeLocalize.doubleCtrlOrShiftShortcut(ShortcutUtil.getKeystrokeTextValue(SHIFT_DEFAULT));
        }
        return KeymapUtil.getShortcutsTextValue(shortcuts);
    }

    private boolean isToolWindowVisible(String toolWindowId) {
        if (!myProject.isInitialized()) {
            return true;
        }

        ToolWindow toolWindow = ToolWindowManager.getInstance(myProject).getToolWindow(toolWindowId);
        return toolWindow != null && toolWindow.isVisible();
    }
}
