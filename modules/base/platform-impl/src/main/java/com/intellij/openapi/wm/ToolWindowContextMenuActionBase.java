// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.openapi.wm;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.PlatformDataKeys;
import com.intellij.ui.content.Content;
import consulo.ui.annotation.RequiredUIAccess;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public abstract class ToolWindowContextMenuActionBase extends AnAction {
  @RequiredUIAccess
  @Override
  public final void actionPerformed(@Nonnull AnActionEvent e) {
    ToolWindow toolWindow = e.getDataContext().getData(PlatformDataKeys.TOOL_WINDOW);
    if (toolWindow == null) {
      return;
    }
    Content content = getContextContent(e, toolWindow);
    actionPerformed(e, toolWindow, content);
  }

  @RequiredUIAccess
  @Override
  public final void update(@Nonnull AnActionEvent e) {
    ToolWindow toolWindow = e.getDataContext().getData(PlatformDataKeys.TOOL_WINDOW);
    if (toolWindow == null) {
      e.getPresentation().setEnabledAndVisible(false);
      return;
    }
    Content content = getContextContent(e, toolWindow);
    update(e, toolWindow, content);
  }

  public abstract void update(@Nonnull AnActionEvent e, @Nonnull ToolWindow toolWindow, @Nullable Content content);

  public abstract void actionPerformed(@Nonnull AnActionEvent e, @Nonnull ToolWindow toolWindow, @Nullable Content content);

  @Nullable
  private static Content getContextContent(@Nonnull AnActionEvent e, @Nonnull ToolWindow toolWindow) {
    Content selectedContent = e.getData(ToolWindowDataKeys.CONTENT);
    if (selectedContent == null) {
      selectedContent = toolWindow.getContentManager().getSelectedContent();
    }
    return selectedContent;
  }
}
