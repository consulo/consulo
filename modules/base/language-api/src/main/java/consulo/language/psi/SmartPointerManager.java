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
package consulo.language.psi;

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ServiceAPI;
import consulo.document.util.TextRange;
import consulo.project.Project;

import jakarta.annotation.Nonnull;

/**
 * Allows to create references to PSI elements that can survive a reparse and return the corresponding
 * element in the PSI tree after the reparse.
 */
@ServiceAPI(ComponentScope.PROJECT)
public abstract class SmartPointerManager {
  @Nonnull
  public abstract SmartPsiFileRange createSmartPsiFileRangePointer(@Nonnull PsiFile file, @Nonnull TextRange range);

  public static SmartPointerManager getInstance(Project project) {
    return project.getInstance(SmartPointerManager.class);
  }

  /**
   * Creates a smart pointer to the specified PSI element
   * using a manager that corresponds to the element's project.
   *
   * @param element the element to create a pointer to
   * @param <E>     the specific type of the given element
   * @return a pointer to the specified element which can survive PSI reparse
   * @see #createSmartPsiElementPointer(PsiElement)
   */
  @Nonnull
  public static <E extends PsiElement> SmartPsiElementPointer<E> createPointer(@Nonnull E element) {
    return getInstance(element.getProject()).createSmartPsiElementPointer(element);
  }

  /**
   * Creates a smart pointer to the specified PSI element. If the element's containing file is known, it's more preferable to use
   * {@link #createSmartPsiElementPointer(PsiElement, PsiFile)}.
   *
   * @param element the element to create a pointer to.
   * @return the smart pointer instance.
   */
  @Nonnull
  public abstract <E extends PsiElement> SmartPsiElementPointer<E> createSmartPsiElementPointer(@Nonnull E element);

  /**
   * Creates a smart pointer to the specified PSI element.
   *
   * @param element the element to create a pointer to.
   * @param containingFile the result of <code>element.getContainingFile()</code>.
   * @return the smart pointer instance.
   */
  @Nonnull
  public abstract <E extends PsiElement> SmartPsiElementPointer<E> createSmartPsiElementPointer(@Nonnull E element, PsiFile containingFile);

  /**
   * Creates a smart pointer to the specified PSI element which doesn't hold a strong reference to the PSI
   * element.
   * @deprecated use {@link #createSmartPsiElementPointer(PsiElement)} instead
   * @param element the element to create a pointer to.
   * @return the smart pointer instance.
   */
  @Nonnull
  public <E extends PsiElement> SmartPsiElementPointer<E> createLazyPointer(@Nonnull E element) {
    return createSmartPsiElementPointer(element);
  }

  /**
   * This method is cheaper than dereferencing both pointers and comparing the result.
   *
   * @param pointer1 smart pointer to compare
   * @param pointer2 smart pointer to compare
   * @return true if both pointers point to the same PSI element.
   */
  public abstract boolean pointToTheSameElement(@Nonnull SmartPsiElementPointer pointer1, @Nonnull SmartPsiElementPointer pointer2);

  /**
   * Disposes a smart pointer and frees the resources associated with it. Calling this method is not obligatory: pointers are
   * freed correctly when they're not used anymore. But disposing the pointers explicitly might be beneficial for performance.
   */
  public abstract void removePointer(@Nonnull SmartPsiElementPointer pointer);
}
