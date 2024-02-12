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
package consulo.language.editor.refactoring.action;

import consulo.annotation.access.RequiredReadAction;
import consulo.codeEditor.Editor;
import consulo.dataContext.DataContext;
import consulo.language.Language;
import consulo.language.editor.LangDataKeys;
import consulo.language.editor.refactoring.ElementsHandler;
import consulo.language.editor.refactoring.RefactoringSupportProvider;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiFile;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

/**
 * @author yole
 */
public abstract class BasePlatformRefactoringAction extends BaseRefactoringAction {
  private Boolean myHidden = null;

  @Override
  @RequiredReadAction
  protected final RefactoringActionHandler getHandler(@Nonnull DataContext dataContext) {
    PsiElement element = null;
    Editor editor = dataContext.getData(Editor.KEY);
    PsiFile file = dataContext.getData(PsiFile.KEY);
    if (editor != null && file != null) {
      element = getElementAtCaret(editor, file);
      if (element != null) {
        RefactoringActionHandler handler = getHandler(element.getLanguage(), element);
        if (handler != null) {
          return handler;
        }
      }
    }

    PsiElement referenced = dataContext.getData(PsiElement.KEY);
    if (referenced != null) {
      RefactoringActionHandler handler = getHandler(referenced.getLanguage(), referenced);
      if (handler != null) {
        return handler;
      }
    }

    PsiElement[] psiElements = dataContext.getData(PsiElement.KEY_OF_ARRAY);
    if (psiElements != null && psiElements.length > 1) {
      RefactoringActionHandler handler = getHandler(psiElements[0].getLanguage(), psiElements[0]);
      if (handler != null && isEnabledOnElements(psiElements)) {
        return handler;
      }
    }

    if (element == null) {
      element = referenced;
    }

    if (element == null) {
      return null;
    }

    final Language[] languages = dataContext.getData(LangDataKeys.CONTEXT_LANGUAGES);
    if (languages != null) {
      for (Language language : languages) {
        RefactoringActionHandler handler = getHandler(language, element);
        if (handler != null) {
          return handler;
        }
      }
    }

    return null;
  }

  @Nullable
  protected RefactoringActionHandler getHandler(@Nonnull Language language, PsiElement element) {
    RefactoringSupportProvider provider = RefactoringSupportProvider.forLanguage(language);
    if (provider.isAvailable(element)) {
      return getRefactoringHandler(provider, element);
    }
    return null;
  }

  @Override
  protected boolean isAvailableOnElementInEditorAndFile(@Nonnull PsiElement element, @Nonnull Editor editor, @Nonnull PsiFile file, @Nonnull DataContext context) {
    return getHandler(context) != null;
  }

  @Override
  protected boolean isAvailableForLanguage(final Language language) {
    RefactoringSupportProvider refactoringSupportProvider = RefactoringSupportProvider.forLanguage(language);
    // any language mean default provider
    return refactoringSupportProvider.getLanguage() != Language.ANY;
  }

  @Override
  protected boolean isEnabledOnElements(@Nonnull PsiElement[] elements) {
    if (elements.length > 0) {
      Language language = elements[0].getLanguage();
      RefactoringActionHandler handler = getHandler(language, elements[0]);
      return handler instanceof ElementsHandler && ((ElementsHandler)handler).isEnabledOnElements(elements);
    }
    return false;
  }

  @Nullable
  protected abstract RefactoringActionHandler getRefactoringHandler(@Nonnull RefactoringSupportProvider provider);

  @Nullable
  protected RefactoringActionHandler getRefactoringHandler(@Nonnull RefactoringSupportProvider provider, PsiElement element) {
    return getRefactoringHandler(provider);
  }

  @Override
  protected boolean isHidden() {
    if (myHidden == null) {
      myHidden = calcHidden();
    }
    return myHidden.booleanValue();
  }

  private boolean calcHidden() {
    for (Language l : Language.getRegisteredLanguages()) {
      if (isAvailableForLanguage(l)) {
        return false;
      }
    }
    return true;
  }
}
