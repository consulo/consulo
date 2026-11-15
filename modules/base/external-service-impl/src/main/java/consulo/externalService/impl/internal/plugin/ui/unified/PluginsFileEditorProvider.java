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
package consulo.externalService.impl.internal.plugin.ui.unified;

import consulo.annotation.component.ExtensionImpl;
import consulo.application.Application;
import consulo.configuration.editor.ConfigurationFileEditor;
import consulo.configuration.editor.ConfigurationFileEditorProvider;
import consulo.externalService.localize.ExternalServiceLocalize;
import consulo.localize.LocalizeValue;
import consulo.platform.base.icon.PlatformIconGroup;
import consulo.project.Project;
import consulo.ui.image.Image;
import consulo.virtualFileSystem.VirtualFile;
import jakarta.inject.Inject;

/**
 * @author VISTALL
 * @since 2026-11-14
 */
@ExtensionImpl
public class PluginsFileEditorProvider implements ConfigurationFileEditorProvider {
    public static final String PLUGIN_PARAM = "plugin";
    public static final String SEARCH_PARAM = "search";

    private final Application myApplication;

    @Inject
    public PluginsFileEditorProvider(Application application) {
        myApplication = application;
    }

    @Override
    public String getId() {
        return "plugins";
    }

    @Override
    public Image getIcon() {
        return PlatformIconGroup.nodesPlugin();
    }

    @Override
    public LocalizeValue getName() {
        return ExternalServiceLocalize.titlePlugins();
    }

    @Override
    public ConfigurationFileEditor createEditor(Project project, VirtualFile file) {
        return new PluginsFileEditor(myApplication, project, file);
    }
}
