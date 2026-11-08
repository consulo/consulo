/*
 * Copyright 2013-2025 consulo.io
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
package consulo.welcomeScreen.impl.internal.action;

import consulo.annotation.component.ActionImpl;
import consulo.configuration.editor.ConfigurationFileEditorManager;
import consulo.platform.base.localize.ActionLocalize;
import consulo.project.Project;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.action.AnActionEvent;
import consulo.ui.ex.action.LegacyDumbAwareAction;
import consulo.welcomeScreen.impl.internal.editor.WelcomeConfigurationFileEditorProvider;
import jakarta.inject.Inject;

import java.util.Map;

/**
 * Opens the welcome tab, which lists the recent projects, in the current window.
 *
 * @author VISTALL
 * @since 2025-09-17
 */
@ActionImpl(id = "ManageRecentProjects")
public class ManageRecentProjectsAction extends LegacyDumbAwareAction {
    private final ConfigurationFileEditorManager myConfigurationFileEditorManager;

    @Inject
    public ManageRecentProjectsAction(ConfigurationFileEditorManager configurationFileEditorManager) {
        super(ActionLocalize.actionManagerecentprojectsText());
        myConfigurationFileEditorManager = configurationFileEditorManager;
    }

    @RequiredUIAccess
    @Override
    public void actionPerformed(AnActionEvent e) {
        Project project = e.getRequiredData(Project.KEY);

        myConfigurationFileEditorManager.open(project, WelcomeConfigurationFileEditorProvider.class, Map.of());
    }

    @Override
    public void update(AnActionEvent e) {
        e.getPresentation().setEnabledAndVisible(e.hasData(Project.KEY));
    }
}
