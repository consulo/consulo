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
package com.intellij.refactoring.actions;

import consulo.language.Language;
import com.intellij.lang.LanguageRefactoringSupport;
import com.intellij.lang.refactoring.RefactoringSupportProvider;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.actionSystem.LangDataKeys;
import com.intellij.openapi.editor.Editor;
import consulo.application.util.function.Condition;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiFile;
import com.intellij.refactoring.RefactoringActionHandler;
import com.intellij.refactoring.lang.ElementsHandler;
import com.intellij.util.containers.ContainerUtil;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;

/**
 * @author yole
 */
public abstract class BasePlatformRefactoringAction extends BaseRefactoringAction {
  private Boolean myHidden = null;
  private final Condition<RefactoringSupportProvider> myCondition = provider -> getRefactoringHandler(provider) != null;

  @Override
  protected final RefactoringActionHandler getHandler(@Nonnull DataContext dataContext) {
    PsiElement element = null;
    Editor editor = dataContext.getData(CommonDataKeys.EDITOR);
    PsiFile file = dataContext.getData(CommonDataKeys.PSI_FILE);
    if (editor != null && file != null) {
      element = getElementAtCaret(editor, file);
      if (element != null) {
        RefactoringActionHandler handler = getHandler(element.getLanguage(), element);
        if (handler != null) {
          return handler;
        }
      }
    }

    PsiElement referenced = dataContext.getData(CommonDataKeys.PSI_ELEMENT);
    if (referenced != null) {
      RefactoringActionHandler handler = getHandler(referenced.getLanguage(), referenced);
      if (handler != null) {
        return handler;
      }
    }

    PsiElement[] psiElements = dataContext.getData(LangDataKeys.PSI_ELEMENT_ARRAY);
    if (psiElements != null && psiElements.length > 1) {
      RefactoringActionHandler handler = getHandler(psiElements[0].getLanguage(), psiElements[0]);
      if (handler != null && isEnabledOnElements(psiElements)) {
        return handler;
      }
    }

    if (element == null) {
      element = referenced;
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
    List<RefactoringSupportProvider> providers = LanguageRefactoringSupport.INSTANCE.allForLanguage(language);
    if (providers.isEmpty()) return null;
    if (element == null) return getRefactoringHandler(providers.get(0));
    for (RefactoringSupportProvider provider : providers) {
      if (provider.isAvailable(element)) {
        return getRefactoringHandler(provider, element);
      }
    }
    return null;
  }

  @Override
  protected boolean isAvailableOnElementInEditorAndFile(@Nonnull PsiElement element, @Nonnull Editor editor, @Nonnull PsiFile file, @Nonnull DataContext context) {
    return getHandler(context) != null;
  }

  @Override
  protected boolean isAvailableForLanguage(final Language language) {
    List<RefactoringSupportProvider> providers = LanguageRefactoringSupport.INSTANCE.allForLanguage(language);
    return ContainerUtil.find(providers, myCondition) != null;
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
    for(Language l: Language.getRegisteredLanguages()) {
      if (isAvailableForLanguage(l)) {
        return false;
      }
    }
    return true;
  }
}
