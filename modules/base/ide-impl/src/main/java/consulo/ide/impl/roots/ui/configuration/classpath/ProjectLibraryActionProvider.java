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
import consulo.content.library.Library;
import consulo.ide.impl.idea.openapi.roots.ui.configuration.libraries.LibraryPresentationManager;
import consulo.ide.setting.module.AddModuleDependencyActionProvider;
import consulo.ide.setting.module.ClasspathPanel;
import consulo.ide.setting.module.LibrariesConfigurator;
import consulo.ide.setting.module.ModulesConfigurator;
import consulo.localize.LocalizeValue;
import consulo.module.content.layer.ModuleRootLayer;
import consulo.platform.base.icon.PlatformIconGroup;
import consulo.project.localize.ProjectLocalize;
import consulo.ui.TextItemPresentation;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.dialog.ChooseItemsDialogDescriptor;
import consulo.ui.image.Image;
import consulo.util.lang.StringUtil;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * @author VISTALL
 * @since 27.09.14
 */
@ExtensionImpl
public class ProjectLibraryActionProvider implements AddModuleDependencyActionProvider<List<Library>, ProjectLibraryContext> {
    @Override
    public LocalizeValue getActionName(ModuleRootLayer layer) {
        return ProjectLocalize.classpathAddLibraryAction();
    }

    @Override
    public Image getIcon(ModuleRootLayer layer) {
        return PlatformIconGroup.nodesPplib();
    }

    @Override
    public ProjectLibraryContext createContext(
        ClasspathPanel classpathPanel,
        ModulesConfigurator modulesConfigurator,
        LibrariesConfigurator librariesConfigurator
    ) {
        return new ProjectLibraryContext(classpathPanel, modulesConfigurator, librariesConfigurator);
    }

    @RequiredUIAccess
    @Override
    public CompletableFuture<List<Library>> invoke(ProjectLibraryContext context) {
        List<Library> libraries = context.getItems();
        Map<Library, Image> icons = new HashMap<>();
        for (Library library : libraries) {
            Image customIcon = LibraryPresentationManager.getInstance().getCustomIcon(library, context.getLibrariesConfigurator());
            icons.put(library, customIcon != null ? customIcon : PlatformIconGroup.nodesPplib());
        }

        return ChooseItemsDialogDescriptor.showAsync(
            context.getClasspathPanel().getUIComponent(),
            ProjectLocalize.classpathChooserTitleAddLibraryDependency(),
            ProjectLocalize.classpathChooserDescriptionAddLibraryDependency(),
            libraries,
            (library, presentation) -> render(presentation, library, icons),
            true,
            List.of()
        );
    }

    private static void render(TextItemPresentation presentation, Library library, Map<Library, Image> icons) {
        presentation.withIcon(icons.get(library));
        presentation.append(StringUtil.notNullize(library.getName()));
    }
}
