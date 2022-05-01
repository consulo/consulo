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

package consulo.ide.impl.idea.refactoring.openapi.impl;

import consulo.project.Project;
import consulo.language.psi.PsiElement;
import consulo.ide.impl.idea.refactoring.RefactoringFactory;
import consulo.ide.impl.idea.refactoring.RenameRefactoring;
import consulo.ide.impl.idea.refactoring.SafeDeleteRefactoring;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;

/**
 * @author yole
 */
@Singleton
public class RefactoringFactoryImpl extends RefactoringFactory {
  private final Project myProject;

  @Inject
  public RefactoringFactoryImpl(final Project project) {
    myProject = project;
  }

  @Override
  public RenameRefactoring createRename(final PsiElement element, final String newName) {
    return new RenameRefactoringImpl(myProject, element, newName, true, true);
  }

  @Override
  public RenameRefactoring createRename(PsiElement element, String newName, boolean searchInComments, boolean searchInNonJavaFiles) {
    return new RenameRefactoringImpl(myProject, element, newName, searchInComments, searchInNonJavaFiles);
  }

  @Override
  public SafeDeleteRefactoring createSafeDelete(final PsiElement[] elements) {
    return new SafeDeleteRefactoringImpl(myProject, elements);
  }
}
