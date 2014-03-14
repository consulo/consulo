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
package org.consulo.compiler.server.fileSystem;

import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.ContentIterator;
import com.intellij.openapi.util.Condition;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.util.Processor;
import com.intellij.util.indexing.FileBasedIndex;
import com.intellij.util.indexing.ID;
import com.intellij.util.indexing.IndexableFileSet;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;

/**
 * @author VISTALL
 * @since 12:08/14.08.13
 */
public class CompilerServerFileBasedIndexImpl extends FileBasedIndex {
  @Override
  public void iterateIndexableFiles(@NotNull ContentIterator processor, @NotNull Project project, ProgressIndicator indicator) {
  }

  @Override
  public void registerIndexableSet(@NotNull IndexableFileSet set, @Nullable Project project) {
  }

  @Override
  public void removeIndexableSet(@NotNull IndexableFileSet set) {
  }

  @Override
  public VirtualFile findFileById(Project project, int id) {
    return null;
  }

  @NotNull
  @Override
  public <K, V> List<V> getValues(@NotNull ID<K, V> indexId, @NotNull K dataKey, @NotNull GlobalSearchScope filter) {
    return null;
  }

  @NotNull
  @Override
  public <K, V> Collection<VirtualFile> getContainingFiles(@NotNull ID<K, V> indexId,
                                                           @NotNull K dataKey,
                                                           @NotNull GlobalSearchScope filter) {
    return Collections.emptyList();
  }

  @Override
  public <K, V> boolean processValues(@NotNull ID<K, V> indexId,
                                      @NotNull K dataKey,
                                      @Nullable VirtualFile inFile,
                                      @NotNull ValueProcessor<V> processor,
                                      @NotNull GlobalSearchScope filter) {
    return false;
  }

  @Override
  public <K, V> boolean processFilesContainingAllKeys(@NotNull ID<K, V> indexId,
                                                      @NotNull Collection<K> dataKeys,
                                                      @NotNull GlobalSearchScope filter,
                                                      @Nullable Condition<V> valueChecker,
                                                      @NotNull Processor<VirtualFile> processor) {
    return false;
  }

  @NotNull
  @Override
  public <K> Collection<K> getAllKeys(@NotNull ID<K, ?> indexId, @NotNull Project project) {
    return null;
  }

  @Override
  public <K> void ensureUpToDate(@NotNull ID<K, ?> indexId, @Nullable Project project, @Nullable GlobalSearchScope filter) {
  }

  @Override
  public void requestRebuild(ID<?, ?> indexId, Throwable throwable) {
  }

  @Override
  public <K> void scheduleRebuild(@NotNull ID<K, ?> indexId, @NotNull Throwable e) {
  }

  @Override
  public void requestReindex(@NotNull VirtualFile file) {
  }

  @Override
  public void requestReindexExcluded(@NotNull VirtualFile file) {
  }

  @Override
  public <K, V> boolean getFilesWithKey(@NotNull ID<K, V> indexId,
                                        @NotNull Set<K> dataKeys,
                                        @NotNull Processor<VirtualFile> processor,
                                        @NotNull GlobalSearchScope filter) {
    return false;
  }

  @Override
  public <K> boolean processAllKeys(@NotNull ID<K, ?> indexId, @NotNull Processor<K> processor, @Nullable Project project) {
    return false;
  }

  @Override
  public void initComponent() {
  }

  @Override
  public void disposeComponent() {
  }
}
