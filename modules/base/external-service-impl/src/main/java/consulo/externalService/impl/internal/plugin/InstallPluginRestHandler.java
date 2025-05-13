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
import consulo.application.Application;
import consulo.application.eap.EarlyAccessProgramManager;
import consulo.application.progress.Task;
import consulo.builtinWebServer.http.HttpRequest;
import consulo.builtinWebServer.json.JsonGetRequestHandler;
import consulo.container.plugin.PluginDescriptor;
import consulo.container.plugin.PluginId;
import consulo.externalService.impl.internal.plugin.ui.action.InstallPluginAction;
import consulo.externalService.impl.internal.repository.RepositoryHelper;
import consulo.externalService.update.UpdateChannel;
import consulo.externalService.update.UpdateSettings;
import consulo.logging.Logger;
import consulo.ui.UIAccess;
import jakarta.annotation.Nonnull;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * @author VISTALL
 * @since 2025-05-13
 */
@ExtensionImpl
public class InstallPluginRestHandler extends JsonGetRequestHandler {
    private static final Logger LOG = Logger.getInstance(InstallPluginRestHandler.class);

    public InstallPluginRestHandler() {
        super("plugins/install");
    }

    @Override
    public boolean isAccessible(HttpRequest request) {
        return true;
    }

    @Nonnull
    @Override
    public JsonResponse handle(HttpRequest request) {
        String pluginIdStr = request == null ? null : request.getParameterValue("pluginId");
        if (pluginIdStr == null) {
            throw new IllegalArgumentException("PluginId expected");
        }

        UIAccess uiAccess = Application.get().getLastUIAccess();

        Task.Backgroundable.queue(null, "Loading Plugins...", progressIndicator -> {
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

                uiAccess.give(() -> InstallPluginAction.install(uiAccess, null, null, null, target.get(), pluginDescriptors, true, null));
            }
            catch (Exception e) {
                LOG.warn(e);
            }
        });
        return JsonResponse.asSuccess(Map.of("pluginId", pluginIdStr));
    }
}
