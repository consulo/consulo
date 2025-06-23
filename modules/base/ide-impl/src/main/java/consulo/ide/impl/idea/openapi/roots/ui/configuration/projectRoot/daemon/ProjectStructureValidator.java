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
package consulo.ide.impl.idea.openapi.roots.ui.configuration.projectRoot.daemon;

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ExtensionAPI;
import consulo.component.extension.ExtensionPointName;
import consulo.content.library.Library;
import consulo.ide.impl.idea.openapi.roots.ui.configuration.libraries.LibraryEditingUtil;
import consulo.ide.impl.idea.openapi.roots.ui.configuration.projectRoot.ModuleStructureConfigurable;
import consulo.module.Module;
import consulo.module.ui.awt.ChooseModulesDialog;
import consulo.project.Project;
import consulo.project.localize.ProjectLocalize;
import consulo.ui.annotation.RequiredUIAccess;
import jakarta.annotation.Nullable;

import java.util.List;

/**
 * @author ksafonov
 */
@ExtensionAPI(ComponentScope.APPLICATION)
public abstract class ProjectStructureValidator {
    private static final ExtensionPointName<ProjectStructureValidator> EP_NAME = ExtensionPointName.create(ProjectStructureValidator.class);

    public static List<ProjectStructureElementUsage> getUsagesInElement(ProjectStructureElement element) {
        for (ProjectStructureValidator validator : EP_NAME.getExtensionList()) {
            List<ProjectStructureElementUsage> usages = validator.getUsagesIn(element);
            if (usages != null) {
                return usages;
            }
        }
        return element.getUsagesInElement();
    }

    public static void check(Project project, ProjectStructureElement element, ProjectStructureProblemsHolder problemsHolder) {
        for (ProjectStructureValidator validator : EP_NAME.getExtensionList()) {
            if (validator.checkElement(element, problemsHolder)) {
                return;
            }
        }
        element.check(project, problemsHolder);
    }

    @RequiredUIAccess
    public static void showDialogAndAddLibraryToDependencies(Library library, Project project, boolean allowEmptySelection) {
        for (ProjectStructureValidator validator : EP_NAME.getExtensionList()) {
            if (validator.addLibraryToDependencies(library, project, allowEmptySelection)) {
                return;
            }
        }

        List<Module> modules = LibraryEditingUtil.getSuitableModules(project, library.getKind(), library);
        if (modules.isEmpty()) {
            return;
        }
        ChooseModulesDialog dlg = new ChooseModulesDialog(
            project,
            modules,
            ProjectLocalize.chooseModulesDialogTitle().get(),
            ProjectLocalize.chooseModulesDialogDescription(library.getName()).get()
        );
        dlg.show();
        if (dlg.isOK()) {
            List<Module> chosenModules = dlg.getChosenElements();
            for (Module module : chosenModules) {
                ModuleStructureConfigurable.addLibraryOrderEntry(module, library);
            }
        }
    }

    /**
     * @return <code>true</code> if handled
     */
    protected boolean addLibraryToDependencies(Library library, Project project, boolean allowEmptySelection) {
        return false;
    }


    /**
     * @return <code>true</code> if it handled this element
     */
    protected boolean checkElement(ProjectStructureElement element, ProjectStructureProblemsHolder problemsHolder) {
        return false;
    }

    /**
     * @return list of usages or <code>null</code> when it does not handle such element
     */
    @Nullable
    protected List<ProjectStructureElementUsage> getUsagesIn(ProjectStructureElement element) {
        return null;
    }
}
