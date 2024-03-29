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
package consulo.language.editor.refactoring.internal;

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ServiceAPI;
import consulo.application.Application;
import consulo.language.psi.PsiDirectory;
import consulo.language.psi.PsiElement;
import consulo.project.Project;
import consulo.usage.UsageTarget;
import consulo.util.dataholder.Key;
import consulo.virtualFileSystem.VirtualFile;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import java.util.Map;

/**
 * @author VISTALL
 * @since 20-Apr-22
 */
@ServiceAPI(ComponentScope.APPLICATION)
public interface RefactoringInternalHelper {
  public static final Key<Boolean> COPY_PASTE_DELEGATE_SHOW_CHOOSER_KEY = Key.create("show.dirs.chooser");

  static RefactoringInternalHelper getInstance() {
    return Application.get().getInstance(RefactoringInternalHelper.class);
  }

  default void disableWriteChecksDuring(Runnable runnable) {
    runnable.run();
  }

  default boolean isWriteAccessAllowed(@Nonnull VirtualFile file, @Nonnull Project project) {
    return true;
  }

  @Nonnull
  UsageTarget createPsiElement2UsageTargetAdapter(PsiElement element);

  @Nullable
  PsiDirectory chooseDirectory(PsiDirectory[] targetDirectories, @Nullable PsiDirectory initialDirectory, @Nonnull Project project, Map<PsiDirectory, String> relativePathsToCreate);
}
