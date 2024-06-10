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

package consulo.ide.impl.idea.codeInsight.navigation.actions;

import consulo.ide.impl.idea.codeInsight.navigation.IncrementalSearchHandler;
import consulo.codeEditor.Editor;
import consulo.dataContext.DataContext;
import consulo.application.dumb.DumbAware;
import consulo.project.Project;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.action.AnAction;
import consulo.ui.ex.action.AnActionEvent;
import consulo.ui.ex.action.Presentation;

import jakarta.annotation.Nonnull;

public class IncrementalSearchAction extends AnAction implements DumbAware {
  public IncrementalSearchAction() {
    setEnabledInModalContext(true);
  }

  @RequiredUIAccess
  @Override
  public void actionPerformed(@Nonnull AnActionEvent e) {
    DataContext dataContext = e.getDataContext();
    Project project = dataContext.getData(Project.KEY);
    Editor editor = dataContext.getData(Editor.KEY);
    if (editor == null) return;

    new IncrementalSearchHandler().invoke(project, editor);
  }

  @RequiredUIAccess
  @Override
  public void update(@Nonnull AnActionEvent event){
    Presentation presentation = event.getPresentation();
    DataContext dataContext = event.getDataContext();
    Project project = dataContext.getData(Project.KEY);
    if (project == null) {
      presentation.setEnabled(false);
      return;
    }

    Editor editor = dataContext.getData(Editor.KEY);
    if (editor == null){
      presentation.setEnabled(false);
      return;
    }

    presentation.setEnabled(true);
  }
}