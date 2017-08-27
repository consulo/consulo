/*
 * Copyright 2013-2016 consulo.io
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
package consulo.compiler.server.roots.impl;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.OrderEntry;
import com.intellij.openapi.roots.impl.DirectoryIndex;
import com.intellij.openapi.roots.impl.DirectoryInfo;
import com.intellij.openapi.roots.impl.RootIndex;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.util.Query;
import com.intellij.util.containers.ConcurrentHashMap;
import consulo.roots.ContentFolderTypeProvider;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Map;

/**
 * @see com.intellij.openapi.roots.impl.DirectoryIndexImpl
 */
public class CompilerServerDirectoryIndex extends DirectoryIndex {
  public static final Logger LOGGER = Logger.getInstance(CompilerServerDirectoryIndex.class);

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
      myRootIndex = rootIndex = new RootIndex(myProject, createRootInfoCache());
    }
    return rootIndex;
  }

  protected RootIndex.InfoCache createRootInfoCache() {
    return new RootIndex.InfoCache() {
      // Upsource can't use int-mapping because different files may have the same id there
      private final Map<VirtualFile, DirectoryInfo> myInfoCache = new ConcurrentHashMap<VirtualFile, DirectoryInfo>();
      @Override
      public void cacheInfo(@NotNull VirtualFile dir, @NotNull DirectoryInfo info) {
        myInfoCache.put(dir, info);
      }

      @Override
      public DirectoryInfo getCachedInfo(@NotNull VirtualFile dir) {
        return myInfoCache.get(dir);
      }
    };
  }

  @Override
  public DirectoryInfo getInfoForDirectory(@NotNull VirtualFile dir) {
    DirectoryInfo info = getInfoForFile(dir);
    return info.isInProject() ? info : null;
  }

  @NotNull
  @Override
  public DirectoryInfo getInfoForFile(@NotNull VirtualFile file) {
    return getRootIndex().getInfoForFile(file);
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
  public String getPackageName(@NotNull VirtualFile dir) {
    RootIndex rootIndex = getRootIndex();
    return rootIndex.getPackageName(dir);
  }

  @NotNull
  @Override
  public OrderEntry[] getOrderEntries(@NotNull DirectoryInfo info) {
    return getRootIndex().getOrderEntries(info);
  }
}
