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
package com.intellij.psi.search;

import com.intellij.openapi.fileTypes.FileType;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.util.Processor;
import com.intellij.util.indexing.IdFilter;
import javax.annotation.Nonnull;

import javax.annotation.Nullable;
import java.util.Collection;

interface FileNameIndexService {
  @Nonnull
  Collection<VirtualFile> getVirtualFilesByName(Project project, @Nonnull String name, @Nonnull GlobalSearchScope scope, @Nullable IdFilter idFilter);

  void processAllFileNames(@Nonnull Processor<? super String> processor, @Nonnull GlobalSearchScope scope, @Nullable IdFilter filter);

  @Nonnull
  Collection<VirtualFile> getFilesWithFileType(@Nonnull FileType type, @Nonnull GlobalSearchScope scope);

  boolean processFilesWithFileType(@Nonnull FileType type, @Nonnull Processor<? super VirtualFile> processor, @Nonnull GlobalSearchScope scope);
}
