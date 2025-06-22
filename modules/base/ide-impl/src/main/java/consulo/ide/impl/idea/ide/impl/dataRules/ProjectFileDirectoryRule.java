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
package consulo.ide.impl.idea.ide.impl.dataRules;

import consulo.annotation.component.ExtensionImpl;
import consulo.dataContext.DataProvider;
import consulo.dataContext.GetDataRule;
import consulo.project.Project;
import consulo.util.dataholder.Key;
import consulo.virtualFileSystem.VirtualFile;
import jakarta.annotation.Nonnull;

/**
 * @author Eugene Zhuravlev
 * @since 2004-02-10
 */
@ExtensionImpl
public class ProjectFileDirectoryRule implements GetDataRule<VirtualFile> {
  @Nonnull
  @Override
  public Key<VirtualFile> getKey() {
    return Project.PROJECT_FILE_DIRECTORY;
  }

  @Override
  public VirtualFile getData(@Nonnull DataProvider dataProvider) {
    VirtualFile dir = dataProvider.getDataUnchecked(Project.PROJECT_FILE_DIRECTORY);
    if (dir == null) {
      final Project project = dataProvider.getDataUnchecked(Project.KEY);
      if (project != null) {
        dir = project.getBaseDir();
      }
    }
    return dir;
  }
}
