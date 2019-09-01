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
 * User: anna
 * Date: 10-Jul-2007
 */
package com.intellij.ide.util.projectWizard;

import com.intellij.openapi.module.ModifiableModuleModel;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.ui.configuration.DefaultModulesProvider;
import com.intellij.openapi.roots.ui.configuration.ModulesProvider;

import javax.annotation.Nullable;
import java.util.List;

@Deprecated
public abstract class ProjectBuilder {
  public boolean isUpdate() {
    return false;
  }

  @Nullable
  public abstract List<Module> commit(final Project project, @javax.annotation.Nullable final ModifiableModuleModel model, final ModulesProvider modulesProvider);

  public List<Module> commit(@Nullable Project project) {
    return commit(project, null, DefaultModulesProvider.createForProject(project));
  }

  public boolean validate(Project current, Project dest) {
    return true;
  }
  public void cleanup() {}

}