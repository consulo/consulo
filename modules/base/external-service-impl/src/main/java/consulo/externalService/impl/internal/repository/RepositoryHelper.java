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
package consulo.externalService.impl.internal.repository;

import com.google.gson.Gson;
import consulo.application.eap.EarlyAccessProgramManager;
import consulo.application.internal.ApplicationInfo;
import consulo.application.progress.ProgressIndicator;
import consulo.container.plugin.PluginDescriptor;
import consulo.externalService.impl.internal.WebServiceApi;
import consulo.externalService.impl.internal.plugin.ExperimentalPluginsDescriptor;
import consulo.externalService.impl.internal.plugin.PluginJsonNode;
import consulo.externalService.impl.internal.plugin.PluginNode;
import consulo.externalService.impl.internal.update.PlatformOrPluginUpdateChecker;
import consulo.externalService.localize.ExternalServiceLocalize;
import consulo.externalService.update.UpdateChannel;
import consulo.http.HttpRequests;
import consulo.util.collection.ContainerUtil;
import consulo.util.io.UnsyncByteArrayInputStream;
import consulo.util.lang.SystemProperties;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.apache.http.client.utils.URIBuilder;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * @author stathik
 * @since 2003-03-28
 */
public class RepositoryHelper {
    @Nonnull
    private static String buildUrlForList(@Nonnull UpdateChannel channel, @Nonnull String platformVersion, boolean addObsoletePlatformsV2) {
        try {
            return new URIBuilder(WebServiceApi.REPOSITORY_API.buildUrl("list"))
                .addParameter("platformVersion", platformVersion)
                .addParameter("channel", channel.toString())
                .addParameter("addObsoletePlatformsV2", Boolean.toString(addObsoletePlatformsV2))
                .toString();
        }
        catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }
    }

    @Nonnull
    public static String buildUrlForDownload(
        @Nonnull UpdateChannel channel,
        @Nonnull String pluginId,
        @Nullable String platformVersion,
        boolean noTracking,
        boolean viaUpdate,
        boolean noRedirect
    ) {
        if (platformVersion == null) {
            platformVersion = ApplicationInfo.getInstance().getBuild().asString();
        }

        URIBuilder builder;
        try {
            builder = new URIBuilder(WebServiceApi.REPOSITORY_API.buildUrl("download"))
                .addParameter("platformVersion", platformVersion)
                .addParameter("channel", channel.toString())
                .addParameter("id", pluginId);
        }
        catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }

        if (!noTracking) {
            noTracking = SystemProperties.getBooleanProperty("consulo.repository.no.tracking", false);
        }

        if (noTracking) {
            builder.addParameter("noTracking", "true");
        }
        if (viaUpdate) {
            builder.addParameter("viaUpdate", "true");
        }
        if (noRedirect) {
            builder.addParameter("noRedirect", "true");
        }
        return builder.toString();
    }

    /**
     * Load & return only plugins from repository
     */
    @Nonnull
    public static List<PluginDescriptor> loadOnlyPluginsFromRepository(
        @Nullable ProgressIndicator indicator,
        @Nonnull UpdateChannel channel,
        @Nonnull EarlyAccessProgramManager eapManager
    )
        throws Exception {
        List<PluginDescriptor> ideaPluginDescriptors = loadPluginsFromRepository(indicator, channel);
        return ContainerUtil.filter(ideaPluginDescriptors, it -> isPluginAllowed(it, eapManager));
    }

    @SuppressWarnings("SimplifiableIfStatement")
    private static boolean isPluginAllowed(PluginDescriptor pluginDescriptor, EarlyAccessProgramManager earlyAccessProgramManager) {
        if (PlatformOrPluginUpdateChecker.isPlatform(pluginDescriptor.getPluginId())) {
            return false;
        }

        return !pluginDescriptor.isExperimental() || earlyAccessProgramManager.getState(ExperimentalPluginsDescriptor.class);
    }

    @Nonnull
    public static List<PluginDescriptor> loadPluginsFromRepository(
        @Nullable ProgressIndicator indicator,
        @Nonnull UpdateChannel channel
    ) throws Exception {
        return loadPluginsFromRepository(indicator, channel, null, false);
    }

    @Nonnull
    public static List<PluginDescriptor> loadPluginsFromRepository(
        @Nullable ProgressIndicator indicator,
        @Nonnull UpdateChannel channel,
        @Nullable String buildNumber,
        boolean withObsoletePlatforms
    ) throws Exception {
        if (buildNumber == null) {
            ApplicationInfo appInfo = ApplicationInfo.getInstance();
            buildNumber = appInfo.getBuild().asString();
        }

        String url = buildUrlForList(channel, buildNumber, withObsoletePlatforms);

        if (indicator != null) {
            indicator.setText2Value(ExternalServiceLocalize.progressConnectingToPluginManager(WebServiceApi.REPOSITORY_API.buildUrl()));
        }

        try {
            byte[] bytes = HttpRequests.request(url).connect(request -> {
                if (indicator != null) {
                    indicator.setText2Value(ExternalServiceLocalize.progressDownloadingListOfPlugins());
                }

                return request.readBytes(indicator);
            });

            return readPluginsStream(new UnsyncByteArrayInputStream(bytes));
        }
        catch (IOException e) {
            throw new IOException("Failed to read data from URL: " + url + "\nView logs for details.", e);
        }
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
