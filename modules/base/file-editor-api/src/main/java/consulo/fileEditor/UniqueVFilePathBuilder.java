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
package consulo.fileEditor;

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ServiceAPI;
import consulo.application.Application;
import consulo.content.scope.SearchScope;
import consulo.project.Project;
import consulo.virtualFileSystem.VirtualFile;

/**
 * @author yole
 */
@ServiceAPI(ComponentScope.APPLICATION)
public abstract class UniqueVFilePathBuilder {
  private static final UniqueVFilePathBuilder DUMMY_BUILDER = new UniqueVFilePathBuilder() {
    
    @Override
    public String getUniqueVirtualFilePath(Project project, VirtualFile vFile) {
      return vFile.getPresentableName();
    }

    
    @Override
    public String getUniqueVirtualFilePathWithinOpenedFileEditors(Project project, VirtualFile vFile) {
      return vFile.getPresentableName();
    }
  };

  public static UniqueVFilePathBuilder getInstance() {
    return Application.get().getInstance(UniqueVFilePathBuilder.class);
  }

  
  public String getUniqueVirtualFilePath(Project project, VirtualFile vFile, SearchScope scope) {
    return getUniqueVirtualFilePath(project, vFile);
  }

  public abstract String getUniqueVirtualFilePath(Project project, VirtualFile vFile);

  
  public abstract String getUniqueVirtualFilePathWithinOpenedFileEditors(Project project, VirtualFile vFile);
}
