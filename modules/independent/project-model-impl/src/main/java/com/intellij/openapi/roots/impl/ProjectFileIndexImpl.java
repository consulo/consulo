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

package com.intellij.openapi.roots.impl;

import com.intellij.injected.editor.VirtualFileWindow;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.fileTypes.FileTypeRegistry;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.ContentIterator;
import com.intellij.openapi.roots.ModuleRootManager;
import com.intellij.openapi.roots.OrderEntry;
import com.intellij.openapi.roots.ProjectFileIndex;
import com.intellij.openapi.vfs.VfsUtilCore;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.VirtualFileFilter;
import javax.annotation.Nonnull;

import consulo.roots.ContentFolderScopes;
import consulo.roots.ContentFolderTypeProvider;
import consulo.roots.impl.ProductionResourceContentFolderTypeProvider;
import consulo.roots.impl.TestResourceContentFolderTypeProvider;

import java.util.Arrays;
import java.util.List;

public class ProjectFileIndexImpl extends FileIndexBase implements ProjectFileIndex {
  public static final Logger LOGGER = Logger.getInstance(ProjectFileIndexImpl.class);

  private final Project myProject;
  private final ContentFilter myContentFilter;

  public ProjectFileIndexImpl(@Nonnull Project project, @Nonnull DirectoryIndex directoryIndex, @Nonnull FileTypeRegistry fileTypeManager) {
    super(directoryIndex, fileTypeManager, project);
    myProject = project;
    myContentFilter = new ContentFilter();
  }

  @Override
  public boolean iterateContent(@Nonnull ContentIterator iterator) {
    Module[] modules = ModuleManager.getInstance(myProject).getModules();
    for (Module module : modules) {
      if (module.isDisposed()) continue;
      VirtualFile[] contentRoots = ModuleRootManager.getInstance(module).getContentRoots();
      for (VirtualFile contentRoot : contentRoots) {
        DirectoryInfo info = getInfoForFileOrDirectory(contentRoot);
        if (!module.equals(info.getModule())) continue; // maybe 2 modules have the same content root?

        VirtualFile parent = contentRoot.getParent();
        if (parent != null) {
          DirectoryInfo parentInfo = getInfoForFileOrDirectory(parent);
          if (parentInfo.getModule() != null) continue; // inner content - skip it
        }

        boolean finished = VfsUtilCore.iterateChildrenRecursively(contentRoot, myContentFilter, iterator);
        if (!finished) return false;
      }
    }

    return true;
  }

  @Override
  public boolean iterateContentUnderDirectory(@Nonnull VirtualFile dir, @Nonnull ContentIterator iterator) {
    return VfsUtilCore.iterateChildrenRecursively(dir, myContentFilter, iterator);
  }

  @Override
  public boolean isIgnored(@Nonnull VirtualFile file) {
    return isExcluded(file);
  }

  @Override
  public boolean isExcluded(@Nonnull VirtualFile file) {
    DirectoryInfo info = getInfoForFileOrDirectory(file);
    return info.isIgnored() || info.isExcluded();
  }

  @Override
  public boolean isUnderIgnored(@Nonnull VirtualFile file) {
    return getInfoForFileOrDirectory(file).isIgnored();
  }

  @Override
  public Module getModuleForFile(@Nonnull VirtualFile file) {
    return getModuleForFile(file, true);
  }

  @javax.annotation.Nullable
  @Override
  public Module getModuleForFile(@Nonnull VirtualFile file, boolean honorExclusion) {
    if (file instanceof VirtualFileWindow) file = ((VirtualFileWindow)file).getDelegate();
    DirectoryInfo info = getInfoForFileOrDirectory(file);
    if (info.isInProject() || !honorExclusion && info.isExcluded()) {
      return info.getModule();
    }
    return null;
  }

  @Override
  @Nonnull
  public List<OrderEntry> getOrderEntriesForFile(@Nonnull VirtualFile file) {
    return Arrays.asList(myDirectoryIndex.getOrderEntries(getInfoForFileOrDirectory(file)));
  }

  @Override
  public VirtualFile getClassRootForFile(@Nonnull VirtualFile file) {
    final DirectoryInfo info = getInfoForFileOrDirectory(file);
    if (!info.isInProject()) return null;
    return info.getLibraryClassRoot();
  }

