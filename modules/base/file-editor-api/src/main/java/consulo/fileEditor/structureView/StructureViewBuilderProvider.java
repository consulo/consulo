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

/*
 * @author max
 */
package consulo.fileEditor.structureView;

import consulo.annotation.access.RequiredReadAction;
import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ExtensionAPI;
import consulo.project.Project;
import consulo.virtualFileSystem.VirtualFile;
import consulo.virtualFileSystem.fileType.FileType;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

@ExtensionAPI(ComponentScope.APPLICATION)
public interface StructureViewBuilderProvider {
  /**
   * Returns the structure view builder for the specified file.
   * @param fileType file type of the file to provide structure for
   * @param file The file for which the structure view builder is requested.
   * @param project The project to which the file belongs.
   * @return The structure view builder, or null if no structure view is available for the file.
   */
  @Nullable
  @RequiredReadAction
  StructureViewBuilder getStructureViewBuilder(@Nonnull FileType fileType, @Nonnull VirtualFile file, @Nonnull Project project);
}