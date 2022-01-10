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
package com.intellij.openapi.fileTypes;

import com.intellij.internal.statistic.AbstractApplicationUsagesCollector;
import com.intellij.internal.statistic.CollectUsagesException;
import com.intellij.internal.statistic.beans.UsageDescriptor;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.search.FileTypeIndex;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.util.NotNullFunction;
import com.intellij.util.containers.ContainerUtil;
import com.intellij.util.indexing.FileBasedIndex;

import javax.annotation.Nonnull;
import java.util.HashSet;
import java.util.Set;

/**
 * @author Nikolay Matveev
 */
public class FileTypeUsagesCollector extends AbstractApplicationUsagesCollector {
  @Nonnull
  @Override
  public String getGroupId() {
    return "consulo.platform.base:file.type";
  }

  @Nonnull
  @Override
  public Set<UsageDescriptor> getProjectUsages(@Nonnull final Project project) throws CollectUsagesException {
    final Set<FileType> usedFileTypes = new HashSet<FileType>();
    final FileTypeManager fileTypeManager = FileTypeManager.getInstance();
    final FileType[] registeredFileTypes = fileTypeManager.getRegisteredFileTypes();
    for (final FileType fileType : registeredFileTypes) {
      if (project.isDisposed()) {
        throw new CollectUsagesException("Project is disposed");
      }
      ApplicationManager.getApplication().runReadAction(new Runnable() {
        @Override
        public void run() {
          FileBasedIndex.getInstance().processValues(
            FileTypeIndex.NAME,
            fileType,
            null,
            new FileBasedIndex.ValueProcessor<Void>() {
              @Override
              public boolean process(VirtualFile file, Void value) {
                usedFileTypes.add(fileType);
                return false;
              }
            }, GlobalSearchScope.projectScope(project));
        }
      });
    }
    usedFileTypes.add(UnknownFileType.INSTANCE);
    return ContainerUtil.map2Set(usedFileTypes, new NotNullFunction<FileType, UsageDescriptor>() {
      @Nonnull
      @Override
      public UsageDescriptor fun(FileType fileType) {
        return new UsageDescriptor(fileType.getId(), 1);
      }
    });
  }
}
