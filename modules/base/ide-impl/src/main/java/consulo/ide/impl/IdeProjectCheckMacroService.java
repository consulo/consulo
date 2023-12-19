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
package consulo.ide.impl;

import consulo.annotation.component.ServiceImpl;
import consulo.ide.impl.idea.openapi.project.impl.ProjectMacrosUtil;
import consulo.project.Project;
import consulo.project.impl.internal.ProjectCheckMacroService;
import jakarta.inject.Singleton;

import java.util.Set;

/**
 * @author VISTALL
 * @since 18-Jul-22
 */
@Singleton
@ServiceImpl
public class IdeProjectCheckMacroService implements ProjectCheckMacroService {
  @Override
  public boolean checkMacros(Project project, Set<String> usedMacros) {
    return ProjectMacrosUtil.checkMacros(project, usedMacros);
  }
}
