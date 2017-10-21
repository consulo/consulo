/*
 * Copyright 2013-2017 consulo.io
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
package consulo.project;

import com.intellij.openapi.components.ServiceManager;
import com.intellij.projectImport.ProjectOpenProcessor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;

/**
 * @author VISTALL
 * @since 31-Jan-17
 */
public interface ProjectOpenProcessors {

  @NotNull
  public static ProjectOpenProcessors getInstance() {
    return ServiceManager.getService(ProjectOpenProcessors.class);
  }

  @NotNull
  public ProjectOpenProcessor[] getProcessors();

  @Nullable
  default ProjectOpenProcessor findProcessor(@NotNull File file) {
    for (ProjectOpenProcessor provider : getProcessors()) {
      if (provider.canOpenProject(file)) {
        return provider;
      }
    }
    return null;
  }
}
