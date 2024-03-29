/*
 * Copyright 2000-2010 JetBrains s.r.o.
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
package consulo.language.editor.refactoring.changeSignature;

import consulo.dataContext.DataContext;
import consulo.codeEditor.Editor;
import consulo.project.Project;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiFile;
import consulo.language.editor.refactoring.action.RefactoringActionHandler;
import consulo.language.editor.refactoring.RefactoringBundle;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

/**
 * @author Maxim.Medvedev
 */
public interface ChangeSignatureHandler extends RefactoringActionHandler {
  String REFACTORING_NAME = RefactoringBundle.message("changeSignature.refactoring.name");

  @Nullable
  PsiElement findTargetMember(PsiFile file, Editor editor);

  @Nullable
  PsiElement findTargetMember(PsiElement element);

  @Override
  void invoke(@Nonnull Project project, @Nonnull PsiElement[] elements, @Nullable DataContext dataContext);

  @Nullable
  String getTargetNotFoundMessage();
}
