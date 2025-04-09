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

import consulo.content.library.Library;
import consulo.ide.impl.idea.openapi.roots.ui.configuration.libraries.LibraryEditingUtil;
import consulo.ide.setting.module.*;
import consulo.module.content.layer.ModifiableModuleRootLayer;
import consulo.module.content.layer.orderEntry.DependencyScope;
import consulo.module.content.layer.orderEntry.LibraryDependencyScopeSuggester;
import consulo.module.content.layer.orderEntry.LibraryOrderEntry;
import consulo.module.content.layer.orderEntry.OrderEntry;
import consulo.project.localize.ProjectLocalize;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.awt.Messages;
import consulo.util.collection.ContainerUtil;
import consulo.util.lang.StringUtil;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Predicate;

/**
 * @author VISTALL
 * @since 2014-09-28
 */
public class ProjectLibraryContext extends AddModuleDependencyContext<List<Library>> {
    private List<Library> myItems;

    public ProjectLibraryContext(
        ClasspathPanel classpathPanel,
        ModulesConfigurator modulesConfigurator,
        LibrariesConfigurator librariesConfigurator
    ) {
        super(classpathPanel, modulesConfigurator, librariesConfigurator);

        LibraryTableModifiableModelProvider projectLibrariesProvider = librariesConfigurator.getProjectLibrariesProvider();
        Library[] libraries = projectLibrariesProvider.getModifiableModel().getLibraries();
        Predicate<Library> condition = LibraryEditingUtil.getNotAddedLibrariesCondition(myClasspathPanel.getRootModel());

        myItems = ContainerUtil.filter(libraries, condition);
        ContainerUtil.sort(myItems, (o1, o2) -> StringUtil.compare(o1.getName(), o2.getName(), false));
    }

    @Nonnull
    @Override
    @RequiredUIAccess
    public List<OrderEntry> createOrderEntries(@Nonnull ModifiableModuleRootLayer layer, @Nonnull List<Library> value) {
        List<OrderEntry> entries = new ArrayList<>();

        for (Library item : value) {
            for (OrderEntry orderEntry : layer.getOrderEntries()) {
                if (orderEntry instanceof LibraryOrderEntry libraryOrderEntry) {
                    if (item.equals(libraryOrderEntry.getLibrary())) {
                        continue; // already added
                    }

                    String name = item.getName();
                    if (name != null && name.equals(libraryOrderEntry.getLibraryName())) {
                        if (orderEntry.isValid()) {
                            Messages.showErrorDialog(
                                ProjectLocalize.classpathMessageLibraryAlreadyAdded(item.getName()).get(),
                                ProjectLocalize.classpathTitleAddingDependency().get()
                            );
                            return Collections.emptyList();
                        }
                        else {
                            layer.removeOrderEntry(orderEntry);
                        }
                    }
                }
            }
            LibraryOrderEntry orderEntry = layer.addLibraryEntry(item);
            DependencyScope defaultScope = getDefaultScope(item);
            if (defaultScope != null) {
                orderEntry.setScope(defaultScope);
            }
            entries.add(orderEntry);
        }

        return entries;
    }

    @Nullable
    private static DependencyScope getDefaultScope(Library item) {
        for (LibraryDependencyScopeSuggester suggester : LibraryDependencyScopeSuggester.EP_NAME.getExtensionList()) {
            DependencyScope scope = suggester.getDefaultDependencyScope(item);
            if (scope != null) {
                return scope;
            }
        }
        return null;
    }

    @Override
    public boolean isEmpty() {
        return myItems.isEmpty();
    }

    public List<Library> getItems() {
        return myItems;
    }
}
