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
package com.intellij.psi.impl;

import consulo.disposer.Disposable;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.VirtualFileFilter;
import com.intellij.psi.PsiManager;
import com.intellij.psi.impl.file.impl.FileManager;
import javax.annotation.Nonnull;
import org.jetbrains.annotations.TestOnly;

/**
 * @author peter
 */
public abstract class PsiManagerEx extends PsiManager {
  public static PsiManagerEx getInstanceEx(Project project) {
    return (PsiManagerEx)getInstance(project);
  }
  public abstract boolean isBatchFilesProcessingMode();

  @TestOnly
  public abstract void setAssertOnFileLoadingFilter(@Nonnull VirtualFileFilter filter, @Nonnull Disposable parentDisposable);

  @TestOnly
  public abstract boolean isAssertOnFileLoading(@Nonnull VirtualFile file);

  /**
   * @param runnable to be run before <b>physical</b> PSI change
   */
  public abstract void registerRunnableToRunOnChange(@Nonnull Runnable runnable);

  /**
   * @param runnable to be run before <b>physical</b> or <b>non-physical</b> PSI change
   */
  public abstract void registerRunnableToRunOnAnyChange(@Nonnull Runnable runnable);

  public abstract void registerRunnableToRunAfterAnyChange(@Nonnull Runnable runnable);

  @Nonnull
  public abstract FileManager getFileManager();

  public abstract void beforeChildAddition(@Nonnull PsiTreeChangeEventImpl event);

  public abstract void beforeChildRemoval(@Nonnull PsiTreeChangeEventImpl event);

  public abstract void beforeChildReplacement(@Nonnull PsiTreeChangeEventImpl event);

  public abstract void beforeChange(boolean isPhysical);

  public abstract void afterChange(boolean isPhysical);
}
