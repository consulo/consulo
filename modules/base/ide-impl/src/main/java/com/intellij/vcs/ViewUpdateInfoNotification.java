/*
 * Copyright 2000-2017 JetBrains s.r.o.
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

import com.intellij.notification.Notification;
import com.intellij.notification.NotificationAction;
import consulo.disposer.Disposable;
import consulo.ui.ex.action.AnActionEvent;
import consulo.project.Project;
import com.intellij.openapi.vcs.ex.ProjectLevelVcsManagerEx;
import com.intellij.openapi.vcs.update.UpdateInfoTree;
import com.intellij.openapi.wm.ToolWindowId;
import consulo.project.ui.wm.ToolWindowManager;
import consulo.ui.ex.content.ContentManager;
import com.intellij.util.ContentUtilEx;
import consulo.disposer.Disposer;

import javax.annotation.Nonnull;

public class ViewUpdateInfoNotification extends NotificationAction {
  @Nonnull
  private final Project myProject;
  @Nonnull
  private final UpdateInfoTree myTree;

  public ViewUpdateInfoNotification(@Nonnull Project project, @Nonnull UpdateInfoTree updateInfoTree, @Nonnull String actionName,
                                    @Nonnull Notification notification) {
    super(actionName);
    myProject = project;
    myTree = updateInfoTree;
    Disposer.register(updateInfoTree, new Disposable() {
      @Override
      public void dispose() {
        notification.expire();
      }
    });
  }

  @Override
  public void actionPerformed(@Nonnull AnActionEvent e, @Nonnull Notification notification) {
    focusUpdateInfoTree(myProject, myTree);
  }

  public static void focusUpdateInfoTree(@Nonnull Project project, @Nonnull UpdateInfoTree updateInfoTree) {
    ToolWindowManager.getInstance(project).getToolWindow(ToolWindowId.VCS).activate(() -> {
      ContentManager contentManager = ProjectLevelVcsManagerEx.getInstanceEx(project).getContentManager();
      if (contentManager != null) ContentUtilEx.selectContent(contentManager, updateInfoTree, true);
    }, true, true);
  }
}
