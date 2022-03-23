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
package com.intellij.openapi.vcs.actions;

import consulo.application.dumb.DumbAware;
import consulo.language.editor.CommonDataKeys;
import consulo.project.Project;
import consulo.ui.ex.action.DefaultActionGroup;
import consulo.ui.ex.action.AnActionEvent;
import consulo.ui.ex.action.Presentation;

/**
 * @author mike
 */
public class VcsActionGroup extends DefaultActionGroup implements DumbAware {
  public void update(AnActionEvent event) {
    super.update(event);

    Presentation presentation = event.getPresentation();
    Project project = event.getDataContext().getData(CommonDataKeys.PROJECT);
    if (project == null){
      presentation.setVisible(false);
      presentation.setEnabled(false);
    } else if (!project.isOpen()) {
      presentation.setVisible(false);
      presentation.setEnabled(false);
    } else {
      presentation.setVisible(true);
      presentation.setEnabled(true);
    }
  }
}