  @Override
  public VirtualFile getSourceRootForFile(@Nonnull VirtualFile file) {
    final DirectoryInfo info = getInfoForFileOrDirectory(file);
    if (!info.isInProject()) return null;
    return info.getSourceRoot();
  }

  @Override
  public VirtualFile getContentRootForFile(@Nonnull VirtualFile file) {
    return getContentRootForFile(file, true);
  }

  @Override
  public VirtualFile getContentRootForFile(@Nonnull VirtualFile file, final boolean honorExclusion) {
    final DirectoryInfo info = getInfoForFileOrDirectory(file);
    if (info.isInProject() || !honorExclusion && info.isExcluded()) {
      return info.getContentRoot();
    }
    return null;
  }

  @javax.annotation.Nullable
  @Override
  public ContentFolderTypeProvider getContentFolderTypeForFile(@Nonnull VirtualFile file) {
    final DirectoryInfo info = getInfoForFileOrDirectory(file);
    return myDirectoryIndex.getContentFolderType(info);
  }

  @Override
  public String getPackageNameByDirectory(@Nonnull VirtualFile dir) {
    LOGGER.assertTrue(dir.isDirectory());
    return myDirectoryIndex.getPackageName(dir);
  }

  @Override
  public boolean isLibraryClassFile(@Nonnull VirtualFile file) {
    if (file.isDirectory()) return false;
    DirectoryInfo parentInfo = getInfoForFileOrDirectory(file);
    return parentInfo.isInProject() && parentInfo.hasLibraryClassRoot();
  }

  @Override
  public boolean isInSource(@Nonnull VirtualFile fileOrDir) {
    DirectoryInfo info = getInfoForFileOrDirectory(fileOrDir);
    return (info.isInModuleSource() || info.isInLibrarySource());
  }

  @Override
  public boolean isInResource(@Nonnull VirtualFile fileOrDir) {
    DirectoryInfo info = getInfoForFileOrDirectory(fileOrDir);
    return info.isInModuleSource() && myDirectoryIndex.getContentFolderType(info) == ProductionResourceContentFolderTypeProvider.getInstance();
  }

  @Override
  public boolean isInTestResource(@Nonnull VirtualFile fileOrDir) {
    DirectoryInfo info = getInfoForFileOrDirectory(fileOrDir);
    return info.isInModuleSource() && myDirectoryIndex.getContentFolderType(info) == TestResourceContentFolderTypeProvider.getInstance();
  }

  @Override
  public boolean isInLibraryClasses(@Nonnull VirtualFile fileOrDir) {
    DirectoryInfo info = getInfoForFileOrDirectory(fileOrDir);
    return info.hasLibraryClassRoot();
  }

  @Override
  public boolean isInLibrarySource(@Nonnull VirtualFile fileOrDir) {
    DirectoryInfo info = getInfoForFileOrDirectory(fileOrDir);
    return info.isInLibrarySource();
  }

  @Override
  public boolean isInContent(@Nonnull VirtualFile fileOrDir) {
    DirectoryInfo info = getInfoForFileOrDirectory(fileOrDir);
    return info.isInProject() && info.getModule() != null;
  }

  @Override
  public boolean isInSourceContent(@Nonnull VirtualFile fileOrDir) {
    return getInfoForFileOrDirectory(fileOrDir).isInModuleSource();
  }

  @Override
  public boolean isInTestSourceContent(@Nonnull VirtualFile fileOrDir) {
    DirectoryInfo info = getInfoForFileOrDirectory(fileOrDir);
    return info.isInModuleSource() && ContentFolderScopes.test().apply(myDirectoryIndex.getContentFolderType(info));
  }

  private class ContentFilter implements VirtualFileFilter {
    @Override
    public boolean accept(@Nonnull VirtualFile file) {
      DirectoryInfo info = getInfoForFileOrDirectory(file);
      if (info.getModule() == null) return false;

      if (file.isDirectory()) {
        return true;
      }
      else {
        return !myFileTypeRegistry.isFileIgnored(file);
      }
    }
  }
}
