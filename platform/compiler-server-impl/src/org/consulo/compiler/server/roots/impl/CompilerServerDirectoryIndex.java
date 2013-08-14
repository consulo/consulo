/*
 * Copyright 2013 Consulo.org
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

import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.ContentEntry;
import com.intellij.openapi.roots.ContentFolder;
import com.intellij.openapi.roots.ModuleRootManager;
import com.intellij.openapi.roots.OrderEntry;
import com.intellij.openapi.roots.impl.DirectoryIndex;
import com.intellij.openapi.roots.impl.DirectoryInfo;
import com.intellij.openapi.vfs.VfsUtilCore;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.util.Query;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * @author VISTALL
 * @since 15:43/12.08.13
 */
public class CompilerServerDirectoryIndex extends DirectoryIndex {
  @NotNull
  private final Project myProject;
  private final ModuleManager myModuleManager;

  public CompilerServerDirectoryIndex(@NotNull Project project,
                                      @NotNull ModuleManager moduleManager) {
    myProject = project;
    myModuleManager = moduleManager;
  }

  @Override
  public void checkConsistency() {
  }

  @Override
  public DirectoryInfo getInfoForDirectory(@NotNull VirtualFile fileForInfo) {

    Module module = null;
    VirtualFile contentRoot = null;
    VirtualFile sourceRoot = null;
    byte flags = 0;

    for (Module moduleIter : myModuleManager.getModules()) {
      final ModuleRootManager moduleRootManager = ModuleRootManager.getInstance(moduleIter);
      for (ContentEntry contentEntry : moduleRootManager.getContentEntries()) {
        if(VfsUtilCore.isAncestor(contentEntry.getFile(), fileForInfo, false)) {
          contentRoot = contentEntry.getFile();
          module = moduleIter;

          for (ContentFolder contentFolder : contentEntry.getFolders()) {
            if(VfsUtilCore.isAncestor(contentFolder.getFile(), fileForInfo, false)) {
              sourceRoot = contentFolder.getFile();

              flags |= DirectoryInfo.MODULE_SOURCE_FLAG;
              switch (contentFolder.getType()) {
                case TEST:
                  flags |= DirectoryInfo.MODULE_TEST_FLAG;
                  break;
                case RESOURCE:
                  flags |= DirectoryInfo.MODULE_RESOURCE_FLAG;
                  break;
              }
            }
          }

          break;
        }
      }
    }
    DirectoryInfo directoryInfo = DirectoryInfo.createNew();
    directoryInfo = directoryInfo.with(module, contentRoot, sourceRoot, null, flags, OrderEntry.EMPTY_ARRAY);
    return directoryInfo;
  }

  @Override
  public boolean isProjectExcludeRoot(@NotNull VirtualFile dir) {
    return false;
  }

  @NotNull
  @Override
  public Query<VirtualFile> getDirectoriesByPackageName(@NotNull String packageName, boolean includeLibrarySources) {
    return null;
  }

  @Nullable
  @Override
  public String getPackageName(@NotNull VirtualFile dir) {
    return null;
  }

  @Override
  public boolean isInitialized() {
    return true;
  }
}
