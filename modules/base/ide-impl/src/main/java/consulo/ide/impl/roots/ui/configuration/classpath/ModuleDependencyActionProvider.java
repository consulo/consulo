/*
 * Copyright 2013-2016 consulo.io
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
package consulo.ide.impl.roots.ui.configuration.classpath;

import consulo.annotation.component.ExtensionImpl;
import consulo.application.AllIcons;
import consulo.ide.setting.module.AddModuleDependencyActionProvider;
import consulo.module.Module;
import consulo.module.ui.awt.ChooseModulesDialog;
import consulo.ide.setting.module.ClasspathPanel;
import consulo.localize.LocalizeValue;
import consulo.module.content.layer.ModuleRootLayer;
import consulo.ide.setting.module.LibrariesConfigurator;
import consulo.ide.setting.module.ModulesConfigurator;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.image.Image;
import consulo.util.concurrent.AsyncResult;

import jakarta.annotation.Nonnull;
import java.util.List;

/**
 * @author VISTALL
 * @since 27.09.14
 */
@ExtensionImpl
public class ModuleDependencyActionProvider implements AddModuleDependencyActionProvider<List<Module>, ModuleDependencyContext> {
  @Nonnull
  @Override
  public LocalizeValue getActionName(@Nonnull ModuleRootLayer layer) {
    return LocalizeValue.localizeTODO("Module");
  }

  @Nonnull
  @Override
  public Image getIcon(@Nonnull ModuleRootLayer layer) {
    return AllIcons.Nodes.Module;
  }

  @Override
  public ModuleDependencyContext createContext(@Nonnull ClasspathPanel classpathPanel, @Nonnull ModulesConfigurator modulesConfigurator, @Nonnull LibrariesConfigurator librariesConfigurator) {
    return new ModuleDependencyContext(classpathPanel, modulesConfigurator, librariesConfigurator);
  }

  @RequiredUIAccess
  @Nonnull
  @Override
  public AsyncResult<List<Module>> invoke(@Nonnull ModuleDependencyContext context) {
    return new ChooseModulesDialog(context.getProject(), context.getNotAddedModules(), "Add Module Dependency", "Select modules for adding as dependency").showAsync2();
  }
}
