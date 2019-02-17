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
package com.intellij.openapi.fileEditor.impl;

import com.intellij.ide.actions.ActivateToolWindowAction;
import com.intellij.ide.ui.UISettings;
import com.intellij.openapi.actionSystem.IdeActions;
import com.intellij.openapi.actionSystem.Shortcut;
import com.intellij.openapi.keymap.KeymapManager;
import com.intellij.openapi.keymap.KeymapUtil;
import com.intellij.openapi.keymap.MacKeymapUtil;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Couple;
import com.intellij.openapi.util.SystemInfo;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.wm.IdeFrame;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowId;
import com.intellij.openapi.wm.ToolWindowManager;
import com.intellij.ui.Gray;
import com.intellij.ui.JBColor;
import com.intellij.util.ui.JBUI;
import com.intellij.util.ui.UIUtil;
import consulo.awt.TargetAWT;
import consulo.wm.util.IdeFrameUtil;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.swing.*;
import java.awt.*;

public class EditorEmptyTextPainter {
  public static EditorEmptyTextPainter ourInstance = new EditorEmptyTextPainter();

  public void paintEmptyText(@Nonnull final JComponent splitters, @Nonnull Graphics g) {
    UISettings.setupAntialiasing(g);
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
                 shortcuts.length == 0 ? "Double " + (SystemInfo.isMac ? MacKeymapUtil.SHIFT : "Shift") : KeymapUtil.getShortcutsText(shortcuts));
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
