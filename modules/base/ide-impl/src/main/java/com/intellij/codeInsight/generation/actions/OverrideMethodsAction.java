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

package com.intellij.codeInsight.generation.actions;

import consulo.language.editor.action.CodeInsightActionHandler;
import com.intellij.codeInsight.actions.BaseCodeInsightAction;
import com.intellij.codeInsight.generation.OverrideMethodsHandler;
import com.intellij.lang.CodeInsightActions;
import consulo.language.Language;
import consulo.language.editor.action.LanguageCodeInsightActionHandler;
import consulo.codeEditor.Editor;
import consulo.project.Project;
import consulo.ui.ex.action.AnActionEvent;
import consulo.language.psi.PsiFile;
import consulo.language.psi.PsiUtilCore;
import javax.annotation.Nonnull;
import consulo.ui.annotation.RequiredUIAccess;

public class OverrideMethodsAction extends BaseCodeInsightAction {

  @Nonnull
  @Override
  protected CodeInsightActionHandler getHandler() {
    return new OverrideMethodsHandler();
  }

  @Override
  protected boolean isValidForFile(@Nonnull Project project, @Nonnull Editor editor, @Nonnull final PsiFile file) {
    Language language = PsiUtilCore.getLanguageAtOffset(file, editor.getCaretModel().getOffset());
    final LanguageCodeInsightActionHandler codeInsightActionHandler = CodeInsightActions.OVERRIDE_METHOD.forLanguage(language);
    if (codeInsightActionHandler != null) {
      return codeInsightActionHandler.isValidFor(editor, file);
    }
    return false;
  }

  @RequiredUIAccess
  @Override
  public void update(final AnActionEvent event) {
    if (CodeInsightActions.OVERRIDE_METHOD.hasAnyExtensions()) {
      event.getPresentation().setVisible(true);
      super.update(event);
    }
    else {
      event.getPresentation().setVisible(false);
    }
  }
}