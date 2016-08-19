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
package com.intellij.compiler.impl;

import com.intellij.compiler.CompilerWorkspaceConfiguration;
import com.intellij.compiler.HelpID;
import com.intellij.compiler.ProblemsView;
import com.intellij.ide.actions.CloseTabToolbarAction;
import com.intellij.ide.errorTreeView.NewErrorTreeViewPanel;
import com.intellij.openapi.actionSystem.*;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;
import consulo.annotations.RequiredDispatchThread;

public class ProblemsViewPanel extends NewErrorTreeViewPanel {
  public ProblemsViewPanel(Project project) {
    super(project, HelpID.COMPILER, false, true, null);
    myTree.getEmptyText().setText("No compilation problems found");
  }

  @Override
  protected void fillRightToolbarGroup(DefaultActionGroup group) {
    super.fillRightToolbarGroup(group);
    group.add(new CompilerPropertiesAction());
    group.add(new CloseTabToolbarAction() {
      @RequiredDispatchThread
      @Override
      public void actionPerformed(@NotNull AnActionEvent e) {
        ProblemsView.getInstance(myProject).showOrHide(true);
      }
    });
  }

  @Override
  protected void addExtraPopupMenuActions(DefaultActionGroup group) {
    group.add(new ExcludeFromCompileAction(myProject, this));

    ActionGroup popupGroup = (ActionGroup)ActionManager.getInstance().getAction(IdeActions.GROUP_COMPILER_ERROR_VIEW_POPUP);
    if (popupGroup != null) {
      for (AnAction action : popupGroup.getChildren(null)) {
        group.add(action);
      }
    }
  }

  @Override
  protected boolean shouldShowFirstErrorInEditor() {
    return CompilerWorkspaceConfiguration.getInstance(myProject).AUTO_SHOW_ERRORS_IN_EDITOR;
  }

  @Override
  protected boolean canHideWarningsOrInfos() {
    return true;
  }
}
