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
package com.intellij.tasks.actions.vcs;

import javax.annotation.Nonnull;

import com.intellij.openapi.project.Project;
import com.intellij.tasks.LocalTask;
import com.intellij.tasks.Task;
import com.intellij.tasks.TaskManager;
import com.intellij.tasks.ui.TaskDialogPanel;
import com.intellij.tasks.ui.TaskDialogPanelProvider;

import javax.annotation.Nullable;

/**
 * @author Dmitry Avdeev
 */
public class VcsTaskDialogPanelProvider extends TaskDialogPanelProvider {
  @Nullable
  @Override
  public TaskDialogPanel getOpenTaskPanel(@Nonnull Project project, @Nonnull Task task) {
    return TaskManager.getManager(project).isVcsEnabled() ? new VcsOpenTaskPanel(project, task) : null;
  }

  @Nullable
  @Override
  public TaskDialogPanel getCloseTaskPanel(@Nonnull Project project, @Nonnull LocalTask task) {
    return TaskManager.getManager(project).isVcsEnabled() ? new VcsCloseTaskPanel(project, task) : null;
  }
}
