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
package consulo.ide.impl.idea.openapi.fileTypes;

import consulo.annotation.component.ExtensionImpl;
import consulo.application.ApplicationManager;
import consulo.externalService.statistic.AbstractApplicationUsagesCollector;
import consulo.externalService.statistic.CollectUsagesException;
import consulo.externalService.statistic.UsageDescriptor;
import consulo.ide.impl.idea.util.containers.ContainerUtil;
import consulo.language.file.FileTypeManager;
import consulo.language.psi.scope.GlobalSearchScope;
import consulo.language.psi.search.FileTypeIndex;
import consulo.language.psi.stub.FileBasedIndex;
import consulo.project.Project;
import consulo.virtualFileSystem.VirtualFile;
import consulo.virtualFileSystem.fileType.FileType;
import consulo.virtualFileSystem.fileType.UnknownFileType;

import jakarta.annotation.Nonnull;
import java.util.HashSet;
import java.util.Set;

/**
 * @author Nikolay Matveev
 */
@ExtensionImpl
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
    FileTypeManager fileTypeManager = FileTypeManager.getInstance();
    FileType[] registeredFileTypes = fileTypeManager.getRegisteredFileTypes();
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
    return ContainerUtil.map2Set(usedFileTypes, fileType -> new UsageDescriptor(fileType.getId(), 1));
  }
}
