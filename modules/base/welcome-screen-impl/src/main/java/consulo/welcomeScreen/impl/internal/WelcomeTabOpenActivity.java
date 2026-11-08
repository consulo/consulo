/*
 * Copyright 2013-2026 consulo.io
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
package consulo.welcomeScreen.impl.internal;

import consulo.annotation.component.ExtensionImpl;
import consulo.application.dumb.DumbAware;
import consulo.configuration.editor.ConfigurationFileEditorManager;
import consulo.project.Project;
import consulo.project.ProjectType;
import consulo.project.startup.PostStartupActivity;
import consulo.ui.UIAccess;
import consulo.welcomeScreen.impl.internal.editor.WelcomeConfigurationFileEditorProvider;
import jakarta.inject.Inject;

import java.util.Map;

/**
 * @author VISTALL
 * @since 2026-11-07
 */
@ExtensionImpl
public class WelcomeTabOpenActivity implements PostStartupActivity, DumbAware {
    private final ConfigurationFileEditorManager myConfigurationFileEditorManager;

    @Inject
    public WelcomeTabOpenActivity(ConfigurationFileEditorManager configurationFileEditorManager) {
        myConfigurationFileEditorManager = configurationFileEditorManager;
    }

    @Override
    public void runActivity(Project project, UIAccess uiAccess) {
        if (project.getType() != ProjectType.WELCOME) {
            return;
        }

        uiAccess.give(() -> myConfigurationFileEditorManager.open(project, WelcomeConfigurationFileEditorProvider.class, Map.of()));
    }
}
