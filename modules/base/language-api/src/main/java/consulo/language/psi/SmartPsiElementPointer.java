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
package consulo.language.psi;

import consulo.annotation.access.RequiredReadAction;
import consulo.document.RangeMarker;
import consulo.document.util.Segment;
import consulo.project.Project;
import consulo.virtualFileSystem.VirtualFile;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

/**
 * A pointer to a PSI element which can survive PSI reparse.
 *
 * @see com.intellij.psi.SmartPointerManager#createSmartPsiElementPointer(PsiElement)
 */
public interface SmartPsiElementPointer<E extends PsiElement> {
  /**
   * Returns the PSI element corresponding to the one from which the smart pointer was created in the
   * current state of the PSI file.
   *
   * @return the PSI element, or null if the PSI reparse has completely invalidated the pointer (for example,
   * the element referenced by the pointer has been deleted).
   */
  @Nullable
  @RequiredReadAction
  E getElement();

  @Nullable
  @RequiredReadAction
  PsiFile getContainingFile();

  @Nonnull
  Project getProject();

  VirtualFile getVirtualFile();

  /**
   * @return the range in the document. For committed document, it's the same as {@link #getPsiRange()}, for non-committed documents
   * the ranges may be changed (like in {@link RangeMarker}) or even invalidated. In the latter case returns null.
   * Returns null for invalid pointers.
   */
  @Nullable
  Segment getRange();

  /**
   * @return the range in the committed PSI file. May be different from {@link #getRange()} result when the document has been changed since commit.
   * Returns null for invalid pointers.
   */
  @Nullable
  Segment getPsiRange();
}
