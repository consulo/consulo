/*
 * Copyright 2000-2009 JetBrains s.r.o.
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

import consulo.ui.ex.action.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import consulo.ui.ex.action.Presentation;
import com.intellij.openapi.actionSystem.ToggleAction;
import consulo.application.dumb.DumbAware;
import consulo.project.Project;
import consulo.project.ui.wm.ToolWindow;
import consulo.project.ui.wm.ToolWindowManager;
import consulo.project.ui.wm.ToolWindowType;

public class ToggleDockModeAction extends ToggleAction implements DumbAware {

  public boolean isSelected(AnActionEvent event) {
    Project project = event.getData(CommonDataKeys.PROJECT);
    if (project == null) {
      return false;
    }
    ToolWindowManager windowManager = ToolWindowManager.getInstance(project);
    String id = windowManager.getActiveToolWindowId();
    if (id == null) {
      return false;
    }
    return ToolWindowType.DOCKED == windowManager.getToolWindow(id).getType();
  }

  public void setSelected(AnActionEvent event, boolean flag) {
    Project project = event.getData(CommonDataKeys.PROJECT);
    if (project == null) {
      return;
    }
    ToolWindowManager windowManager = ToolWindowManager.getInstance(project);
    String id = windowManager.getActiveToolWindowId();
    if (id == null) {
      return;
    }
    ToolWindow toolWindow = windowManager.getToolWindow(id);
    ToolWindowType type = toolWindow.getType();
    if (ToolWindowType.DOCKED == type) {
      toolWindow.setType(ToolWindowType.SLIDING, null);
    }
    else if (ToolWindowType.SLIDING == type) {
      toolWindow.setType(ToolWindowType.DOCKED, null);
    }
  }

  public void update(AnActionEvent event) {
    super.update(event);
    Presentation presentation = event.getPresentation();
    Project project = event.getData(CommonDataKeys.PROJECT);
    if (project == null) {
      presentation.setEnabled(false);
      return;
    }
    ToolWindowManager mgr = ToolWindowManager.getInstance(project);
    String id = mgr.getActiveToolWindowId();
    if (id == null) {
      presentation.setEnabled(false);
      return;
    }
    ToolWindow toolWindow = mgr.getToolWindow(id);
    presentation.setEnabled(toolWindow.isAvailable() && ToolWindowType.FLOATING != toolWindow.getType());
  }
}
