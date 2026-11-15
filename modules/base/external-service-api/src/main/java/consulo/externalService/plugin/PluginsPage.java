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
package consulo.externalService.plugin;

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ServiceAPI;
import consulo.application.Application;
import consulo.container.plugin.PluginId;
import consulo.project.Project;
import consulo.ui.annotation.RequiredUIAccess;
import org.jspecify.annotations.Nullable;

/**
 * The page listing installed and repository plugins, shown in an editor tab.
 *
 * @author VISTALL
 * @since 2026-11-14
 */
@ServiceAPI(ComponentScope.APPLICATION)
public interface PluginsPage {
    static PluginsPage getInstance() {
        return Application.get().getInstance(PluginsPage.class);
    }

    /**
     * Opens the page, or brings its tab to front.
     *
     * @param project  the project of the tab, the one of the focused frame when null
     * @param pluginId the plugin to select, if any
     */
    @RequiredUIAccess
    void open(@Nullable Project project, @Nullable PluginId pluginId);

    /**
     * Opens the page searching all plugins.
     *
     * @param project the project of the tab, the one of the focused frame when null
     * @param query   search text, with filters like {@code /tag:vcs}, {@code /extension:*.yaml} or
     *                {@code /extension:<api class>=<id>}
     */
    @RequiredUIAccess
    void search(@Nullable Project project, String query);
}
