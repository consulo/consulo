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

import consulo.content.library.Library;
import consulo.ide.impl.idea.openapi.roots.ui.configuration.libraries.LibraryEditingUtil;
import consulo.project.Project;
import consulo.project.localize.ProjectLocalize;
import consulo.ui.UIAction;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.action.AnActionEvent;
import consulo.ui.ex.action.AnActionWithAsyncUpdate;
import consulo.ui.ex.action.DumbAwareAction;
import consulo.util.concurrent.coroutine.Coroutine;

/**
 * @author nik
 */
public class AddLibraryToModuleDependenciesAction extends DumbAwareAction implements AnActionWithAsyncUpdate {
    private final Project myProject;
    private final BaseLibrariesConfigurable myConfigurable;

    public AddLibraryToModuleDependenciesAction(Project project, BaseLibrariesConfigurable configurable) {
        super(ProjectLocalize.libraryAddToModulesAction(), ProjectLocalize.libraryAddToModulesDescription());
        myProject = project;
        myConfigurable = configurable;
    }

    @Override
    public Coroutine<?, ?> updateAsync(AnActionEvent e) {
        return Coroutine.first(UIAction.<Object, Object>apply(input -> {
            boolean visible = myConfigurable.getSelectedValues().size() == 1
                && myConfigurable.getSelectedValue() instanceof Library library
                && !LibraryEditingUtil.getSuitableModules(myProject, library.getKind(), library).isEmpty();
            e.getPresentation().setVisible(visible);
            return input;
        }));
    }

    @RequiredUIAccess
    @Override
    public void actionPerformed(AnActionEvent e) {
        if (myConfigurable.getSelectedValue() instanceof Library library) {
            LibraryEditingUtil.showDialogAndAddLibraryToDependencies(myConfigurable.getTree(), library, myProject);
        }
    }
}
