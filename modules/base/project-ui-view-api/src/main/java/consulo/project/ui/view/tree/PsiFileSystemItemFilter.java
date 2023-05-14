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
package consulo.project.ui.view.tree;

import consulo.language.psi.PsiDirectory;
import consulo.language.psi.PsiFile;
import consulo.language.psi.PsiFileSystemItem;
import jakarta.annotation.Nonnull;

public interface PsiFileSystemItemFilter {

  /**
   * @param item {@link PsiFile file} or {@link PsiDirectory directory}.
   * @return {@code true} if item should be showed in project view, otherwise {@code false}.
   * @see ProjectViewDirectoryHelper#getDirectoryChildren(PsiDirectory, ViewSettings, boolean, PsiFileSystemItemFilter)
   */
  boolean shouldShow(@Nonnull PsiFileSystemItem item);
}
