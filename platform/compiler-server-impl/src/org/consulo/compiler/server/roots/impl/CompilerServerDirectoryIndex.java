/*
 * Copyright 2013-2014 must-be.org
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
package org.consulo.compiler.server.roots.impl;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.impl.DirectoryIndex;
import com.intellij.openapi.roots.impl.DirectoryInfo;
import com.intellij.openapi.roots.impl.RootIndex;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.util.Query;
import org.consulo.lombok.annotations.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.TestOnly;
import org.mustbe.consulo.roots.ContentFolderTypeProvider;

/**
 * @see com.intellij.openapi.roots.impl.DirectoryIndexImpl
 */
@Logger
public class CompilerServerDirectoryIndex extends DirectoryIndex {
  private final Project myProject;

  private volatile RootIndex myRootIndex = null;

  public CompilerServerDirectoryIndex(@NotNull Project project) {
    myProject = project;
  }

  @Override
  @NotNull
  public Query<VirtualFile> getDirectoriesByPackageName(@NotNull String packageName, boolean includeLibrarySources) {
    RootIndex rootIndex = getRootIndex();
    return rootIndex.getDirectoriesByPackageName(packageName, includeLibrarySources);
  }

  @NotNull
  private RootIndex getRootIndex() {
    RootIndex rootIndex = myRootIndex;
    if (rootIndex == null) {
      myRootIndex = rootIndex = new RootIndex(myProject);
    }
    return rootIndex;
  }

  @Override
  @TestOnly
  public void checkConsistency() {
    RootIndex rootIndex = getRootIndex();
    rootIndex.checkConsistency();
  }

  @Override
  public DirectoryInfo getInfoForDirectory(@NotNull VirtualFile dir) {
    RootIndex rootIndex = getRootIndex();
    return rootIndex.getInfoForDirectory(dir);
  }

  @Override
  @Nullable
  public ContentFolderTypeProvider getContentFolderType(@NotNull DirectoryInfo info) {
    if (info.isInModuleSource()) {
      RootIndex rootIndex = getRootIndex();
      return rootIndex.getContentFolderType(info);
    }
    return null;
  }

  @Override
  public boolean isProjectExcludeRoot(@NotNull VirtualFile dir) {
    return getRootIndex().isProjectExcludeRoot(dir);
  }

  @Override
  public boolean isModuleExcludeRoot(@NotNull VirtualFile dir) {
    return getRootIndex().isModuleExcludeRoot(dir);
  }

  @Override
  public String getPackageName(@NotNull VirtualFile dir) {
    RootIndex rootIndex = getRootIndex();
    return rootIndex.getPackageName(dir);
  }
}
