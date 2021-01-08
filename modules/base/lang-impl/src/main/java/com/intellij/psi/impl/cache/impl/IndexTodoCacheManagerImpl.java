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

package com.intellij.psi.impl.cache.impl;

import com.intellij.injected.editor.VirtualFileWindow;
import com.intellij.openapi.application.ReadAction;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiManager;
import com.intellij.psi.impl.cache.TodoCacheManager;
import com.intellij.psi.impl.cache.impl.todo.TodoIndex;
import com.intellij.psi.impl.cache.impl.todo.TodoIndexEntry;
import com.intellij.psi.impl.cache.impl.todo.TodoIndexers;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.search.IndexPattern;
import com.intellij.psi.search.IndexPatternProvider;
import com.intellij.psi.util.PsiUtilCore;
import com.intellij.testFramework.LightVirtualFile;
import com.intellij.util.indexing.*;
import consulo.logging.Logger;
import consulo.util.collection.ContainerUtil;
import consulo.util.lang.ref.SimpleReference;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.IOException;
import java.util.*;

/**
 * @author Eugene Zhuravlev
 * Date: Jan 16, 2008
 */
@Singleton
public class IndexTodoCacheManagerImpl extends TodoCacheManager {
  private static final Logger LOG = Logger.getInstance(IndexTodoCacheManagerImpl.class);

  private final Project myProject;
  private final PsiManager myPsiManager;

  @Inject
  public IndexTodoCacheManagerImpl(PsiManager psiManager) {
    myPsiManager = psiManager;
    myProject = psiManager.getProject();
  }

  @Override
  @Nonnull
  public PsiFile[] getFilesWithTodoItems() {
    if (myProject.isDefault()) {
      return PsiFile.EMPTY_ARRAY;
    }
    final FileBasedIndex fileBasedIndex = FileBasedIndex.getInstance();
    final Set<PsiFile> allFiles = new HashSet<>();

    fileBasedIndex.ignoreDumbMode(() -> {
      for (IndexPattern indexPattern : IndexPatternUtil.getIndexPatterns()) {
        final Collection<VirtualFile> files =
                fileBasedIndex.getContainingFiles(TodoIndex.NAME, new TodoIndexEntry(indexPattern.getPatternString(), indexPattern.isCaseSensitive()), GlobalSearchScope.allScope(myProject));
        for (VirtualFile file : files) {
          ReadAction.run(() -> {
            if (file.isValid() && TodoIndexers.belongsToProject(myProject, file)) {
              ContainerUtil.addIfNotNull(allFiles, myPsiManager.findFile(file));
            }
          });
        }
      }
    }, DumbModeAccessType.RELIABLE_DATA_ONLY);

    return allFiles.isEmpty() ? PsiFile.EMPTY_ARRAY : PsiUtilCore.toPsiFileArray(allFiles);
  }

  @Override
  public int getTodoCount(@Nonnull final VirtualFile file, @Nonnull final IndexPatternProvider patternProvider) {
    return getTodoCountImpl(file, patternProvider.getIndexPatterns());
  }

  @Override
  public int getTodoCount(@Nonnull final VirtualFile file, @Nonnull final IndexPattern pattern) {
    return getTodoCountImpl(file, pattern);
  }

  private int getTodoCountImpl(@Nonnull VirtualFile file, IndexPattern ... indexPatterns) {
    if (myProject.isDefault()) {
      return 0;
    }

    if (file instanceof VirtualFileWindow) {
      return -1;
    }

    if (file instanceof LightVirtualFile) {
      return calculateTodoCount((LightVirtualFile)file, indexPatterns);
    }

    if (!TodoIndexers.belongsToProject(myProject, file)) {
      return 0;
    }

    return fetchTodoCountFromIndex(file, indexPatterns);
  }

  private int calculateTodoCount(@Nonnull LightVirtualFile file, @Nonnull IndexPattern[] indexPatterns) {
    TodoIndex extension = FileBasedIndexExtension.EXTENSION_POINT_NAME.findExtension(TodoIndex.class);
    if (extension == null) return 0;

    try {
      FileContent fc = FileContentImpl.createByFile(file, myProject);
      Map<TodoIndexEntry, Integer> data = extension.getIndexer().map(fc);
      return getTodoCountForInputData(data, indexPatterns);
    }
    catch (IOException e) {
      LOG.error(e);
      return 0;
    }
  }

  private int fetchTodoCountFromIndex(@Nonnull VirtualFile file, @Nonnull IndexPattern[] indexPatterns) {
    SimpleReference<Map<TodoIndexEntry, Integer>> inputData = SimpleReference.create();
    FileBasedIndex.getInstance().ignoreDumbMode(() -> {
      Map<TodoIndexEntry, Integer> data = FileBasedIndex.getInstance().getFileData(TodoIndex.NAME, file, myProject);
      inputData.set(data);
    }, DumbModeAccessType.RELIABLE_DATA_ONLY);
    return getTodoCountForInputData(inputData.get(), indexPatterns);
  }

  private static int getTodoCountForInputData(@Nullable Map<TodoIndexEntry, Integer> data, @Nonnull IndexPattern[] indexPatterns) {
    if (data == null) return 0;

    return Arrays.stream(indexPatterns).map(p -> new TodoIndexEntry(p.getPatternString(), p.isCaseSensitive())).mapToInt(e -> data.getOrDefault(e, 0)).sum();
  }
}
