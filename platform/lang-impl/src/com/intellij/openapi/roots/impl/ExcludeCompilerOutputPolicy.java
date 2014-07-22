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

import com.intellij.compiler.CompilerConfiguration;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.ContentEntry;
import com.intellij.openapi.roots.ModuleRootModel;
import com.intellij.openapi.util.Comparing;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.pointers.VirtualFilePointer;
import org.consulo.compiler.ModuleCompilerPathsManager;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.mustbe.consulo.roots.ContentFolderScopes;
import org.mustbe.consulo.roots.ContentFolderTypeProvider;

import java.util.ArrayList;
import java.util.List;

/**
 * @author yole
 */
public class ExcludeCompilerOutputPolicy implements DirectoryIndexExcludePolicy {
  private final Project myProject;

  public ExcludeCompilerOutputPolicy(final Project project) {
    myProject = project;
  }

  @Override
  public boolean isExcludeRoot(final VirtualFile file) {
    CompilerConfiguration manager = CompilerConfiguration.getInstance(myProject);
    if (isEqualWithFileOrUrl(file, manager.getCompilerOutput(), manager.getCompilerOutputUrl())) {
      return true;
    }

    for (Module m : ModuleManager.getInstance(myProject).getModules()) {
      ModuleCompilerPathsManager moduleCompilerPathsManager = ModuleCompilerPathsManager.getInstance(m);
      for (ContentFolderTypeProvider contentFolderType : ContentFolderTypeProvider.filter(ContentFolderScopes.productionAndTest())) {
        if (isEqualWithFileOrUrl(file, moduleCompilerPathsManager.getCompilerOutput(contentFolderType),
                                 moduleCompilerPathsManager.getCompilerOutputUrl(contentFolderType))) {
          return true;
        }
      }
    }
    return false;
  }

  @Override
  public boolean isExcludeRootForModule(@NotNull final Module module, final VirtualFile excludeRoot) {
    ModuleCompilerPathsManager manager = ModuleCompilerPathsManager.getInstance(module);

    for (ContentFolderTypeProvider contentFolderType : ContentFolderTypeProvider.filter(ContentFolderScopes.productionAndTest())) {
      if (Comparing.equal(manager.getCompilerOutputUrl(contentFolderType), excludeRoot)) {
        return true;
      }
    }
    return false;
  }

  @NotNull
  @Override
  public VirtualFile[] getExcludeRootsForProject() {
    VirtualFile outputPath = CompilerConfiguration.getInstance(myProject).getCompilerOutput();
    if (outputPath != null) {
      return new VirtualFile[]{outputPath};
    }
    return VirtualFile.EMPTY_ARRAY;
  }

  @NotNull
  @Override
  public VirtualFilePointer[] getExcludeRootsForModule(@NotNull final ModuleRootModel rootModel) {
    ModuleCompilerPathsManager manager = ModuleCompilerPathsManager.getInstance(rootModel.getModule());
    List<VirtualFilePointer> result = new ArrayList<VirtualFilePointer>(3);

    if (manager.isInheritedCompilerOutput()) {
      final VirtualFilePointer compilerOutputPointer = CompilerConfiguration.getInstance(myProject).getCompilerOutputPointer();
      for(ContentEntry contentEntry : rootModel.getContentEntries()) {
        if(compilerOutputPointer.getUrl().contains(contentEntry.getUrl())) {
          result.add(compilerOutputPointer);
        }
      }
    }
    else {
      if (!manager.isExcludeOutput()) {
        return VirtualFilePointer.EMPTY_ARRAY;
      }

      for (ContentFolderTypeProvider contentFolderType : ContentFolderTypeProvider.filter(ContentFolderScopes.productionAndTest())) {
        result.add(manager.getCompilerOutputPointer(contentFolderType));
      }
    }
    return result.isEmpty() ? VirtualFilePointer.EMPTY_ARRAY : result.toArray(new VirtualFilePointer[result.size()]);
  }

  private static boolean isEqualWithFileOrUrl(@NotNull VirtualFile file, @Nullable VirtualFile fileToCompareWith, @Nullable String url) {
    if (fileToCompareWith != null) {
      if (Comparing.equal(fileToCompareWith, file)) return true;
    }
    else if (url != null) {
      if (FileUtil.pathsEqual(url, file.getUrl())) return true;
    }
    return false;
  }
}
