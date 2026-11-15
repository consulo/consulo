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

import consulo.application.Application;
import consulo.configuration.editor.ConfigurationFileEditor;
import consulo.container.plugin.PluginId;
import consulo.disposer.Disposer;
import consulo.project.Project;
import consulo.ui.Component;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.virtualFileSystem.VirtualFile;
import org.jspecify.annotations.Nullable;

import java.util.Map;

/**
 * @author VISTALL
 * @since 2026-11-14
 */
public class PluginsFileEditor extends ConfigurationFileEditor {
    private final Application myApplication;

    private @Nullable UnifiedPluginsPage myPage;
    private @Nullable String myPluginToSelect;
    private @Nullable String mySearchQuery;

    public PluginsFileEditor(Application application, Project project, VirtualFile file) {
        super(project, file);
        myApplication = application;
    }

    /**
     * Built once and kept, the platform asks an editor for its component again whenever the selection changes.
     */
    @Override
    @RequiredUIAccess
    public Component getUIComponent() {
        UnifiedPluginsPage page = myPage;
        if (page == null) {
            myPage = page = new UnifiedPluginsPage(myApplication);
            Disposer.register(this, page);

            String searchQuery = mySearchQuery;
            mySearchQuery = null;
            if (searchQuery != null) {
                page.search(searchQuery);
            }

            String pluginToSelect = myPluginToSelect;
            myPluginToSelect = null;
            if (pluginToSelect != null) {
                page.select(PluginId.getId(pluginToSelect));
            }
        }
        return page.getComponent();
    }

    @Override
    public void dispose() {
    }

    @Override
    @RequiredUIAccess
    public void onUpdateRequestParams(Map<String, String> params) {
        String searchQuery = params.get(PluginsFileEditorProvider.SEARCH_PARAM);
        String pluginId = params.get(PluginsFileEditorProvider.PLUGIN_PARAM);

        UnifiedPluginsPage page = myPage;
        if (page == null) {
            mySearchQuery = searchQuery;
            myPluginToSelect = pluginId;
            return;
        }

        if (searchQuery != null) {
            page.search(searchQuery);
        }
        if (pluginId != null) {
            page.select(PluginId.getId(pluginId));
        }
    }
}
