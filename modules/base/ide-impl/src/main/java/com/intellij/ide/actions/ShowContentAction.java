/*
 * Copyright 2000-2012 JetBrains s.r.o.
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
package com.intellij.ide.actions;

import com.intellij.openapi.ui.ShadowAction;
import consulo.application.dumb.DumbAware;
import consulo.language.editor.CommonDataKeys;
import consulo.project.Project;
import consulo.project.ui.wm.ToolWindowManager;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.action.ActionManager;
import consulo.ui.ex.action.AnAction;
import consulo.ui.ex.action.AnActionEvent;
import consulo.ui.ex.awt.UIExAWTDataKey;
import consulo.ui.ex.toolWindow.ToolWindow;
import consulo.ui.ex.toolWindow.ToolWindowContentUiType;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.swing.*;
import java.awt.*;

public class ShowContentAction extends AnAction implements DumbAware {
  private ToolWindow myWindow;

  @SuppressWarnings({"UnusedDeclaration"})
  public ShowContentAction() {
  }

  public ShowContentAction(ToolWindow window, JComponent c) {
    myWindow = window;
    AnAction original = ActionManager.getInstance().getAction("ShowContent");
    new ShadowAction(this, original, c);
    copyFrom(original);
  }

  @RequiredUIAccess
  @Override
  public void update(@Nonnull AnActionEvent e) {
    final ToolWindow window = getWindow(e);
    e.getPresentation().setEnabled(window != null && window.getContentManager().getContentCount() > 1);
    e.getPresentation().setText(window == null || window.getContentUiType() == ToolWindowContentUiType.TABBED
                                ? "Show List of Tabs"
                                : "Show List of Views");
  }

  @RequiredUIAccess
  @Override
  public void actionPerformed(@Nonnull AnActionEvent e) {
    getWindow(e).showContentPopup(e.getInputEvent());
  }

  @Nullable
  private ToolWindow getWindow(AnActionEvent event) {
    if (myWindow != null) return myWindow;

    Project project = event.getData(CommonDataKeys.PROJECT);
    if (project == null) return null;

    ToolWindowManager manager = ToolWindowManager.getInstance(project);

    final ToolWindow window = manager.getToolWindow(manager.getActiveToolWindowId());
    if (window == null) return null;

    final Component context = event.getData(UIExAWTDataKey.CONTEXT_COMPONENT);
    if (context == null) return null;

    return SwingUtilities.isDescendingFrom(window.getComponent(), context) ? window : null;
  }
}
