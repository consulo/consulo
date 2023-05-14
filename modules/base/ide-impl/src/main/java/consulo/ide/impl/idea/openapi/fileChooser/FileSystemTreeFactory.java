/*
 * Copyright 2000-2009 JetBrains s.r.o.
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
package consulo.ide.impl.idea.openapi.fileChooser;

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ServiceAPI;
import consulo.ui.ex.action.DefaultActionGroup;
import consulo.ide.ServiceManager;
import consulo.fileChooser.FileChooserDescriptor;
import consulo.project.Project;
import jakarta.annotation.Nonnull;

@ServiceAPI(ComponentScope.APPLICATION)
public abstract class FileSystemTreeFactory {
  @Nonnull
  public static FileSystemTreeFactory getInstance() {
    return ServiceManager.getService(FileSystemTreeFactory.class);
  }

  @Nonnull
  public abstract FileSystemTree createFileSystemTree(Project project, FileChooserDescriptor fileChooserDescriptor);

  @Nonnull
  public abstract DefaultActionGroup createDefaultFileSystemActions(FileSystemTree fileSystemTree);
}
