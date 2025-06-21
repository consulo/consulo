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
package consulo.ide.impl.idea.ide.projectView.actions;

import consulo.configurable.Settings;
import consulo.dataContext.DataContext;
import consulo.ide.impl.idea.openapi.roots.ui.configuration.projectRoot.ModuleStructureConfigurable;
import consulo.ide.localize.IdeLocalize;
import consulo.language.editor.LangDataKeys;
import consulo.localize.LocalizeValue;
import consulo.module.ModifiableModuleModel;
import consulo.module.Module;
import consulo.module.impl.internal.ModuleManagerImpl;
import consulo.project.Project;
import consulo.project.ui.view.ProjectView;
import consulo.project.ui.view.ProjectViewPane;
import consulo.project.ui.view.tree.ModuleGroup;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.action.AnAction;
import consulo.ui.ex.action.AnActionEvent;
import consulo.ui.ex.action.Presentation;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

/**
 * @author cdr
 */
public class MoveModulesToGroupAction extends AnAction {
    protected final ModuleGroup myModuleGroup;

    public MoveModulesToGroupAction(@Nullable ModuleGroup moduleGroup, @Nonnull LocalizeValue title, @Nonnull LocalizeValue description) {
        super(title, description);
        myModuleGroup = moduleGroup;
    }

    @Override
    @RequiredUIAccess
    public void update(AnActionEvent e) {
        Presentation presentation = getTemplatePresentation();
        DataContext dataContext = e.getDataContext();
        Module[] modules = dataContext.getData(LangDataKeys.MODULE_CONTEXT_ARRAY);

        LocalizeValue description = IdeLocalize.messageMoveModulesToGroup(whatToMove(modules), myModuleGroup.presentableText());
        presentation.setDescriptionValue(description);
    }

    protected static String whatToMove(Module[] modules) {
        return modules.length == 1 ? IdeLocalize.messageModule(modules[0].getName()).get() : IdeLocalize.messageModules().get();
    }

    @Override
    @RequiredUIAccess
    public void actionPerformed(AnActionEvent e) {
        DataContext dataContext = e.getDataContext();
        Module[] modules = dataContext.getData(LangDataKeys.MODULE_CONTEXT_ARRAY);
        doMove(modules, myModuleGroup, dataContext);
    }

    public static void doMove(Module[] modules, ModuleGroup group, @Nullable DataContext dataContext) {
        Project project = modules[0].getProject();
        for (Module module : modules) {
            ModifiableModuleModel model = dataContext != null
                ? dataContext.getData(LangDataKeys.MODIFIABLE_MODULE_MODEL)
                : null;
            if (model != null) {
                model.setModuleGroupPath(module, group == null ? null : group.getGroupPath());
            }
            else {
                ModuleManagerImpl.getInstanceImpl(project).setModuleGroupPath(module, group == null ? null : group.getGroupPath());
            }
        }

        ProjectViewPane pane = ProjectView.getInstance(project).getCurrentProjectViewPane();
        if (pane != null) {
            pane.updateFromRoot(true);
        }

        boolean processedInsideSettings = false;

        if (dataContext != null) {
            Settings settings = dataContext.getData(Settings.KEY);
            if (settings != null) {
                ModuleStructureConfigurable configurable = settings.findConfigurable(ModuleStructureConfigurable.class);
                if (configurable != null) {
                    processedInsideSettings = ModuleStructureConfigurable.processModulesMoved(configurable, modules, group);
                }
            }
        }

        if (!processedInsideSettings && pane != null) {
            if (group != null) {
                pane.selectModuleGroup(group, true);
            }
            else {
                pane.selectModule(modules[0], true);
            }
        }
    }
}
