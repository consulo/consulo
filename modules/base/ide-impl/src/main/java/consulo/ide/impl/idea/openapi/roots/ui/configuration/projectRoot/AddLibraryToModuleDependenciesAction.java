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
package consulo.ide.impl.idea.openapi.roots.ui.configuration.projectRoot;

import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.action.AnActionEvent;
import consulo.ui.ex.action.DumbAwareAction;
import consulo.project.Project;
import consulo.content.internal.LibraryEx;
import consulo.content.library.Library;
import consulo.ide.impl.idea.openapi.roots.ui.configuration.libraries.LibraryEditingUtil;
import consulo.ide.impl.idea.openapi.roots.ui.configuration.projectRoot.daemon.LibraryProjectStructureElement;
import consulo.ide.impl.idea.openapi.roots.ui.configuration.projectRoot.daemon.ProjectStructureElement;
import jakarta.annotation.Nonnull;

/**
 * @author nik
 */
public class AddLibraryToModuleDependenciesAction extends DumbAwareAction {
    @Nonnull
    private final Project myProject;
    @Nonnull
    private final BaseLibrariesConfigurable myConfigurable;

    public AddLibraryToModuleDependenciesAction(@Nonnull Project project, @Nonnull BaseLibrariesConfigurable configurable) {
        super("Add to Modules...", "Add the library to the dependencies list of chosen modules", null);
        myProject = project;
        myConfigurable = configurable;
    }

    @Override
    public void update(@Nonnull AnActionEvent e) {
        ProjectStructureElement element = myConfigurable.getSelectedElement();
        boolean visible = false;
        if (element instanceof LibraryProjectStructureElement) {
            LibraryEx library = (LibraryEx) ((LibraryProjectStructureElement) element).getLibrary();
            visible = !LibraryEditingUtil.getSuitableModules(myProject, library.getKind(), library).isEmpty();
        }
        e.getPresentation().setVisible(visible);
    }

    @Override
    @RequiredUIAccess
    public void actionPerformed(@Nonnull AnActionEvent e) {
        LibraryProjectStructureElement element = (LibraryProjectStructureElement) myConfigurable.getSelectedElement();
        if (element == null) {
            return;
        }
        Library library = element.getLibrary();
        LibraryEditingUtil.showDialogAndAddLibraryToDependencies(library, myProject, false);
    }
}
