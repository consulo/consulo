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
package consulo.project.internal;

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ServiceAPI;
import consulo.application.Application;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import java.io.File;
import java.util.List;

/**
 * @author VISTALL
 * @since 31-Jan-17
 */
@ServiceAPI(ComponentScope.APPLICATION)
public interface ProjectOpenProcessors {

  @Nonnull
  public static ProjectOpenProcessors getInstance() {
    return Application.get().getInstance(ProjectOpenProcessors.class);
  }

  @Nonnull
  List<ProjectOpenProcessor> getProcessors();

  @Nullable
  default ProjectOpenProcessor findProcessor(@Nonnull File file) {
    for (ProjectOpenProcessor provider : getProcessors()) {
      if (provider.canOpenProject(file)) {
        return provider;
      }
    }
    return null;
  }
}
