/*
 * Copyright 2000-2010 JetBrains s.r.o.
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
package consulo.ide.impl.idea.openapi.roots.ui.configuration.classpath;

import consulo.content.base.BinariesOrderRootType;
import consulo.content.library.Library;
import consulo.content.library.LibraryTable;
import consulo.content.library.LibraryTablePresentation;
import consulo.ide.impl.idea.openapi.roots.ui.configuration.libraries.LibraryEditingUtil;
import consulo.ide.impl.idea.openapi.roots.ui.configuration.libraries.LibraryPresentationManager;
import consulo.ide.impl.idea.openapi.roots.ui.configuration.libraryEditor.EditExistingLibraryDialogDescriptor;
import consulo.ide.setting.module.ClasspathPanel;
import consulo.ide.setting.module.ClasspathTableItem;
import consulo.ide.setting.module.LibrariesConfigurator;
import consulo.ide.setting.module.LibraryTableModifiableModelProvider;
import consulo.ide.setting.module.ModulesConfigurator;
import consulo.localize.LocalizeValue;
import consulo.module.content.layer.orderEntry.LibraryOrderEntry;
import consulo.module.impl.internal.layer.library.LibraryTableImplUtil;
import consulo.project.Project;
import consulo.project.localize.ProjectLocalize;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.util.lang.StringUtil;
import consulo.util.lang.xml.XmlStringUtil;

import java.util.List;
import java.util.concurrent.CompletableFuture;

public class LibraryClasspathTableItem<T extends LibraryOrderEntry> extends ClasspathTableItem<T> {
    private final Project myProject;
    private final LibrariesConfigurator myLibrariesConfigurator;

    public LibraryClasspathTableItem(
        T orderEntry,
        Project project,
        ModulesConfigurator modulesConfigurator,
        LibrariesConfigurator librariesConfigurator
    ) {
        super(orderEntry);
        myProject = project;
        myLibrariesConfigurator = librariesConfigurator;
    }

    @Override
    public boolean isEditable() {
        return myEntry.isValid();
    }

    @Override
    @RequiredUIAccess
    public CompletableFuture<?> doEdit(ClasspathPanel panel) {
        Library library = getEntry().getLibrary();
        if (library == null) {
            return CompletableFuture.completedFuture(null);
        }

        LibraryTable table = library.getTable();
        String tableLevel = table != null ? table.getTableLevel() : LibraryTableImplUtil.MODULE_LEVEL;
        LibraryTablePresentation presentation = LibraryEditingUtil.getLibraryTablePresentation(myProject, tableLevel);
        LibraryTableModifiableModelProvider provider = panel.getModifiableModelProvider(tableLevel);
        return EditExistingLibraryDialogDescriptor.showAsync(
            panel.getUIComponent(),
            provider,
            library,
            myProject,
            presentation,
            myLibrariesConfigurator,
            getEntry().getOwnerModule()
        );
    }

    @Override
    public LocalizeValue getTooltipText() {
        Library library = myEntry.getLibrary();
        if (library == null) {
            return LocalizeValue.empty();
        }

        String name = library.getName();
        if (name != null) {
            List<String> invalidUrls = library.getInvalidRootUrls(BinariesOrderRootType.ID);
            if (!invalidUrls.isEmpty()) {
                return ProjectLocalize.projectRootsTooltipLibraryHasBrokenPaths(name, invalidUrls.size());
            }
        }

        List<String> descriptions = LibraryPresentationManager.getInstance().getDescriptions(library, myLibrariesConfigurator);
        if (descriptions.isEmpty()) {
            return LocalizeValue.empty();
        }

        return LocalizeValue.of(XmlStringUtil.wrapInHtml(StringUtil.join(descriptions, "<br>")));
    }
}
