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
package consulo.language.editor.refactoring.event;

import consulo.language.psi.PsiElement;
import jakarta.annotation.Nonnull;

/**
 * {@linkplain RefactoringElementListenerProvider} receives a notification of what happened
 * to element it have been observing during a refactoring.
 * @author dsl
 */
public interface RefactoringElementListener {
  RefactoringElementListener DEAF = new RefactoringElementListener() {
    @Override
    public void elementMoved(@Nonnull PsiElement newElement) {}

    @Override
    public void elementRenamed(@Nonnull PsiElement newElement) {}
  };
  
  /**
   * Invoked in write action and command.
   */
  void elementMoved(@Nonnull PsiElement newElement);
  /**
   * Invoked in write action and command.
   */
  void elementRenamed(@Nonnull PsiElement newElement);
}
