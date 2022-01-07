/*
 * Copyright 2000-2012 JetBrains s.r.o.
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
package com.intellij.psi.search;

import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiBundle;
import consulo.ui.image.Image;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public abstract class SearchScope {
  private static int hashCodeCounter = 0;

  @SuppressWarnings({"AssignmentToStaticFieldFromInstanceMethod"})
  private final int myHashCode = hashCodeCounter++;

  /**
   * Overridden for performance reason. Object.hashCode() is native method and becomes a bottleneck when called often.
   *
   * @return hashCode value semantically identical to one from Object but not native
   */
  public int hashCode() {
    return myHashCode;
  }

  @Nonnull
  public String getDisplayName() {
    return PsiBundle.message("search.scope.unknown");
  }

  @Nullable
  public Image getIcon() {
    return null;
  }

  @Nonnull
  public abstract SearchScope intersectWith(@Nonnull SearchScope scope2);
  @Nonnull
  public abstract SearchScope union(@Nonnull SearchScope scope);

  public abstract boolean contains(@Nonnull VirtualFile file);
}
