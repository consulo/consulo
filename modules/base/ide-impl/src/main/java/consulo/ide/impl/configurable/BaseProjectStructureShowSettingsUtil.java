/*
 * Copyright 2013-2021 consulo.io
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
package consulo.ide.impl.configurable;

import consulo.ide.impl.idea.openapi.roots.ui.configuration.ModulesConfiguratorImpl;
import consulo.ide.impl.base.BaseShowSettingsUtil;
import consulo.ide.setting.module.LibrariesConfigurator;
import consulo.ide.setting.module.ModulesConfigurator;
import consulo.project.Project;
import consulo.ide.impl.roots.ui.configuration.impl.DefaultLibrariesConfigurator;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

/**
 * @author VISTALL
 * @since 19/04/2021
 */
public abstract class BaseProjectStructureShowSettingsUtil extends BaseShowSettingsUtil {
  private ModulesConfiguratorImpl myModulesConfigurator;
  private LibrariesConfigurator myLibrariesConfigurator;

  protected void clearCaches() {
    // TODO [VISTALL] can be bug with multiple project dialogs
    if(myModulesConfigurator != null) {
      myModulesConfigurator.disposeWithTree();
      myModulesConfigurator = null;
    }

    if(myLibrariesConfigurator != null) {
      myLibrariesConfigurator.disposeWithTree();
      myLibrariesConfigurator = null;
    }
  }

  @Nullable
  @Override
  public ModulesConfigurator getModulesModel(@Nonnull Project project) {
    if(myModulesConfigurator == null) {
      myModulesConfigurator = new ModulesConfiguratorImpl(project, () -> getLibrariesModel(project));
      myModulesConfigurator.reset();
    }
    return myModulesConfigurator;
  }

  @Nullable
  @Override
  public LibrariesConfigurator getLibrariesModel(@Nonnull Project project) {
    if(myLibrariesConfigurator == null) {
      myLibrariesConfigurator = new DefaultLibrariesConfigurator(project);
      myLibrariesConfigurator.reset();
    }
    return myLibrariesConfigurator;
  }
}
