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
import consulo.annotation.component.ActionParentRef;
import consulo.annotation.component.ActionRef;
import consulo.annotation.component.ComponentProfiles;
import consulo.configuration.editor.ConfigurationFileEditorManager;
import consulo.localize.LocalizeValue;
import consulo.project.Project;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.action.AnActionEvent;
import consulo.ui.ex.action.DumbAwareAction;
import consulo.ui.ex.action.IdeActions;
import consulo.welcomeScreen.impl.internal.editor.WelcomeConfigurationFileEditorProvider;
import jakarta.annotation.Nonnull;
import jakarta.inject.Inject;

import java.util.Map;

/**
 * @author VISTALL
 * @since 2025-09-17
 */
@ActionImpl(
    id = "OpenWelcomeScreenAction",
    profiles = ComponentProfiles.SANDBOX,
    parents = @ActionParentRef(@ActionRef(id = IdeActions.TOOLS_MENU))
)
public class OpenWelcomeScreenAction extends DumbAwareAction {
    private final ConfigurationFileEditorManager myConfigurationFileEditorManager;

    @Inject
    public OpenWelcomeScreenAction(ConfigurationFileEditorManager configurationFileEditorManager) {
        super(LocalizeValue.localizeTODO("Open Welcome"));
        myConfigurationFileEditorManager = configurationFileEditorManager;
    }

    @RequiredUIAccess
    @Override
    public void actionPerformed(@Nonnull AnActionEvent e) {
        Project project = e.getData(Project.KEY);

        myConfigurationFileEditorManager.open(project, WelcomeConfigurationFileEditorProvider.class, Map.of());
    }
}
