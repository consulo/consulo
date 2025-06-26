/*
 * Copyright 2000-2009 JetBrains s.r.o.
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
package consulo.ide.impl.idea.openapi.roots.ui.configuration.actions;

import consulo.ide.newModule.NewOrImportModuleUtil;
import consulo.project.localize.ProjectLocalize;
import consulo.ui.ex.action.AnAction;
import consulo.ui.ex.action.AnActionEvent;
import consulo.fileChooser.FileChooserDescriptor;
import consulo.module.Module;
import consulo.module.ModuleManager;
import consulo.application.dumb.DumbAware;
import consulo.project.Project;
import consulo.virtualFileSystem.VirtualFile;
import consulo.ide.impl.newProject.ui.NewProjectDialog;
import consulo.ide.impl.newProject.ui.NewProjectPanel;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.fileChooser.FileChooser;
import consulo.util.concurrent.AsyncResult;

import jakarta.annotation.Nonnull;

/**
 * @author Eugene Zhuravlev
 * @since 2004-01-05
 */
public class NewModuleAction extends AnAction implements DumbAware {
    public NewModuleAction() {
        super(ProjectLocalize.moduleNewAction(), ProjectLocalize.moduleNewActionDescription(), null);
    }

    @Override
    @RequiredUIAccess
    public void actionPerformed(@Nonnull AnActionEvent e) {
        final Project project = e == null ? null : e.getData(Project.KEY);
        if (project == null) {
            return;
        }
        final VirtualFile virtualFile = e.getData(VirtualFile.KEY);

        final ModuleManager moduleManager = ModuleManager.getInstance(project);
        FileChooserDescriptor fileChooserDescriptor = new FileChooserDescriptor(false, true, false, false, false, false) {
            @Override
            @RequiredUIAccess
            public boolean isFileSelectable(VirtualFile file) {
                if (!super.isFileSelectable(file)) {
                    return false;
                }
                for (Module module : moduleManager.getModules()) {
                    VirtualFile moduleDir = module.getModuleDir();
                    if (moduleDir != null && moduleDir.equals(file)) {
                        return false;
                    }
                }
                return true;
            }
        };
        fileChooserDescriptor.withTitleValue(ProjectLocalize.chooseModuleHome());


        AsyncResult<VirtualFile> chooseAsync =
            FileChooser.chooseFile(fileChooserDescriptor, project, virtualFile != null && virtualFile.isDirectory() ? virtualFile : null);
        chooseAsync.doWhenDone(moduleDir -> {
            NewProjectDialog dialog = new NewProjectDialog(project, moduleDir);

            dialog.showAsync().doWhenDone(() -> {
                NewProjectPanel panel = dialog.getProjectPanel();
                NewOrImportModuleUtil.doCreate(panel.getProcessor(), panel.getWizardContext(), project, moduleDir);
            });
        });
    }

    @RequiredUIAccess
    @Override
    public void update(@Nonnull AnActionEvent e) {
        super.update(e);
        e.getPresentation().setEnabled(e.getData(Project.KEY) != null);
    }
}
