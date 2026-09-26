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
package consulo.externalService.impl.internal.plugin;

import consulo.annotation.component.ExtensionImpl;
import consulo.application.eap.EarlyAccessProgramManager;
import consulo.application.progress.Task;
import consulo.builtinWebServer.http.HttpRequest;
import consulo.builtinWebServer.http.OriginCheckResult;
import consulo.builtinWebServer.json.JsonGetRequestHandler;
import consulo.container.plugin.PluginDescriptor;
import consulo.container.plugin.PluginId;
import consulo.externalService.impl.internal.plugin.ui.action.InstallPluginAction;
import consulo.externalService.impl.internal.repository.RepositoryHelper;
import consulo.externalService.update.UpdateChannel;
import consulo.externalService.update.UpdateSettings;
import consulo.localize.LocalizeValue;
import consulo.logging.Logger;
import consulo.project.Project;
import consulo.project.ProjectManager;
import consulo.ui.UIAccess;
import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

/**
 * @author VISTALL
 * @since 2025-05-13
 */
@ExtensionImpl
public class InstallPluginRestHandler extends JsonGetRequestHandler {
    private static final Logger LOG = Logger.getInstance(InstallPluginRestHandler.class);

    private static final Set<String> TRUSTED_PREDEFINED_HOSTS = Set.of("hub.consulo.io", "consulo.io", "www.consulo.io");

    public InstallPluginRestHandler() {
        super("plugins/install");
    }

    @Override
    protected OriginCheckResult isOriginAllowed(HttpRequest request) {
        return OriginCheckResult.ASK_CONFIRMATION;
    }

    @Override
    protected boolean isHostTrusted(HttpRequest request) {
        return isHostInPredefinedHosts(request, TRUSTED_PREDEFINED_HOSTS, "consulo.api.install.hosts.trusted")
            || super.isHostTrusted(request);
    }

    @Override
    public JsonResponse handle(HttpRequest request) {
        String pluginIdStr = request == null ? null : request.getParameterValue("pluginId");
        if (pluginIdStr == null) {
            throw new IllegalArgumentException("PluginId expected");
        }

        Project project = findProject();
        if (project == null) {
            return JsonResponse.asError("No open project to install plugin: " + pluginIdStr);
        }

        Task.Backgroundable.queue(
            project,
            LocalizeValue.localizeTODO("Loading Plugins..."),
            progressIndicator -> {
                UpdateChannel channel = UpdateSettings.getInstance().getChannel();
                EarlyAccessProgramManager earlyAccessProgramManager = EarlyAccessProgramManager.getInstance();
                try {
                    List<PluginDescriptor> pluginDescriptors =
                        RepositoryHelper.loadOnlyPluginsFromRepository(progressIndicator, channel, earlyAccessProgramManager);

                    PluginId pluginId = PluginId.getId(pluginIdStr);

                    Optional<PluginDescriptor> target = pluginDescriptors
                        .stream()
                        .filter(pluginDescriptor -> Objects.equals(pluginDescriptor.getPluginId(), pluginId))
                        .findFirst();

                    if (target.isEmpty()) {
                        LOG.warn("Plugin can't installed: " + pluginIdStr);
                        return;
                    }

                    UIAccess uiAccess = project.getUIAccess();
                    if (!uiAccess.isValid()) {
                        LOG.warn("Plugin can't be installed, project is closed: " + pluginIdStr);
                        return;
                    }

                    uiAccess.give(() -> InstallPluginAction.install(
                        uiAccess,
                        null,
                        null,
                        project,
                        target.get(),
                        pluginDescriptors,
                        true,
                        null
                    ));
                }
                catch (Exception e) {
                    LOG.warn(e);
                }
            }
        );
        return JsonResponse.asSuccess(Map.of("pluginId", pluginIdStr));
    }

    private static @Nullable Project findProject() {
        for (Project project : ProjectManager.getInstance().getOpenProjects()) {
            if (!project.isDisposed() && project.getUIAccess().isValid()) {
                return project;
            }
        }
        return null;
    }
}
