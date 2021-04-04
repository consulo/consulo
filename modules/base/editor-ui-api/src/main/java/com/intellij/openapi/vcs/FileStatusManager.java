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
package com.intellij.openapi.vcs;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import consulo.disposer.Disposable;
import consulo.ui.color.ColorValue;

import javax.annotation.Nonnull;

/**
 * @author mike
 */
public abstract class FileStatusManager {
  @Nonnull
  public static FileStatusManager getInstance(Project project) {
    return project.getComponent(FileStatusManager.class);
  }

  public abstract FileStatus getStatus(VirtualFile virtualFile);

  public abstract void fileStatusesChanged();

  public abstract void fileStatusChanged(VirtualFile file);

  public abstract void addFileStatusListener(FileStatusListener listener);

  public abstract void addFileStatusListener(FileStatusListener listener, Disposable parentDisposable);

  public abstract void removeFileStatusListener(FileStatusListener listener);

  /**
   * @deprecated Use getStatus(file).getText()} instead
   */
  public String getStatusText(VirtualFile file) {
    return getStatus(file).getText().get();
  }

  /**
   * @deprecated Use getStatus(file).getColor()} instead
   */
  public ColorValue getStatusColor(VirtualFile file) {
    return getStatus(file).getColor();
  }

  public abstract ColorValue getNotChangedDirectoryColor(VirtualFile vf);

  /**
   * @See VcsConfiguration#SHOW_DIRTY_RECURSIVELY
   * @See com.intellij.openapi.vcs.FileStatus#NOT_CHANGED_IMMEDIATE
   * @See com.intellij.openapi.vcs.FileStatus#NOT_CHANGED_RECURSIVE
   */
  @Nonnull
  public FileStatus getRecursiveStatus(@Nonnull VirtualFile file) {
    FileStatus status = getStatus(file);
    return status != null ? status : FileStatus.NOT_CHANGED;
  }
}
