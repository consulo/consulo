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
package com.intellij.util.indexing;

import com.intellij.openapi.roots.ContentIterator;
import com.intellij.openapi.vfs.VfsUtilCore;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.VirtualFileVisitor;

import javax.annotation.Nonnull;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * @author peter
 */
public class AdditionalIndexableFileSet implements IndexableFileSet {
  private volatile Set<VirtualFile> cachedFiles;
  private volatile Set<VirtualFile> cachedDirectories;
  private volatile List<IndexableSetContributor> myExtensions;

  public AdditionalIndexableFileSet(IndexableSetContributor... extensions) {
    myExtensions = extensions == null ? null : Arrays.asList(extensions);
  }

  private Set<VirtualFile> getDirectories() {
    Set<VirtualFile> directories = cachedDirectories;
    if (directories == null || filesInvalidated(directories) || filesInvalidated(cachedFiles)) {
      directories = collectFilesAndDirectories();
    }
    return directories;
  }

  private Set<VirtualFile> collectFilesAndDirectories() {
    Set<VirtualFile> files = new HashSet<>();
    Set<VirtualFile> directories = new HashSet<>();
    if (myExtensions == null) {
      myExtensions = IndexableSetContributor.EP_NAME.getExtensionList();
    }
    for (IndexableSetContributor provider : myExtensions) {
      for (VirtualFile file : provider.getAdditionalRootsToIndex()) {
        (file.isDirectory() ? directories : files).add(file);
      }
    }
    cachedFiles = files;
    cachedDirectories = directories;
    return directories;
  }

  public static boolean filesInvalidated(Set<VirtualFile> files) {
    for (VirtualFile file : files) {
      if (!file.isValid()) {
        return true;
      }
    }
    return false;
  }

  public AdditionalIndexableFileSet() {
  }

  @Override
  public boolean isInSet(@Nonnull VirtualFile file) {
    for (final VirtualFile root : getDirectories()) {
      if (VfsUtilCore.isAncestor(root, file, false)) {
        return true;
      }
    }
    return cachedFiles.contains(file);
  }

  @Override
  public void iterateIndexableFilesIn(@Nonnull VirtualFile file, @Nonnull final ContentIterator iterator) {
    VfsUtilCore.visitChildrenRecursively(file, new VirtualFileVisitor() {
      @Override
      public boolean visitFile(@Nonnull VirtualFile file) {
        if (!isInSet(file)) {
          return false;
        }

        if (!file.isDirectory()) {
          iterator.processFile(file);
        }

        return true;
      }
    });
  }
}
