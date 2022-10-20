/*
 * Copyright 2013-2022 consulo.io
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
package consulo.project.macro;

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ServiceAPI;
import consulo.component.macro.PathMacroManager;
import consulo.project.Project;

/**
 * @author VISTALL
 * @since 20/10/2022
 */
@ServiceAPI(ComponentScope.PROJECT)
public interface ProjectPathMacroManager extends PathMacroManager {
  public static ProjectPathMacroManager getInstance(Project project) {
    return project.getInstance(ProjectPathMacroManager.class);
  }
}
