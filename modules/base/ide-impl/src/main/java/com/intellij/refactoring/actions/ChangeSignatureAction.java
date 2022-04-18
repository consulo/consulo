/*
 * Copyright 2000-2013 JetBrains s.r.o.
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
package com.intellij.refactoring.actions;

import consulo.language.Language;
import com.intellij.lang.LanguageRefactoringSupport;
import com.intellij.lang.refactoring.RefactoringSupportProvider;
import consulo.dataContext.DataContext;
import consulo.codeEditor.Editor;
import consulo.codeEditor.ScrollType;
import consulo.project.Project;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiFile;
import consulo.language.psi.PsiNameIdentifierOwner;
import consulo.language.psi.PsiReference;
import consulo.language.editor.refactoring.RefactoringActionHandler;
import com.intellij.refactoring.changeSignature.ChangeSignatureHandler;
import consulo.language.editor.refactoring.util.CommonRefactoringUtil;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class ChangeSignatureAction extends BasePlatformRefactoringAction {

  public ChangeSignatureAction() {
    setInjectedContext(true);
  }

  @Override
  public boolean isAvailableInEditorOnly() {
    return false;
  }

  @Override
  public boolean isEnabledOnElements(@Nonnull PsiElement[] elements) {
    return elements.length == 1 && findTargetMember(elements[0]) != null;
  }

  @Override
  protected boolean isAvailableOnElementInEditorAndFile(@Nonnull final PsiElement element, @Nonnull final Editor editor, @Nonnull PsiFile file, @Nonnull DataContext context) {
    PsiElement targetMember = findTargetMember(element);
    if (targetMember == null) {
      final ChangeSignatureHandler targetHandler = getChangeSignatureHandler(file.getLanguage());
      if (targetHandler != null) {
        return true;
      }
      return false;
    }
    final ChangeSignatureHandler targetHandler = getChangeSignatureHandler(targetMember.getLanguage());
    if (targetHandler == null) return false;
    return true;
  }

  @Nullable
  private static PsiElement findTargetMember(@Nullable PsiElement element) {
    if (element == null) return null;
    final ChangeSignatureHandler fileHandler = getChangeSignatureHandler(element.getLanguage());
    if (fileHandler != null) {
      final PsiElement targetMember = fileHandler.findTargetMember(element);
      if (targetMember != null) return targetMember;
    }
    PsiReference reference = element.getReference();
    if (reference == null && element instanceof PsiNameIdentifierOwner) {
      return element;
    }
    if (reference != null) {
      return reference.resolve();
    }
    return null;
  }

  @Nullable
  @Override
  protected RefactoringActionHandler getRefactoringHandler(@Nonnull RefactoringSupportProvider provider) {
    return provider.getChangeSignatureHandler();
  }

  @Nullable
  @Override
  protected RefactoringActionHandler getRefactoringHandler(@Nonnull RefactoringSupportProvider provider, final PsiElement element) {
    return new RefactoringActionHandler() {
      @Override
      public void invoke(@Nonnull Project project, Editor editor, PsiFile file, DataContext dataContext) {
        editor.getScrollingModel().scrollToCaret(ScrollType.MAKE_VISIBLE);
        final PsiElement targetMember = findTargetMember(element);
        if (targetMember == null) {
          final ChangeSignatureHandler handler = getChangeSignatureHandler(file.getLanguage());
          if (handler != null) {
            final String notFoundMessage = handler.getTargetNotFoundMessage();
            if (notFoundMessage != null) {
              CommonRefactoringUtil.showErrorHint(project, editor, notFoundMessage, ChangeSignatureHandler.REFACTORING_NAME, null);
            }
          }
          return;
        }
        final ChangeSignatureHandler handler = getChangeSignatureHandler(targetMember.getLanguage());
        if (handler == null) return;
        handler.invoke(project, new PsiElement[]{targetMember}, dataContext);
      }

      @Override
      public void invoke(@Nonnull Project project, @Nonnull PsiElement[] elements, DataContext dataContext) {
        if (elements.length != 1) return;
        final PsiElement targetMember = findTargetMember(elements[0]);
        if (targetMember == null) return;
        final ChangeSignatureHandler handler = getChangeSignatureHandler(targetMember.getLanguage());
        if (handler == null) return;
        handler.invoke(project, new PsiElement[]{targetMember}, dataContext);
      }
    };
  }

  @Nullable
  private static ChangeSignatureHandler getChangeSignatureHandler(Language language) {
    return LanguageRefactoringSupport.INSTANCE.forLanguage(language).getChangeSignatureHandler();
  }
}
