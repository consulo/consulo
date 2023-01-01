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
import consulo.language.editor.CommonDataKeys;
import consulo.dataContext.DataProvider;
import consulo.language.editor.PlatformDataKeys;
import consulo.dataContext.GetDataRule;
import consulo.project.Project;
import consulo.util.dataholder.Key;
import consulo.virtualFileSystem.VirtualFile;
import javax.annotation.Nonnull;

/**
 * @author Eugene Zhuravlev
 *         Date: Feb 10, 2004
 */
@ExtensionImpl
public class ProjectFileDirectoryRule implements GetDataRule<VirtualFile> {
  @Nonnull
  @Override
  public Key<VirtualFile> getKey() {
    return PlatformDataKeys.PROJECT_FILE_DIRECTORY;
  }

  @Override
  public VirtualFile getData(@Nonnull DataProvider dataProvider) {
    VirtualFile dir = dataProvider.getDataUnchecked(PlatformDataKeys.PROJECT_FILE_DIRECTORY);
    if (dir == null) {
      final Project project = dataProvider.getDataUnchecked(CommonDataKeys.PROJECT);
      if (project != null) {
        dir = project.getBaseDir();
      }
    }
    return dir;
  }
}
