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

package consulo.ide.impl.idea.ide.util.gotoByName;

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.Service;
import consulo.annotation.component.ServiceImpl;
import consulo.component.persist.State;
import consulo.component.persist.Storage;
import consulo.component.persist.StoragePathMacros;
import consulo.ide.ServiceManager;
import consulo.project.Project;
import consulo.virtualFileSystem.fileType.FileType;
import jakarta.inject.Singleton;

/**
 * Configuration for file type filtering popup in "Go to | File" action.
 *
 * @author Constantine.Plotnikov
 */
@Singleton
@State(name = "GotoFileConfiguration", storages = {@Storage(file = StoragePathMacros.WORKSPACE_FILE)})
@Service(ComponentScope.PROJECT)
@ServiceImpl
public class GotoFileConfiguration extends ChooseByNameFilterConfiguration<FileType> {
  /**
   * Get configuration instance
   *
   * @param project a project instance
   * @return a configuration instance
   */
  public static GotoFileConfiguration getInstance(Project project) {
    return ServiceManager.getService(project, GotoFileConfiguration.class);
  }

  @Override
  protected String nameForElement(FileType type) {
    return type.getId();
  }
}
