/*
 * Copyright 2000-2011 JetBrains s.r.o.
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

import consulo.application.Application;
import consulo.application.dumb.DumbAware;
import consulo.codeEditor.Editor;
import consulo.language.Language;
import consulo.language.editor.action.CodeInsightActionHandler;
import consulo.language.editor.action.GotoSuperActionHander;
import consulo.language.editor.impl.action.BaseCodeInsightAction;
import consulo.language.psi.PsiDocumentManager;
import consulo.language.psi.PsiFile;
import consulo.language.psi.PsiUtilCore;
import consulo.project.Project;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.action.AnActionEvent;

import jakarta.annotation.Nonnull;

public class GotoSuperAction extends BaseCodeInsightAction implements CodeInsightActionHandler, DumbAware {

  public static final String FEATURE_ID = "navigation.goto.super";

  @Nonnull
  @Override
  protected CodeInsightActionHandler getHandler() {
    return this;
  }

  @RequiredUIAccess
  @Override
  public void invoke(@Nonnull final Project project, @Nonnull Editor editor, @Nonnull PsiFile file) {
    PsiDocumentManager.getInstance(project).commitAllDocuments();

    int offset = editor.getCaretModel().getOffset();
    final Language language = PsiUtilCore.getLanguageAtOffset(file, offset);

    final CodeInsightActionHandler codeInsightActionHandler = GotoSuperActionHander.forLanguage(language);
    if (codeInsightActionHandler != null) {
      codeInsightActionHandler.invoke(project, editor, file);
    }
  }

  @Override
  public boolean startInWriteAction() {
    return false;
  }

  @RequiredUIAccess
  @Override
  public void update(final AnActionEvent event) {
    if (Application.get().getExtensionPoint(GotoSuperActionHander.class).hasAnyExtensions()) {
      event.getPresentation().setVisible(true);
      super.update(event);
    }
    else {
      event.getPresentation().setVisible(false);
    }
  }
}
