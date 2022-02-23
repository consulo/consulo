/*
 * Copyright 2000-2015 JetBrains s.r.o.
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
package com.intellij.vcs;

import consulo.ui.ex.action.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import consulo.ui.ex.action.DumbAwareAction;
import consulo.project.Project;
import com.intellij.openapi.vcs.changes.ui.ChangesViewContentManager;
import consulo.project.ui.wm.ToolWindow;
import consulo.project.ui.wm.ToolWindowManager;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import static com.intellij.util.ObjectUtils.assertNotNull;

public abstract class VcsShowToolWindowTabAction extends DumbAwareAction {

  @Override
  public void actionPerformed(@Nonnull AnActionEvent e) {
    final Project project = e.getRequiredData(CommonDataKeys.PROJECT);
    ToolWindow toolWindow = assertNotNull(getToolWindow(project));
    final ChangesViewContentManager changesViewContentManager = (ChangesViewContentManager)ChangesViewContentManager.getInstance(project);
    final String tabName = getTabName();

    boolean contentAlreadySelected = changesViewContentManager.isContentSelected(tabName);
    if (toolWindow.isActive() && contentAlreadySelected) {
      toolWindow.hide(null);
    }
    else {
      Runnable runnable = contentAlreadySelected ? null : new Runnable() {
        @Override
        public void run() {
          changesViewContentManager.selectContent(tabName, true);
        }
      };
      toolWindow.activate(runnable, true, true);
    }
  }

  @Nullable
  private static ToolWindow getToolWindow(@javax.annotation.Nullable Project project) {
    if (project == null) return null;
    return ToolWindowManager.getInstance(project).getToolWindow(ChangesViewContentManager.TOOLWINDOW_ID);
  }

  @Override
  public void update(@Nonnull AnActionEvent e) {
    super.update(e);
    e.getPresentation().setEnabledAndVisible(getToolWindow(e.getData(CommonDataKeys.PROJECT)) != null);
  }

  @Nonnull
  protected abstract String getTabName();
}
