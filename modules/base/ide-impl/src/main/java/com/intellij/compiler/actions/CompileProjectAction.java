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
package com.intellij.compiler.actions;

import com.intellij.history.LocalHistory;
import consulo.compiler.CompileContext;
import consulo.compiler.CompileStatusNotification;
import consulo.compiler.CompilerBundle;
import consulo.compiler.CompilerManager;
import consulo.dataContext.DataContext;
import consulo.language.editor.CommonDataKeys;
import consulo.project.Project;
import javax.annotation.Nonnull;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.action.AnActionEvent;
import consulo.ui.ex.action.Presentation;

public class CompileProjectAction extends CompileActionBase {
  @RequiredUIAccess
  @Override
  protected void doAction(DataContext dataContext, final Project project) {
    CompilerManager.getInstance(project).rebuild(new CompileStatusNotification() {
      @Override
      public void finished(boolean aborted, int errors, int warnings, final CompileContext compileContext) {
        if (aborted) return;

        String text = getTemplatePresentation().getText();
        LocalHistory.getInstance().putSystemLabel(project, errors == 0
                                       ? CompilerBundle.message("rebuild.lvcs.label.no.errors", text)
                                       : CompilerBundle.message("rebuild.lvcs.label.with.errors", text));
      }
    });
  }

  @Override
  @RequiredUIAccess
  public void update(@Nonnull AnActionEvent event) {
    super.update(event);
    Presentation presentation = event.getPresentation();
    if (!presentation.isEnabled()) {
      return;
    }
    Project project = event.getData(CommonDataKeys.PROJECT);
    presentation.setEnabled(project != null);
  }
}