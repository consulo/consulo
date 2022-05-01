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
package consulo.roots.ui.configuration.classpath;

import consulo.application.Application;
import consulo.ide.impl.idea.openapi.roots.ui.configuration.classpath.ClasspathPanel;
import consulo.component.extension.ExtensionType;
import consulo.component.extension.ExtensionList;
import consulo.localize.LocalizeValue;
import consulo.module.content.layer.ModuleRootLayer;
import consulo.ide.setting.module.LibrariesConfigurator;
import consulo.ide.setting.module.ModulesConfigurator;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.image.Image;
import consulo.util.concurrent.AsyncResult;

import javax.annotation.Nonnull;

/**
 * @author VISTALL
 * @since 27.09.14
 */
@ExtensionType(value = "addModuleDependencyActionProvider", component = Application.class)
public interface AddModuleDependencyActionProvider<T, C extends AddModuleDependencyContext<T>> {
  ExtensionList<AddModuleDependencyActionProvider, Application> EP = ExtensionList.of(AddModuleDependencyActionProvider.class);

  C createContext(@Nonnull ClasspathPanel classpathPanel, @Nonnull ModulesConfigurator modulesConfigurator, @Nonnull LibrariesConfigurator librariesConfigurator);

  default boolean isAvailable(@Nonnull C context) {
    return !context.isEmpty();
  }

  @Nonnull
  LocalizeValue getActionName(@Nonnull ModuleRootLayer layer);

  @Nonnull
  Image getIcon(@Nonnull ModuleRootLayer layer);

  @Nonnull
  @RequiredUIAccess
  AsyncResult<T> invoke(@Nonnull C context);
}
