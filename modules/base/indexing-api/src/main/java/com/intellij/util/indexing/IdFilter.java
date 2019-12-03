/*
 * Copyright 2000-2013 JetBrains s.r.o.
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

import consulo.logging.Logger;
import com.intellij.openapi.progress.ProgressIndicatorProvider;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.ContentIterator;
import com.intellij.openapi.roots.ProjectRootManager;
import consulo.util.dataholder.Key;
import com.intellij.openapi.vfs.VirtualFileManager;
import com.intellij.openapi.vfs.VirtualFileWithId;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.util.CachedValue;
import com.intellij.psi.util.CachedValueProvider;
import com.intellij.psi.util.CachedValuesManager;
import javax.annotation.Nonnull;

import javax.annotation.Nullable;
import java.util.BitSet;

public abstract class IdFilter {
  private static final Logger LOG = Logger.getInstance(IdFilter.class);
  private static final Key<CachedValue<IdFilter>> INSIDE_PROJECT = Key.create("INSIDE_PROJECT");
  private static final Key<CachedValue<IdFilter>> OUTSIDE_PROJECT = Key.create("OUTSIDE_PROJECT");

  @Nonnull
  public static IdFilter getProjectIdFilter(@Nonnull Project project, final boolean includeNonProjectItems) {
    Key<CachedValue<IdFilter>> key = includeNonProjectItems ? OUTSIDE_PROJECT : INSIDE_PROJECT;
    CachedValueProvider<IdFilter> provider = () -> CachedValueProvider.Result.create(buildProjectIdFilter(project, includeNonProjectItems), ProjectRootManager.getInstance(project), VirtualFileManager.VFS_STRUCTURE_MODIFICATIONS);
    return CachedValuesManager.getManager(project).getCachedValue(project, key, provider, false);
  }

  @Nonnull
  private static IdFilter buildProjectIdFilter(Project project, boolean includeNonProjectItems) {
    long started = System.currentTimeMillis();
    final BitSet idSet = new BitSet();

    ContentIterator iterator = fileOrDir -> {
      idSet.set(((VirtualFileWithId)fileOrDir).getId());
      ProgressManager.checkCanceled();
      return true;
    };

    if (!includeNonProjectItems) {
      ProjectRootManager.getInstance(project).getFileIndex().iterateContent(iterator);
    }
    else {
      FileBasedIndex.getInstance().iterateIndexableFiles(iterator, project, ProgressIndicatorProvider.getGlobalProgressIndicator());
    }

    if (LOG.isDebugEnabled()) {
      long elapsed = System.currentTimeMillis() - started;
      LOG.debug("Done filter (includeNonProjectItems=" + includeNonProjectItems + ") " + "in " + elapsed + "ms. Total files in set: " + idSet.cardinality());
    }
    return new IdFilter() {
      @Override
      public boolean containsFileId(int id) {
        return id >= 0 && idSet.get(id);
      }

      @Nonnull
      @Override
      public GlobalSearchScope getEffectiveFilteringScope() {
        return includeNonProjectItems ? GlobalSearchScope.allScope(project) : GlobalSearchScope.projectScope(project);
      }
    };
  }

  public abstract boolean containsFileId(int id);

  @Nullable
  public GlobalSearchScope getEffectiveFilteringScope() {
    return null;
  }
}
