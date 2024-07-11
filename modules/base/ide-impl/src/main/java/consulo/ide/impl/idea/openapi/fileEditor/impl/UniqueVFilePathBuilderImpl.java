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
package consulo.ide.impl.idea.openapi.fileEditor.impl;

import consulo.annotation.component.ServiceImpl;
import consulo.application.util.CachedValue;
import consulo.application.util.CachedValueProvider;
import consulo.application.util.CachedValuesManager;
import consulo.application.util.UniqueNameBuilder;
import consulo.content.scope.SearchScope;
import consulo.fileEditor.FileEditorManager;
import consulo.fileEditor.UniqueVFilePathBuilder;
import consulo.util.io.FileUtil;
import consulo.util.collection.ContainerUtil;
import consulo.language.psi.PsiModificationTracker;
import consulo.language.psi.scope.GlobalSearchScope;
import consulo.language.psi.search.FilenameIndex;
import consulo.project.Project;
import consulo.util.collection.Maps;
import consulo.util.dataholder.Key;
import consulo.virtualFileSystem.VirtualFile;
import consulo.virtualFileSystem.VirtualFilePathWrapper;
import jakarta.inject.Singleton;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import java.io.File;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * @author yole
 */
@Singleton
@ServiceImpl
public class UniqueVFilePathBuilderImpl extends UniqueVFilePathBuilder {
  @Nonnull
  @Override
  public String getUniqueVirtualFilePath(@Nonnull Project project, @Nonnull VirtualFile file, @Nonnull SearchScope scope) {
    return getUniqueVirtualFilePath(project, file, false, scope);
  }

  @Override
  public String getUniqueVirtualFilePath(Project project, VirtualFile vFile) {
    return getUniqueVirtualFilePath(project, vFile, GlobalSearchScope.projectScope(project));
  }

  @Nonnull
  @Override
  public String getUniqueVirtualFilePathWithinOpenedFileEditors(Project project, VirtualFile vFile) {
    return getUniqueVirtualFilePath(project, vFile, true, GlobalSearchScope.projectScope(project));
  }

  private static final Key<CachedValue<Map<SearchScope, Map<String, UniqueNameBuilder<VirtualFile>>>>> ourShortNameBuilderCacheKey =
    Key.create("project's.short.file.name.builder");
  private static final Key<CachedValue<Map<SearchScope, Map<String, UniqueNameBuilder<VirtualFile>>>>> ourShortNameOpenedBuilderCacheKey =
    Key.create("project's.short.file.name.opened.builder");
  private static final UniqueNameBuilder<VirtualFile> ourEmptyBuilder = new UniqueNameBuilder<>(null, null, -1);

  private static String getUniqueVirtualFilePath(
    final Project project,
    VirtualFile file,
    final boolean skipNonOpenedFiles,
    SearchScope scope
  ) {
    Key<CachedValue<Map<SearchScope, Map<String, UniqueNameBuilder<VirtualFile>>>>> key =
      skipNonOpenedFiles ? ourShortNameOpenedBuilderCacheKey : ourShortNameBuilderCacheKey;
    CachedValue<Map<SearchScope, Map<String, UniqueNameBuilder<VirtualFile>>>> data = project.getUserData(key);
    if (data == null) {
      project.putUserData(
        key,
        data = CachedValuesManager.getManager(project).createCachedValue(
          () -> new CachedValueProvider.Result<Map<SearchScope, Map<String, UniqueNameBuilder<VirtualFile>>>>(
            new ConcurrentHashMap<>(2),
            PsiModificationTracker.MODIFICATION_COUNT,
            //ProjectRootModificationTracker.getInstance(project),
            //VirtualFileManager.VFS_STRUCTURE_MODIFICATIONS,
            FileEditorManagerImpl.OPEN_FILE_SET_MODIFICATION_COUNT
          ),
          false
        )
      );
    }

    ConcurrentMap<SearchScope, Map<String, UniqueNameBuilder<VirtualFile>>> scope2ValueMap = (ConcurrentMap<SearchScope, Map<String, UniqueNameBuilder<VirtualFile>>>)data.getValue();
    Map<String, UniqueNameBuilder<VirtualFile>> valueMap = scope2ValueMap.get(scope);
    if (valueMap == null) {
      valueMap = Maps.cacheOrGet(scope2ValueMap, scope, ContainerUtil.createConcurrentSoftValueMap());
    }

    final String fileName = file.getName();
    UniqueNameBuilder<VirtualFile> uniqueNameBuilderForShortName = valueMap.get(fileName);

    if (uniqueNameBuilderForShortName == null) {
      final UniqueNameBuilder<VirtualFile> builder = filesWithTheSameName(fileName, project, skipNonOpenedFiles, scope);
      valueMap.put(fileName, builder != null ? builder : ourEmptyBuilder);
      uniqueNameBuilderForShortName = builder;
    }
    else if (uniqueNameBuilderForShortName == ourEmptyBuilder) {
      uniqueNameBuilderForShortName = null;
    }

    if (uniqueNameBuilderForShortName != null && uniqueNameBuilderForShortName.contains(file)) {
      return file instanceof VirtualFilePathWrapper virtualFilePathWrapper
        ? virtualFilePathWrapper.getPresentablePath()
        : uniqueNameBuilderForShortName.getShortPath(file);
    }
    return file.getName();
  }

  @Nullable
  private static UniqueNameBuilder<VirtualFile> filesWithTheSameName(
    String fileName,
    Project project,
    boolean skipNonOpenedFiles,
    SearchScope scope
  ) {
    Collection<VirtualFile> filesWithSameName =
      skipNonOpenedFiles ? Collections.emptySet() : FilenameIndex.getVirtualFilesByName(project, fileName, scope);
    Set<VirtualFile> setOfFilesWithTheSameName = new HashSet<>(filesWithSameName);
    // add open files out of project scope
    for (VirtualFile openFile : FileEditorManager.getInstance(project).getOpenFiles()) {
      if (openFile.getName().equals(fileName)) {
        setOfFilesWithTheSameName.add(openFile);
      }
    }
    if (!skipNonOpenedFiles) {
      for (VirtualFile recentlyEditedFile : EditorHistoryManagerImpl.getInstance(project).getFiles()) {
        if (recentlyEditedFile.getName().equals(fileName)) {
          setOfFilesWithTheSameName.add(recentlyEditedFile);
        }
      }
    }

    filesWithSameName = setOfFilesWithTheSameName;

    if (filesWithSameName.size() > 1) {
      String path = project.getBasePath();
      path = path == null ? "" : FileUtil.toSystemIndependentName(path);
      UniqueNameBuilder<VirtualFile> builder = new UniqueNameBuilder<>(path, File.separator, 25);
      for (VirtualFile virtualFile : filesWithSameName) {
        builder.addPath(virtualFile, virtualFile.getPath());
      }
      return builder;
    }
    return null;
  }
}
