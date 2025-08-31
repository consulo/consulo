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

package consulo.module.content.internal;

import consulo.module.Module;
import consulo.module.ModuleManager;
import consulo.module.content.layer.ModulesProvider;
import consulo.project.Project;
import consulo.module.content.ModuleRootManager;
import consulo.module.content.layer.ModuleRootModel;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

/**
 * @author nik
 */
public class DefaultModulesProvider implements ModulesProvider {
  @Nonnull
  public static ModulesProvider of(@Nullable Project project) {
    return project == null ? EMPTY_MODULES_PROVIDER : new DefaultModulesProvider(project);
  }

  private final Project myProject;

  public DefaultModulesProvider(Project project) {
    myProject = project;
  }

  @Override
  @Nonnull
  public Module[] getModules() {
    return ModuleManager.getInstance(myProject).getModules();
  }

  @Override
  public Module getModule(String name) {
    return ModuleManager.getInstance(myProject).findModuleByName(name);
  }

  @Override
  public ModuleRootModel getRootModel(@Nonnull Module module) {
    return ModuleRootManager.getInstance(module);
  }
}
