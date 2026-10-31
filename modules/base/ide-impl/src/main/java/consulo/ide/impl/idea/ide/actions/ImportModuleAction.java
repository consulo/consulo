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
import consulo.application.Application;
import consulo.application.concurrent.coroutine.ReadLock;
import consulo.application.concurrent.coroutine.WriteLock;
import consulo.application.progress.ProgressBuilderFactory;
import consulo.fileChooser.FileChooserDescriptor;
import consulo.ide.impl.module.importing.ModuleImportProcessor;
import consulo.ide.impl.module.importing.ModuleImportProviders;
import consulo.module.ModifiableModuleModel;
import consulo.module.creation.importing.ModuleImportContext;
import consulo.module.creation.importing.ModuleImportProvider;
import consulo.module.ModuleManager;
import consulo.platform.base.localize.ActionLocalize;
import consulo.project.Project;
import consulo.ui.UIAccess;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.action.AnActionEvent;
import consulo.ui.ex.action.LegacyAnAction;
import consulo.util.concurrent.coroutine.Coroutine;
import consulo.util.concurrent.coroutine.step.CallSubroutine;
import consulo.util.dataholder.Key;
import consulo.util.lang.Pair;
import org.jspecify.annotations.Nullable;

import java.util.concurrent.CompletableFuture;

/**
 * @author Dmitry Avdeev
 * @since 2012-10-31
 */
@ActionImpl(id = "ImportModule")
public class ImportModuleAction extends LegacyAnAction {
    private static final Key<ModifiableModuleModel> MODULE_MODEL = Key.create("ImportModuleAction.moduleModel");

    public ImportModuleAction() {
        super(ActionLocalize.actionImportmoduleText());
    }

    @Override
    @RequiredUIAccess
    public void actionPerformed(AnActionEvent e) {
        Project project = e.getRequiredData(Project.KEY);
        executeImportAction(project, null);
    }

    @RequiredUIAccess
    public static void executeImportAction(Project project, @Nullable FileChooserDescriptor descriptor) {
        UIAccess uiAccess = UIAccess.current();
        CompletableFuture<Pair<ModuleImportContext, ModuleImportProvider<ModuleImportContext>>> chooser =
            ModuleImportProcessor.showFileChooser(project, descriptor);

        chooser.whenComplete((pair, error) -> {
            if (error != null) {
                return;
            }

            ModuleImportContext context = pair.getFirst();
            ModuleImportProvider<ModuleImportContext> provider = pair.getSecond();

            Application.get().getInstance(ProgressBuilderFactory.class)
                .newProgressBuilder(project, provider.getName())
                .execute(uiAccess, () -> Coroutine
                    // the model is created under a read action, the provider runs as a subroutine
                    .first(ReadLock.<Object, Object>apply((o, c) -> {
                        c.putCopyableUserData(MODULE_MODEL, ModuleManager.getInstance(project).getModifiableModel());
                        return o;
                    }))
                    .then(CallSubroutine.<Object, Object>call(c ->
                        provider.process(context, project, c.getCopyableUserData(MODULE_MODEL), module -> {
                        })))
                    // and the model is committed under a write action, not on the UI thread
                    .then(WriteLock.<Object, Object>apply((o, c) -> {
                        c.getCopyableUserData(MODULE_MODEL).commit();
                        return o;
                    })))
                .whenComplete((result, importError) -> context.dispose());
        });
    }

    @Override
    public void update(AnActionEvent e) {
        e.getPresentation().setEnabledAndVisible(e.hasData(Project.KEY) && !ModuleImportProviders.getExtensions(true).isEmpty());
    }

    @Override
    public boolean isDumbAware() {
        return true;
    }
}
