/*
 * Copyright 2000-2014 JetBrains s.r.o.
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

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.fileTypes.FileType;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.ContentIterator;
import com.intellij.openapi.roots.ProjectFileIndex;
import com.intellij.openapi.util.Condition;
import com.intellij.openapi.util.registry.Registry;
import com.intellij.openapi.vfs.*;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.util.Consumer;
import com.intellij.util.Processor;
import com.intellij.util.SystemProperties;
import org.jetbrains.annotations.NonNls;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Collection;
import java.util.List;
import java.util.Set;

/**
 * Author: dmitrylomov
 */
public abstract class FileBasedIndex {
  public abstract void iterateIndexableFiles(@Nonnull ContentIterator processor, @Nonnull Project project, ProgressIndicator indicator);

  public void iterateIndexableFilesConcurrently(@Nonnull ContentIterator processor, @Nonnull Project project, ProgressIndicator indicator) {
    iterateIndexableFiles(processor, project, indicator);
  }

  public abstract void registerIndexableSet(@Nonnull IndexableFileSet set, @javax.annotation.Nullable Project project);

  public abstract void removeIndexableSet(@Nonnull IndexableFileSet set);

  public static FileBasedIndex getInstance() {
    return ApplicationManager.getApplication().getComponent(FileBasedIndex.class);
  }

  public static int getFileId(@Nonnull final VirtualFile file) {
    if (file instanceof VirtualFileWithId) {
      return ((VirtualFileWithId)file).getId();
    }

    throw new IllegalArgumentException("Virtual file doesn't support id: " + file + ", implementation class: " + file.getClass().getName());
  }

  // note: upsource implementation requires access to Project here, please don't remove
  public abstract VirtualFile findFileById(Project project, int id);

  public void requestRebuild(ID<?, ?> indexId) {
    requestRebuild(indexId, new Throwable());
  }


  @NonNls
  @Nonnull
  public String getComponentName() {
    return "FileBasedIndex";
  }

  @Nonnull
  public abstract <K, V> List<V> getValues(@Nonnull ID<K, V> indexId, @Nonnull K dataKey, @Nonnull GlobalSearchScope filter);

  @Nonnull
  public abstract <K, V> Collection<VirtualFile> getContainingFiles(@Nonnull ID<K, V> indexId, @Nonnull K dataKey, @Nonnull GlobalSearchScope filter);

  /**
   * @return false if ValueProcessor.process() returned false; true otherwise or if ValueProcessor was not called at all
   */
  public abstract <K, V> boolean processValues(@Nonnull ID<K, V> indexId,
                                               @Nonnull K dataKey,
                                               @Nullable VirtualFile inFile,
                                               @Nonnull FileBasedIndex.ValueProcessor<V> processor,
                                               @Nonnull GlobalSearchScope filter);

  /**
   * @return false if ValueProcessor.process() returned false; true otherwise or if ValueProcessor was not called at all
   */
  public <K, V> boolean processValues(@Nonnull ID<K, V> indexId,
                                      @Nonnull K dataKey,
                                      @Nullable VirtualFile inFile,
                                      @Nonnull FileBasedIndex.ValueProcessor<V> processor,
                                      @Nonnull GlobalSearchScope filter,
                                      @javax.annotation.Nullable IdFilter idFilter) {
    return processValues(indexId, dataKey, inFile, processor, filter);
  }

  public abstract <K, V> boolean processFilesContainingAllKeys(@Nonnull ID<K, V> indexId,
                                                               @Nonnull Collection<K> dataKeys,
                                                               @Nonnull GlobalSearchScope filter,
                                                               @Nullable Condition<V> valueChecker,
                                                               @Nonnull Processor<VirtualFile> processor);

  /**
   * @param project it is guaranteed to return data which is up-to-date withing the project
   *                Keys obtained from the files which do not belong to the project specified may not be up-to-date or even exist
   */
  @Nonnull
  public abstract <K> Collection<K> getAllKeys(@Nonnull ID<K, ?> indexId, @Nonnull Project project);

  /**
   * DO NOT CALL DIRECTLY IN CLIENT CODE
   * The method is internal to indexing engine end is called internally. The method is public due to implementation details
   */
  public abstract <K> void ensureUpToDate(@Nonnull ID<K, ?> indexId, @Nullable Project project, @javax.annotation.Nullable GlobalSearchScope filter);

  public abstract void requestRebuild(ID<?, ?> indexId, Throwable throwable);

  public abstract <K> void scheduleRebuild(@Nonnull ID<K, ?> indexId, @Nonnull Throwable e);

  public abstract void requestReindex(@Nonnull VirtualFile file);

  public abstract <K, V> boolean getFilesWithKey(@Nonnull ID<K, V> indexId,
                                                 @Nonnull Set<K> dataKeys,
                                                 @Nonnull Processor<VirtualFile> processor,
                                                 @Nonnull GlobalSearchScope filter);

  /**
   * @param project it is guaranteed to return data which is up-to-date withing the project
   *                Keys obtained from the files which do not belong to the project specified may not be up-to-date or even exist
   */
  public abstract <K> boolean processAllKeys(@Nonnull ID<K, ?> indexId, @Nonnull Processor<K> processor, @Nullable Project project);

  public <K> boolean processAllKeys(@Nonnull ID<K, ?> indexId, @Nonnull Processor<K> processor, @Nonnull GlobalSearchScope scope, @Nullable IdFilter idFilter) {
    return processAllKeys(indexId, processor, scope.getProject());
  }

  public static void iterateRecursively(@Nullable final VirtualFile root,
                                        @Nonnull final ContentIterator processor,
                                        @Nullable final ProgressIndicator indicator,
                                        @javax.annotation.Nullable final Set<VirtualFile> visitedRoots,
                                        @Nullable final ProjectFileIndex projectFileIndex) {
    if (root == null) {
      return;
    }

    VfsUtilCore.visitChildrenRecursively(root, new VirtualFileVisitor() {
      @Override
      public boolean visitFile(@Nonnull VirtualFile file) {
        if (!acceptsFile(file)) return false;
        if (file.is(VFileProperty.SYMLINK)) {
          if (!Registry.is("indexer.follows.symlinks")) return false;
          VirtualFile canonicalFile = file.getCanonicalFile();

          if (canonicalFile != null) {
            if (!acceptsFile(canonicalFile)) return false;
          }
        }
        if (indicator != null) indicator.checkCanceled();

        processor.processFile(file);
        return true;
      }

      private boolean acceptsFile(@Nonnull VirtualFile file) {
        if (visitedRoots != null && !root.equals(file) && file.isDirectory() && !visitedRoots.add(file)) {
          return false;
        }
        if (projectFileIndex != null && projectFileIndex.isExcluded(file)) {
          return false;
        }
        return true;
      }
    });
  }

  public interface ValueProcessor<V> {
    /**
     * @param value a value to process
     * @param file  the file the value came from
     * @return false if no further processing is needed, true otherwise
     */
    boolean process(VirtualFile file, V value);
  }

  /**
   * Author: dmitrylomov
   */
  public interface InputFilter {
    boolean acceptInput(@javax.annotation.Nullable Project project, @Nonnull VirtualFile file);
  }

  public interface FileTypeSpecificInputFilter extends InputFilter {
    void registerFileTypesUsedForIndexing(@Nonnull Consumer<FileType> fileTypeSink);
  }

  // TODO: remove once changes becomes permanent
  public static final boolean ourEnableTracingOfKeyHashToVirtualFileMapping =
          SystemProperties.getBooleanProperty("idea.enable.tracing.keyhash2virtualfile", true);
}
