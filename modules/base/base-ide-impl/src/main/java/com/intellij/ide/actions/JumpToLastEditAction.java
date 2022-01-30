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

import com.intellij.openapi.actionSystem.*;
import com.intellij.openapi.fileEditor.ex.IdeDocumentHistory;
import consulo.project.Project;
import consulo.application.dumb.DumbAware;
import consulo.ui.ex.action.AnAction;
import consulo.ui.ex.action.AnActionEvent;
import consulo.ui.ex.action.Presentation;

public class JumpToLastEditAction extends AnAction implements DumbAware {
  public void actionPerformed(AnActionEvent e) {
    Project project = e.getData(CommonDataKeys.PROJECT);
    if (project == null) return;
    IdeDocumentHistory.getInstance(project).navigatePreviousChange();
  }

  public void update(AnActionEvent e){
    Presentation presentation = e.getPresentation();
    Project project = e.getData(CommonDataKeys.PROJECT);
    if (project == null) {
      presentation.setEnabled(false);
      return;
    }
    presentation.setEnabled(IdeDocumentHistory.getInstance(project).isNavigatePreviousChangeAvailable());
  }
}