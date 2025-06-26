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
package consulo.ide.impl.idea.ide.actions;

import consulo.annotation.component.ActionImpl;
import consulo.application.WriteAction;
import consulo.fileChooser.FileChooserDescriptor;
import consulo.ide.moduleImport.ModuleImportContext;
import consulo.ide.moduleImport.ModuleImportProcessor;
import consulo.ide.moduleImport.ModuleImportProvider;
import consulo.ide.moduleImport.ModuleImportProviders;
import consulo.module.ModifiableModuleModel;
import consulo.module.ModuleManager;
import consulo.platform.base.localize.ActionLocalize;
import consulo.project.Project;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.action.AnAction;
import consulo.ui.ex.action.AnActionEvent;
import consulo.ui.ex.action.Presentation;
import consulo.util.concurrent.AsyncResult;
import consulo.util.lang.Pair;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

/**
 * @author Dmitry Avdeev
 * @since 2012-10-31
 */
@ActionImpl(id = "ImportModule")
public class ImportModuleAction extends AnAction {
    public ImportModuleAction() {
        super(ActionLocalize.actionImportmoduleText());
    }

    @Override
    @RequiredUIAccess
    public void actionPerformed(@Nonnull AnActionEvent e) {
        Project project = e.getData(Project.KEY);
        assert project != null;
        executeImportAction(project, null);
    }

    @RequiredUIAccess
    public static void executeImportAction(@Nonnull Project project, @Nullable FileChooserDescriptor descriptor) {
        AsyncResult<Pair<ModuleImportContext, ModuleImportProvider<ModuleImportContext>>> chooser =
            ModuleImportProcessor.showFileChooser(project, descriptor);

        chooser.doWhenDone(pair -> {
            ModuleImportContext context = pair.getFirst();

            ModuleImportProvider<ModuleImportContext> provider = pair.getSecond();

            ModifiableModuleModel modifiableModel = ModuleManager.getInstance(project).getModifiableModel();
            provider.process(
                context,
                project,
                modifiableModel,
                module -> {
                }
            );
            WriteAction.runAndWait(modifiableModel::commit);
        });
    }

    @Override
    @RequiredUIAccess
    public void update(@Nonnull AnActionEvent e) {
        Presentation presentation = e.getPresentation();

        if (e.getData(Project.KEY) == null) {
            presentation.setEnabledAndVisible(false);
            return;
        }

        presentation.setEnabledAndVisible(!ModuleImportProviders.getExtensions(true).isEmpty());
    }

    @Override
    public boolean isDumbAware() {
        return true;
    }
}
