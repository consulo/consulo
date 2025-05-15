/*
 * Copyright 2013-2017 consulo.io
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
package consulo.builtinWebServer.impl.json;

import consulo.annotation.component.ExtensionImpl;
import consulo.application.Application;
import consulo.application.internal.ApplicationInfo;
import consulo.builtinWebServer.http.HttpRequest;
import consulo.builtinWebServer.json.JsonGetRequestHandler;
import consulo.container.plugin.PluginDescriptor;
import consulo.container.plugin.PluginManager;
import consulo.externalService.update.UpdateChannel;
import consulo.externalService.update.UpdateSettings;
import jakarta.annotation.Nonnull;
import jakarta.inject.Inject;

import java.util.ArrayList;
import java.util.List;

/**
 * @author VISTALL
 * @since 06-May-17
 */
@ExtensionImpl
public class AboutRestHandler extends JsonGetRequestHandler {
    public static class AboutInfo {
        public String name;
        public int build;
        public UpdateChannel channel;
        public List<String> plugins = List.of();
    }

    @Nonnull
    private final Application myApplication;

    @Inject
    public AboutRestHandler(@Nonnull Application application) {
        super("about");
        myApplication = application;
    }

    @Override
    public boolean isAccessible(HttpRequest request) {
        return true; // trust in any cases
    }

    @Nonnull
    @Override
    public JsonResponse handle(HttpRequest request) {
        ApplicationInfo info = ApplicationInfo.getInstance();
        AboutInfo data = new AboutInfo();
        data.name = myApplication.getName().get();
        data.build = info.getBuild().getBuildNumber();
        data.channel = UpdateSettings.getInstance().getChannel();
        data.plugins = new ArrayList<>();
        for (PluginDescriptor plugin : PluginManager.getPlugins()) {
            data.plugins.add(plugin.getPluginId().getIdString());
        }
        return JsonResponse.asSuccess(data);
    }
}
