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
package consulo.ide.impl.idea.ide.actions;

import consulo.annotation.component.ActionImpl;
import consulo.ide.setting.ShowSettingsUtil;
import consulo.language.editor.LangDataKeys;
import consulo.module.Module;
import consulo.platform.base.localize.ActionLocalize;
import consulo.project.Project;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.action.AnAction;
import consulo.ui.ex.action.AnActionEvent;
import jakarta.annotation.Nonnull;
import jakarta.inject.Inject;
import jakarta.inject.Provider;

/**
 * @author Eugene Zhuravlev
 * @since 2004-02-08
 */
@ActionImpl(id = "ModuleSettings")
public class ShowModulePropertiesAction extends AnAction {
    private final Provider<ShowSettingsUtil> myShowSettingsUtilProvider;

    @Inject
    public ShowModulePropertiesAction(Provider<ShowSettingsUtil> showSettingsUtilProvider) {
        super(ActionLocalize.actionModulesettingsText(), ActionLocalize.actionModulesettingsDescription());
        myShowSettingsUtilProvider = showSettingsUtilProvider;
    }

    @Override
    @RequiredUIAccess
    public void actionPerformed(@Nonnull AnActionEvent e) {
        Project project = e.getRequiredData(Project.KEY);
        Module module = e.getRequiredData(LangDataKeys.MODULE_CONTEXT);
        myShowSettingsUtilProvider.get().showProjectStructureDialog(project, it -> it.select(module.getName(), null, true));
    }

    @Override
    public void update(@Nonnull AnActionEvent e) {
        e.getPresentation().setVisible(e.hasData(Project.KEY) && e.hasData(LangDataKeys.MODULE_CONTEXT));
    }
}
