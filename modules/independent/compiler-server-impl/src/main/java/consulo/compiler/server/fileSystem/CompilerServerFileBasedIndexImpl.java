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
package consulo.compiler.server.fileSystem;

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
import javax.annotation.Nonnull;

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
  public void iterateIndexableFiles(@Nonnull ContentIterator processor, @Nonnull Project project, ProgressIndicator indicator) {
  }

  @Override
  public void registerIndexableSet(@Nonnull IndexableFileSet set, @javax.annotation.Nullable Project project) {
  }

  @Override
  public void removeIndexableSet(@Nonnull IndexableFileSet set) {
  }

  @Override
  public VirtualFile findFileById(Project project, int id) {
    return null;
  }

  @Nonnull
  @Override
  public <K, V> List<V> getValues(@Nonnull ID<K, V> indexId, @Nonnull K dataKey, @Nonnull GlobalSearchScope filter) {
    return null;
  }

  @Nonnull
  @Override
  public <K, V> Collection<VirtualFile> getContainingFiles(@Nonnull ID<K, V> indexId,
                                                           @Nonnull K dataKey,
                                                           @Nonnull GlobalSearchScope filter) {
    return Collections.emptyList();
  }

  @Override
  public <K, V> boolean processValues(@Nonnull ID<K, V> indexId,
                                      @Nonnull K dataKey,
                                      @javax.annotation.Nullable VirtualFile inFile,
                                      @Nonnull ValueProcessor<V> processor,
                                      @Nonnull GlobalSearchScope filter) {
    return false;
  }

  @Override
  public <K, V> boolean processFilesContainingAllKeys(@Nonnull ID<K, V> indexId,
                                                      @Nonnull Collection<K> dataKeys,
                                                      @Nonnull GlobalSearchScope filter,
                                                      @javax.annotation.Nullable Condition<V> valueChecker,
                                                      @Nonnull Processor<VirtualFile> processor) {
    return false;
  }

  @Nonnull
  @Override
  public <K> Collection<K> getAllKeys(@Nonnull ID<K, ?> indexId, @Nonnull Project project) {
    return null;
  }

  @Override
  public <K> void ensureUpToDate(@Nonnull ID<K, ?> indexId, @javax.annotation.Nullable Project project, @javax.annotation.Nullable GlobalSearchScope filter) {
  }

  @Override
  public void requestRebuild(ID<?, ?> indexId, Throwable throwable) {
  }

  @Override
  public <K> void scheduleRebuild(@Nonnull ID<K, ?> indexId, @Nonnull Throwable e) {
  }

  @Override
  public void requestReindex(@Nonnull VirtualFile file) {
  }

  @Override
  public <K, V> boolean getFilesWithKey(@Nonnull ID<K, V> indexId,
                                        @Nonnull Set<K> dataKeys,
                                        @Nonnull Processor<VirtualFile> processor,
                                        @Nonnull GlobalSearchScope filter) {
    return false;
  }

  @Override
  public <K> boolean processAllKeys(@Nonnull ID<K, ?> indexId, @Nonnull Processor<K> processor, @javax.annotation.Nullable Project project) {
    return false;
  }
}
