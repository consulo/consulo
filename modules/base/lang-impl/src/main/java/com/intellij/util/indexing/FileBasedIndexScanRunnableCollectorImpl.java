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
package com.intellij.util.indexing;

import com.intellij.openapi.extensions.Extensions;
import com.intellij.openapi.fileTypes.FileTypeManager;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleManager;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.*;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.util.containers.JBIterable;
import consulo.application.AccessRule;
import consulo.roots.OrderEntryWithTracking;
import consulo.roots.types.BinariesOrderRootType;
import consulo.roots.types.SourcesOrderRootType;
import jakarta.inject.Inject;
import jakarta.inject.Provider;
import jakarta.inject.Singleton;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Singleton
public class FileBasedIndexScanRunnableCollectorImpl extends FileBasedIndexScanRunnableCollector {
  private final Project myProject;
  @Nonnull
  private final Provider<ProjectFileIndex> myProjectFileIndexProvider;
  private final FileTypeManager myFileTypeManager;

  @Inject
  public FileBasedIndexScanRunnableCollectorImpl(@Nonnull Project project, @Nonnull Provider<ProjectFileIndex> projectFileIndexProvider) {
    myProject = project;
    myProjectFileIndexProvider = projectFileIndexProvider;
    myFileTypeManager = FileTypeManager.getInstance();
  }

  @Override
  public boolean shouldCollect(@Nonnull VirtualFile file) {
    ProjectFileIndex projectFileIndex = myProjectFileIndexProvider.get();
    if (projectFileIndex.isInContent(file) || projectFileIndex.isInLibraryClasses(file) || projectFileIndex.isInLibrarySource(file)) {
      return !myFileTypeManager.isFileIgnored(file);
    }
    return false;
  }

  @Override
  public List<Runnable> collectScanRootRunnables(@Nonnull ContentIterator processor, ProgressIndicator indicator) {
    ProjectFileIndex projectFileIndex = myProjectFileIndexProvider.get();

    return AccessRule.read(() -> {
      if (myProject.isDisposed()) {
        return Collections.emptyList();
      }

      List<Runnable> tasks = new ArrayList<>();
      final Set<VirtualFile> visitedRoots = ConcurrentHashMap.newKeySet();

      tasks.add(() -> projectFileIndex.iterateContent(processor, file -> !file.isDirectory() || visitedRoots.add(file)));

      JBIterable<VirtualFile> contributedRoots = JBIterable.empty();
      for (IndexableSetContributor contributor : Extensions.getExtensions(IndexableSetContributor.EP_NAME)) {
        //important not to depend on project here, to support per-project background reindex
        // each client gives a project to FileBasedIndex
        if (myProject.isDisposed()) {
          return tasks;
        }
        contributedRoots = contributedRoots.append(IndexableSetContributor.getRootsToIndex(contributor));
        contributedRoots = contributedRoots.append(IndexableSetContributor.getProjectRootsToIndex(contributor, myProject));
      }

      for (VirtualFile root : contributedRoots) {
        if (visitedRoots.add(root)) {
          tasks.add(() -> {
            if (myProject.isDisposed() || !root.isValid()) return;
            FileBasedIndex.iterateRecursively(root, processor, indicator, visitedRoots, null);
          });
        }
      }

      // iterate associated libraries
      for (final Module module : ModuleManager.getInstance(myProject).getModules()) {
        OrderEntry[] orderEntries = ModuleRootManager.getInstance(module).getOrderEntries();
        for (OrderEntry orderEntry : orderEntries) {
          if (orderEntry instanceof OrderEntryWithTracking) {
            if (orderEntry.isValid()) {
              final VirtualFile[] libSources = orderEntry.getFiles(SourcesOrderRootType.getInstance());
              final VirtualFile[] libClasses = orderEntry.getFiles(BinariesOrderRootType.getInstance());
              for (VirtualFile[] roots : new VirtualFile[][]{libSources, libClasses}) {
                for (final VirtualFile root : roots) {
                  if (visitedRoots.add(root)) {
                    tasks.add(() -> {
                      if (myProject.isDisposed() || module.isDisposed() || !root.isValid()) return;
                      FileBasedIndex.iterateRecursively(root, processor, indicator, visitedRoots, projectFileIndex);
                    });
                  }
                }
              }
            }
          }
        }
      }
      return tasks;
    });
  }
}
