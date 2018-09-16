/*
 * Copyright 2000-2010 JetBrains s.r.o.
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
package com.intellij.openapi.compiler.generic;

import com.intellij.openapi.compiler.CompileContext;
import com.intellij.openapi.project.Project;
import javax.annotation.Nonnull;

import java.io.File;
import java.util.List;

/**
 * @author nik
 */
public abstract class GenericCompilerInstance<T extends BuildTarget, Item extends CompileItem<Key, SourceState, OutputState>, Key, SourceState, OutputState> {
  protected final CompileContext myContext;

  protected GenericCompilerInstance(CompileContext context) {
    myContext = context;
  }

  protected Project getProject() {
    return myContext.getProject();
  }

  @Nonnull
  public abstract List<T> getAllTargets();

  @Nonnull
  public abstract List<T> getSelectedTargets();

  public abstract void processObsoleteTarget(@Nonnull String targetId, @Nonnull List<GenericCompilerCacheState<Key, SourceState, OutputState>> obsoleteItems);


  @Nonnull
  public abstract List<Item> getItems(@Nonnull T target);

  public abstract void processItems(@Nonnull T target, @Nonnull List<GenericCompilerProcessingItem<Item, SourceState, OutputState>> changedItems, @Nonnull List<GenericCompilerCacheState<Key, SourceState, OutputState>> obsoleteItems,
                                    @Nonnull OutputConsumer<Item> consumer);

  public interface OutputConsumer<Item extends CompileItem<?,?,?>> {
    void addFileToRefresh(@Nonnull File file);

    void addDirectoryToRefresh(@Nonnull File dir);

    void addProcessedItem(@Nonnull Item sourceItem);
  }
}
