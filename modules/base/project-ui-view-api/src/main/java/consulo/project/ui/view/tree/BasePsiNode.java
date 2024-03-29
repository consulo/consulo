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

package consulo.project.ui.view.tree;

import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiUtilCore;
import consulo.project.Project;
import consulo.virtualFileSystem.VirtualFile;
import consulo.virtualFileSystem.status.FileStatus;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import java.util.Objects;

public abstract class BasePsiNode<T extends PsiElement> extends AbstractPsiBasedNode<T> {
  @Nullable
  private final VirtualFile myVirtualFile;

  protected BasePsiNode(Project project, @Nonnull T value, ViewSettings viewSettings) {
    super(project, value, viewSettings);
    myVirtualFile = PsiUtilCore.getVirtualFile(value);
  }

  @Nonnull
  @Override
  public FileStatus getFileStatus() {
    return computeFileStatus(getVirtualFile(), Objects.requireNonNull(getProject()));
  }

  @Override
  @Nullable
  public VirtualFile getVirtualFile() {
    return myVirtualFile;
  }

  @Override
  @Nullable
  protected PsiElement extractPsiFromValue() {
    return getValue();
  }
}
