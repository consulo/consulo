/*
 * Copyright 2000-2016 JetBrains s.r.o.
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
package com.intellij.codeInspection;

import com.intellij.ide.DataManager;
import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiNamedElement;
import com.intellij.refactoring.RefactoringActionHandler;
import org.jetbrains.annotations.NonNls;
import javax.annotation.Nonnull;
import java.util.function.Consumer;

/**
 * @author Bas Leijdekkers
 */
public interface RefactoringQuickFix extends LocalQuickFix {

  @Override
  default boolean startInWriteAction() {
    return false;
  }

  /**
   * Usually a call to com.intellij.refactoring.RefactoringActionHandlerFactory or a language specific factory like
   * com.intellij.refactoring.JavaRefactoringActionHandlerFactory.
   */
  @Nonnull
  RefactoringActionHandler getHandler();

  default PsiElement getElementToRefactor(PsiElement element) {
    final PsiElement parent = element.getParent();
    return (parent instanceof PsiNamedElement) ? parent : element;
  }

  default void doFix(@Nonnull PsiElement element) {
    final PsiElement elementToRefactor = getElementToRefactor(element);
    if (elementToRefactor == null) {
      return;
    }
    final Consumer<DataContext> consumer = dataContext -> {
      dataContext = enhanceDataContext(dataContext);
      final RefactoringActionHandler handler = getHandler();
      handler.invoke(element.getProject(), new PsiElement[] {elementToRefactor}, dataContext);
    };
    DataManager.getInstance().getDataContextFromFocus().doWhenDone(consumer);
  }

  @Override
  default void applyFix(@Nonnull Project project, @Nonnull ProblemDescriptor descriptor) {
    doFix(descriptor.getPsiElement());
  }

  /**
   * @see com.intellij.openapi.actionSystem.impl.SimpleDataContext
   */
  @Nonnull
  default DataContext enhanceDataContext(@NonNls DataContext context) {
    return context;
  }
}
