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

package consulo.ide.impl.psi.impl.cache.impl;

import consulo.annotation.component.ServiceImpl;
import consulo.application.ReadAction;
import consulo.language.psi.stub.todo.TodoCacheManager;
import consulo.ide.impl.psi.impl.cache.impl.todo.TodoIndex;
import consulo.language.psi.stub.*;
import consulo.language.psi.stub.todo.TodoIndexEntry;
import consulo.language.file.inject.VirtualFileWindow;
import consulo.language.file.light.LightVirtualFile;
import consulo.language.impl.internal.psi.stub.FileContentImpl;
import consulo.language.psi.PsiFile;
import consulo.language.psi.PsiManager;
import consulo.language.psi.PsiUtilCore;
import consulo.language.psi.scope.GlobalSearchScope;
import consulo.language.psi.search.IndexPattern;
import consulo.language.psi.search.IndexPatternProvider;
import consulo.logging.Logger;
import consulo.module.content.ProjectFileIndex;
import consulo.project.Project;
import consulo.util.collection.ContainerUtil;
import consulo.util.lang.ref.SimpleReference;
import consulo.virtualFileSystem.VirtualFile;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import java.io.IOException;
import java.util.*;

/**
 * @author Eugene Zhuravlev
 * @since 2008-01-16
 */
@Singleton
@ServiceImpl
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
            if (file.isValid() && belongsToProject(myProject, file)) {
              ContainerUtil.addIfNotNull(allFiles, myPsiManager.findFile(file));
            }
          });
        }
      }
    }, DumbModeAccessType.RELIABLE_DATA_ONLY);

    return allFiles.isEmpty() ? PsiFile.EMPTY_ARRAY : PsiUtilCore.toPsiFileArray(allFiles);
  }

  public static boolean belongsToProject(@Nonnull Project project, @Nonnull VirtualFile file) {
    if (!ProjectFileIndex.getInstance(project).isInContent(file)) {
      return false;
    }
    return true;
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

    if (!belongsToProject(myProject, file)) {
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
