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

package consulo.ide.impl.idea.refactoring.actions;

import consulo.ide.impl.idea.ide.TitledHandler;
import consulo.language.Language;
import consulo.language.editor.refactoring.LanguageExtractIncludeHandler;
import consulo.language.editor.refactoring.action.BasePlatformRefactoringAction;
import consulo.language.editor.refactoring.action.RefactoringActionHandler;
import consulo.language.editor.refactoring.RefactoringSupportProvider;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiFile;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.action.AnActionEvent;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

/**
 * @author ven
 */
public class ExtractIncludeAction extends BasePlatformRefactoringAction {
  @Override
  public boolean isAvailableInEditorOnly() {
    return true;
  }

  @Override
  public boolean isEnabledOnElements(@Nonnull PsiElement[] elements) {
    return false;
  }

  @Override
  protected boolean isAvailableForLanguage(Language language) {
    return true;
  }

  @RequiredUIAccess
  @Override
  public void update(@Nonnull final AnActionEvent e) {
    super.update(e);
    final RefactoringActionHandler handler = getHandler(e.getDataContext());
    if (handler instanceof TitledHandler) {
      e.getPresentation().setText(((TitledHandler) handler).getActionTitle());
    }
    else {
      e.getPresentation().setText("Include File...");
    }
  }

  @Override
  protected boolean isAvailableForFile(PsiFile file) {
    final Language baseLanguage = file.getViewProvider().getBaseLanguage();
    return LanguageExtractIncludeHandler.forLanguage(baseLanguage) != null;
  }

  @Nullable
  @Override
  protected RefactoringActionHandler getRefactoringHandler(@Nonnull RefactoringSupportProvider provider) {
    return null;
  }

  @Override
  @Nullable
  protected RefactoringActionHandler getRefactoringHandler(@Nonnull RefactoringSupportProvider provider, PsiElement element) {
    PsiFile file = element.getContainingFile();
    if (file == null) return null;
    return LanguageExtractIncludeHandler.forLanguage(file.getViewProvider().getBaseLanguage());
  }
}
