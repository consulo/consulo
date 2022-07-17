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

package consulo.ide.impl.idea.codeInsight.generation.actions;

import consulo.annotation.component.ActionImpl;
import consulo.codeEditor.Editor;
import consulo.ide.impl.idea.codeInsight.actions.BaseCodeInsightAction;
import consulo.ide.impl.idea.codeInsight.generation.ImplementMethodsHandler;
import consulo.ide.impl.idea.lang.CodeInsightActions;
import consulo.language.Language;
import consulo.language.editor.action.CodeInsightActionHandler;
import consulo.language.editor.action.LanguageCodeInsightActionHandler;
import consulo.language.psi.PsiFile;
import consulo.language.psi.PsiUtilCore;
import consulo.project.Project;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.action.AnActionEvent;

import javax.annotation.Nonnull;

@ActionImpl(id = "ImplementMethods")
public class ImplementMethodsAction extends BaseCodeInsightAction {
  @Nonnull
  @Override
  protected CodeInsightActionHandler getHandler() {
    return new ImplementMethodsHandler();
  }

  @Override
  protected boolean isValidForFile(@Nonnull Project project, @Nonnull Editor editor, @Nonnull final PsiFile file) {
    final Language language = PsiUtilCore.getLanguageAtOffset(file, editor.getCaretModel().getOffset());
    final LanguageCodeInsightActionHandler codeInsightActionHandler = CodeInsightActions.IMPLEMENT_METHOD.forLanguage(language);
    return codeInsightActionHandler != null && codeInsightActionHandler.isValidFor(editor, file);
  }

  @RequiredUIAccess
  @Override
  public void update(final AnActionEvent event) {
    if (CodeInsightActions.IMPLEMENT_METHOD.hasAnyExtensions()) {
      event.getPresentation().setVisible(true);
      super.update(event);
    }
    else {
      event.getPresentation().setVisible(false);
    }
  }
}