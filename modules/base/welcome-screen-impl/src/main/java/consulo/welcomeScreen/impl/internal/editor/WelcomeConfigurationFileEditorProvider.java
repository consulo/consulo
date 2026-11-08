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
package consulo.welcomeScreen.impl.internal.editor;

import consulo.annotation.component.ExtensionImpl;
import consulo.configuration.editor.ConfigurationFileEditor;
import consulo.configuration.editor.ConfigurationFileEditorProvider;
import consulo.dataContext.DataManager;
import consulo.localize.LocalizeValue;
import consulo.platform.base.icon.PlatformIconGroup;
import consulo.project.Project;
import consulo.project.RecentProjectsManager;
import consulo.ui.ex.action.ActionManager;
import consulo.ui.image.Image;
import consulo.virtualFileSystem.VirtualFile;
import jakarta.inject.Inject;
import jakarta.inject.Provider;

/**
 * @author VISTALL
 * @since 2025-09-17
 */
@ExtensionImpl
public class WelcomeConfigurationFileEditorProvider implements ConfigurationFileEditorProvider {
    private final Provider<RecentProjectsManager> myRecentProjectsManager;
    private final DataManager myDataManager;
    private final ActionManager myActionManager;

    @Inject
    public WelcomeConfigurationFileEditorProvider(
        Provider<RecentProjectsManager> recentProjectsManager,
        DataManager dataManager,
        ActionManager actionManager
    ) {
        myRecentProjectsManager = recentProjectsManager;
        myDataManager = dataManager;
        myActionManager = actionManager;
    }

    
    @Override
    public String getId() {
        return "welcome";
    }

    
    @Override
    public Image getIcon() {
        return PlatformIconGroup.filetypesAny_type();
    }

    
    @Override
    public LocalizeValue getName() {
        return LocalizeValue.localizeTODO("Welcome");
    }

    
    @Override
    public ConfigurationFileEditor createEditor(Project project, VirtualFile file) {
        return new WelcomeConfigurationFileEditor(project, file, myRecentProjectsManager.get(), myDataManager, myActionManager);
    }
}
