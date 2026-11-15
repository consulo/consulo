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

import consulo.annotation.component.ServiceImpl;
import consulo.application.ui.wm.IdeFocusManager;
import consulo.configuration.editor.ConfigurationFileEditorManager;
import consulo.container.plugin.PluginId;
import consulo.externalService.plugin.PluginsPage;
import consulo.logging.Logger;
import consulo.project.Project;
import consulo.project.ProjectManager;
import consulo.project.ui.wm.IdeFrame;
import consulo.ui.annotation.RequiredUIAccess;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.jspecify.annotations.Nullable;

import java.util.Map;

/**
 * @author VISTALL
 * @since 2026-11-14
 */
@Singleton
@ServiceImpl
public class PluginsPageImpl implements PluginsPage {
    private static final Logger LOG = Logger.getInstance(PluginsPageImpl.class);

    private final ConfigurationFileEditorManager myConfigurationFileEditorManager;

    @Inject
    public PluginsPageImpl(ConfigurationFileEditorManager configurationFileEditorManager) {
        myConfigurationFileEditorManager = configurationFileEditorManager;
    }

    @Override
    @RequiredUIAccess
    public void open(@Nullable Project project, @Nullable PluginId pluginId) {
        open(project, pluginId == null ? Map.of() : Map.of(PluginsFileEditorProvider.PLUGIN_PARAM, pluginId.getIdString()));
    }

    @Override
    @RequiredUIAccess
    public void search(@Nullable Project project, String query) {
        open(project, Map.of(PluginsFileEditorProvider.SEARCH_PARAM, query));
    }

    @RequiredUIAccess
    private void open(@Nullable Project project, Map<String, String> params) {
        Project target = project != null ? project : findFocusedProject();
        if (target == null) {
            LOG.warn("No project to open the plugins page in");
            return;
        }
        myConfigurationFileEditorManager.open(target, PluginsFileEditorProvider.class, params);
    }

    private static @Nullable Project findFocusedProject() {
        if (IdeFocusManager.getGlobalInstance().getLastFocusedFrame() instanceof IdeFrame frame && frame.getProject() != null) {
            return frame.getProject();
        }
        Project[] projects = ProjectManager.getInstance().getOpenProjects();
        return projects.length == 0 ? null : projects[0];
    }
}
