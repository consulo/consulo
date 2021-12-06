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

package com.intellij.openapi.roots.impl;

import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.OrderEntry;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.util.Query;
import consulo.roots.ContentFolderTypeProvider;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public abstract class DirectoryIndex {
  @Nonnull
  public static DirectoryIndex getInstance(Project project) {
    assert !project.isDefault() : "Must not call DirectoryIndex for default project";
    return ServiceManager.getService(project, DirectoryIndex.class);
  }

  /**
   * The same as {@link #getInfoForFile} but works only for directories or file roots and returns {@code null} for directories
   * which aren't included in project content or libraries
   *
   * @deprecated use {@link #getInfoForFile(com.intellij.openapi.vfs.VirtualFile)} instead
   */
  @Deprecated
  public abstract DirectoryInfo getInfoForDirectory(@Nonnull VirtualFile dir);

  @Nonnull
  public abstract DirectoryInfo getInfoForFile(@Nonnull VirtualFile file);

  @Nullable
  public abstract ContentFolderTypeProvider getContentFolderType(@Nonnull DirectoryInfo info);

  @Nonnull
  public abstract Query<VirtualFile> getDirectoriesByPackageName(@Nonnull String packageName, boolean includeLibrarySources);

  @Nullable
  public abstract String getPackageName(@Nonnull VirtualFile dir);

  @Nonnull
  public abstract OrderEntry[] getOrderEntries(@Nonnull DirectoryInfo info);
}
