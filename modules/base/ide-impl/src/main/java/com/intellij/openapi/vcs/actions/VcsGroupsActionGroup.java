/*
 * Copyright 2013-2021 consulo.io
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
package com.intellij.openapi.vcs.actions;

import consulo.ui.ex.action.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import consulo.ui.ex.action.DefaultActionGroup;
import consulo.ui.ex.action.Presentation;
import consulo.application.dumb.DumbAware;
import consulo.project.Project;
import com.intellij.openapi.vcs.ProjectLevelVcsManager;
import consulo.ui.annotation.RequiredUIAccess;

import javax.annotation.Nonnull;

// from kotlin
public class VcsGroupsActionGroup extends DefaultActionGroup implements DumbAware {
  @RequiredUIAccess
  @Override
  public void update(@Nonnull AnActionEvent e) {
    Presentation presentation = e.getPresentation();
    Project project = e.getData(CommonDataKeys.PROJECT);
    if(project != null) {
      presentation.setTextValue(ProjectLevelVcsManager.getInstance(project).getConsolidatedVcsName());
    }
  }
}
