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
import consulo.ide.setting.module.AddModuleDependencyActionProvider;
import consulo.ide.setting.module.ClasspathPanel;
import consulo.ide.setting.module.LibrariesConfigurator;
import consulo.ide.setting.module.ModulesConfigurator;
import consulo.localize.LocalizeValue;
import consulo.module.Module;
import consulo.module.content.layer.ModuleRootLayer;
import consulo.platform.base.icon.PlatformIconGroup;
import consulo.project.localize.ProjectLocalize;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.dialog.ChooseItemsDialogDescriptor;
import consulo.ui.image.Image;

import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * @author VISTALL
 * @since 27.09.14
 */
@ExtensionImpl
public class ModuleDependencyActionProvider implements AddModuleDependencyActionProvider<List<Module>, ModuleDependencyContext> {
    @Override
    public LocalizeValue getActionName(ModuleRootLayer layer) {
        return ProjectLocalize.classpathAddModuleDependencyAction();
    }

    @Override
    public Image getIcon(ModuleRootLayer layer) {
        return PlatformIconGroup.nodesModule();
    }

    @Override
    public ModuleDependencyContext createContext(
        ClasspathPanel classpathPanel,
        ModulesConfigurator modulesConfigurator,
        LibrariesConfigurator librariesConfigurator
    ) {
        return new ModuleDependencyContext(classpathPanel, modulesConfigurator, librariesConfigurator);
    }

    @RequiredUIAccess
    @Override
    public CompletableFuture<List<Module>> invoke(ModuleDependencyContext context) {
        return ChooseItemsDialogDescriptor.showAsync(
            context.getClasspathPanel().getUIComponent(),
            ProjectLocalize.classpathChooserTitleAddModuleDependency(),
            ProjectLocalize.classpathChooserDescriptionAddModuleDependency(),
            context.getNotAddedModules(),
            (module, presentation) -> {
                presentation.withIcon(PlatformIconGroup.nodesModule());
                presentation.append(module.getName());
            },
            true,
            List.of()
        );
    }
}
