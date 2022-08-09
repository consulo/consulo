/*
 * Copyright 2013-2022 consulo.io
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
package consulo.ide.impl.language.editor.refactoring;

import consulo.annotation.component.ServiceImpl;
import consulo.ide.impl.find.PsiElement2UsageTargetAdapter;
import consulo.ide.impl.idea.openapi.fileEditor.impl.NonProjectFileWritingAccessProvider;
import consulo.language.editor.refactoring.internal.RefactoringInternalHelper;
import consulo.language.psi.PsiElement;
import consulo.project.Project;
import consulo.usage.UsageTarget;
import consulo.virtualFileSystem.VirtualFile;
import jakarta.inject.Singleton;

import javax.annotation.Nonnull;

/**
 * @author VISTALL
 * @since 20-Apr-22
 */
@Singleton
@ServiceImpl
public class RefactoringInternalHelperImpl implements RefactoringInternalHelper {
  @Override
  public void disableWriteChecksDuring(Runnable runnable) {
    NonProjectFileWritingAccessProvider.disableChecksDuring(runnable);
  }

  @Override
  public boolean isWriteAccessAllowed(@Nonnull VirtualFile file, @Nonnull Project project) {
    return NonProjectFileWritingAccessProvider.isWriteAccessAllowed(file, project);
  }

  @Nonnull
  @Override
  public UsageTarget createPsiElement2UsageTargetAdapter(PsiElement element) {
    return new PsiElement2UsageTargetAdapter(element);
  }
}
