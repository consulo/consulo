/*
 * Copyright 2000-2013 JetBrains s.r.o.
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
package consulo.ide.impl.idea.ide.plugins;

import com.google.gson.Gson;
import consulo.application.eap.EarlyAccessProgramManager;
import consulo.application.internal.ApplicationInfo;
import consulo.application.progress.ProgressIndicator;
import consulo.container.plugin.PluginDescriptor;
import consulo.externalService.update.UpdateChannel;
import consulo.http.HttpRequests;
import consulo.ide.impl.eap.plugins.ExperimentalPluginsDescriptor;
import consulo.ide.impl.externalService.impl.WebServiceApi;
import consulo.ide.impl.plugins.PluginJsonNode;
import consulo.ide.impl.updateSettings.impl.PlatformOrPluginUpdateChecker;
import consulo.ide.localize.IdeLocalize;
import consulo.util.collection.ContainerUtil;
import consulo.util.io.UnsyncByteArrayInputStream;
import consulo.util.lang.SystemProperties;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * @author stathik
 * @since Mar 28, 2003
 */
public class RepositoryHelper {
    @Nonnull
    public static String buildUrlForList(@Nonnull UpdateChannel channel, @Nonnull String platformVersion) {
        return new StringBuilder().append(WebServiceApi.REPOSITORY_API.buildUrl("list"))
            .append("?platformVersion=")
            .append(platformVersion)
            .append("&channel=")
            .append(channel)
            .append("&addObsoletePlatforms=false")
            .toString();
    }

    @Nonnull
    public static String buildUrlForDownload(@Nonnull UpdateChannel channel,
                                             @Nonnull String pluginId,
                                             @Nullable String platformVersion,
                                             boolean noTracking,
                                             boolean viaUpdate,
                                             boolean noRedirect) {
        if (platformVersion == null) {
            platformVersion = ApplicationInfo.getInstance().getBuild().asString();
        }

        StringBuilder builder = new StringBuilder();
        builder.append(WebServiceApi.REPOSITORY_API.buildUrl("download"));
        builder.append("?platformVersion=");
        builder.append(platformVersion);
        builder.append("&channel=");
        builder.append(channel);
        builder.append("&id=");
        builder.append(pluginId);

        if (!noTracking) {
            noTracking = SystemProperties.getBooleanProperty("consulo.repository.no.tracking", false);
        }

        if (noTracking) {
            builder.append("&noTracking=true");
        }
        if (viaUpdate) {
            builder.append("&viaUpdate=true");
        }
        if (noRedirect) {
            builder.append("&noRedirect=true");
        }
        return builder.toString();
    }

    /**
     * Load & return only plugins from repository
     */
    @Nonnull
    public static List<PluginDescriptor> loadOnlyPluginsFromRepository(@Nullable ProgressIndicator indicator,
                                                                       @Nonnull UpdateChannel channel,
                                                                       @Nonnull EarlyAccessProgramManager eapManager)
        throws Exception {
        List<PluginDescriptor> ideaPluginDescriptors = loadPluginsFromRepository(indicator, channel);
        return ContainerUtil.filter(ideaPluginDescriptors, it -> isPluginAllowed(it, eapManager));
    }

    private static boolean isPluginAllowed(PluginDescriptor pluginDescriptor, EarlyAccessProgramManager earlyAccessProgramManager) {
        if (PlatformOrPluginUpdateChecker.isPlatform(pluginDescriptor.getPluginId())) {
            return false;
        }

        return !pluginDescriptor.isExperimental() || earlyAccessProgramManager.getState(ExperimentalPluginsDescriptor.class);
    }

    @Nonnull
    public static List<PluginDescriptor> loadPluginsFromRepository(@Nullable ProgressIndicator indicator,
                                                                   @Nonnull UpdateChannel channel) throws Exception {
        return loadPluginsFromRepository(indicator, channel, null);
    }

    @Nonnull
    public static List<PluginDescriptor> loadPluginsFromRepository(@Nullable ProgressIndicator indicator,
                                                                   @Nonnull UpdateChannel channel,
                                                                   @Nullable String buildNumber) throws Exception {
        if (buildNumber == null) {
            ApplicationInfo appInfo = ApplicationInfo.getInstance();
            buildNumber = appInfo.getBuild().asString();
        }

        String url = buildUrlForList(channel, buildNumber);

        if (indicator != null) {
            indicator.setText2Value(IdeLocalize.progressConnectingToPluginManager(WebServiceApi.REPOSITORY_API.buildUrl()));
        }

        byte[] bytes = HttpRequests.request(url)
            .connect(request -> {
                if (indicator != null) {
                    indicator.setText2Value(IdeLocalize.progressDownloadingListOfPlugins());
                }

                return request.readBytes(indicator);
            });

        return readPluginsStream(new UnsyncByteArrayInputStream(bytes));
    }

    private static List<PluginDescriptor> readPluginsStream(InputStream is) throws Exception {
        PluginJsonNode[] nodes = new Gson().fromJson(new InputStreamReader(is, StandardCharsets.UTF_8), PluginJsonNode[].class);

        List<PluginDescriptor> pluginDescriptors = new ArrayList<>(nodes.length);
        for (PluginJsonNode jsonPlugin : nodes) {
            pluginDescriptors.add(new PluginNode(jsonPlugin));
        }

        return pluginDescriptors;
    }
}
