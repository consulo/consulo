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

package consulo.language.editor.refactoring.impl.internal;

import consulo.annotation.component.ServiceImpl;
import consulo.language.editor.refactoring.RefactoringFactory;
import consulo.language.editor.refactoring.RenameRefactoring;
import consulo.language.editor.refactoring.SafeDeleteRefactoring;
import consulo.language.editor.refactoring.impl.internal.rename.RenameRefactoringImpl;
import consulo.language.editor.refactoring.impl.internal.safeDelete.SafeDeleteRefactoringImpl;
import consulo.language.psi.PsiElement;
import consulo.project.Project;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;

/**
 * @author yole
 */
@Singleton
@ServiceImpl
public class RefactoringFactoryImpl extends RefactoringFactory {
  private final Project myProject;

  @Inject
  public RefactoringFactoryImpl(Project project) {
    myProject = project;
  }

  @Override
  public RenameRefactoring createRename(PsiElement element, String newName) {
    return new RenameRefactoringImpl(myProject, element, newName, true, true);
  }

  @Override
  public RenameRefactoring createRename(PsiElement element, String newName, boolean searchInComments, boolean searchInNonJavaFiles) {
    return new RenameRefactoringImpl(myProject, element, newName, searchInComments, searchInNonJavaFiles);
  }

  @Override
  public SafeDeleteRefactoring createSafeDelete(PsiElement[] elements) {
    return new SafeDeleteRefactoringImpl(myProject, elements);
  }
}
