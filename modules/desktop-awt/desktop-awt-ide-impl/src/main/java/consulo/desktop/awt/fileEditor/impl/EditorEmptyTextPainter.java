/*
 * Copyright 2000-2016 JetBrains s.r.o.
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
package consulo.desktop.awt.fileEditor.impl;

import consulo.ide.impl.idea.ide.actions.ActivateToolWindowAction;
import consulo.ide.impl.idea.openapi.keymap.KeymapUtil;
import consulo.platform.Platform;
import consulo.project.Project;
import consulo.project.ui.wm.IdeFrame;
import consulo.project.ui.wm.IdeFrameUtil;
import consulo.project.ui.wm.ToolWindowId;
import consulo.project.ui.wm.ToolWindowManager;
import consulo.ui.ex.Gray;
import consulo.ui.ex.JBColor;
import consulo.ui.ex.action.IdeActions;
import consulo.ui.ex.action.Shortcut;
import consulo.ui.ex.action.util.MacKeymapUtil;
import consulo.ui.ex.awt.JBUI;
import consulo.ui.ex.awt.UIUtil;
import consulo.ui.ex.awt.util.UISettingsUtil;
import consulo.ui.ex.awtUnsafe.TargetAWT;
import consulo.ui.ex.keymap.KeymapManager;
import consulo.ui.ex.toolWindow.ToolWindow;
import consulo.util.lang.Couple;
import consulo.util.lang.StringUtil;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import javax.swing.*;
import java.awt.*;

public class EditorEmptyTextPainter {
  public static EditorEmptyTextPainter ourInstance = new EditorEmptyTextPainter();

  public void paintEmptyText(@Nonnull final JComponent splitters, @Nonnull Graphics g) {
    UISettingsUtil.setupAntialiasing(g);
    g.setColor(new JBColor(Gray._80, Gray._160));
    g.setFont(JBUI.Fonts.label(16f));
    UIUtil.TextPainter painter = new UIUtil.TextPainter().withLineSpacing(1.8f);
    advertiseActions(splitters, painter);
    painter.draw(g, (width, height) -> {
      Dimension s = splitters.getSize();
      int w = (s.width - width) / 2;
      int h = (int)(s.height * heightRatio());
      return Couple.of(w, h);
    });
  }

  protected double heightRatio() {
    return 0.375; // fix vertical position @ golden ratio
  }

  protected void advertiseActions(@Nonnull JComponent splitters, @Nonnull UIUtil.TextPainter painter) {
    appendSearchEverywhere(painter);
    appendToolWindow(painter, "Project View", ToolWindowId.PROJECT_VIEW, splitters);
    appendAction(painter, "Go to File", getActionShortcutText("GotoFile"));
    appendAction(painter, "Recent Files", getActionShortcutText(IdeActions.ACTION_RECENT_FILES));
    appendAction(painter, "Navigation Bar", getActionShortcutText("ShowNavBar"));
    appendDnd(painter);
  }

  protected void appendDnd(@Nonnull UIUtil.TextPainter painter) {
    appendLine(painter, "Drop files here to open");
  }

  protected void appendSearchEverywhere(@Nonnull UIUtil.TextPainter painter) {
    Shortcut[] shortcuts = KeymapManager.getInstance().getActiveKeymap().getShortcuts(IdeActions.ACTION_SEARCH_EVERYWHERE);
    appendAction(painter, "Search Everywhere",
                 shortcuts.length == 0 ? "Double " + (Platform.current().os().isMac() ? MacKeymapUtil.SHIFT : "Shift") : KeymapUtil.getShortcutsText(shortcuts));
  }

  protected void appendToolWindow(@Nonnull UIUtil.TextPainter painter, @Nonnull String action, @Nonnull String toolWindowId, @Nonnull JComponent splitters) {
    if (!isToolwindowVisible(splitters, toolWindowId)) {
      String activateActionId = ActivateToolWindowAction.getActionIdForToolWindow(toolWindowId);
      appendAction(painter, action, getActionShortcutText(activateActionId));
    }
  }

  protected void appendAction(@Nonnull UIUtil.TextPainter painter, @Nonnull String action, @Nullable String shortcut) {
    if (StringUtil.isEmpty(shortcut)) return;
    appendLine(painter, action + " " + "<shortcut>" + shortcut + "</shortcut>");
  }

  protected void appendLine(@Nonnull UIUtil.TextPainter painter, String line) {
    painter.appendLine(line);
  }

  @Nonnull
  protected String getActionShortcutText(@Nonnull String actionId) {
    return KeymapUtil.getFirstKeyboardShortcutText(actionId);
  }

  protected static boolean isToolwindowVisible(@Nonnull JComponent splitters, @Nonnull String toolwindowId) {
    Window frame = SwingUtilities.getWindowAncestor(splitters);

    IdeFrame ideFrameIfRoot = IdeFrameUtil.findRootIdeFrame(TargetAWT.from(frame));
    if (ideFrameIfRoot != null) {
      Project project = ideFrameIfRoot.getProject();
      if (project != null) {
        if (!project.isInitialized()) return true;
        ToolWindow toolWindow = ToolWindowManager.getInstance(project).getToolWindow(toolwindowId);
        return toolWindow != null && toolWindow.isVisible();
      }
    }
    return false;
  }
}
