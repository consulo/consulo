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
package com.intellij.ide.plugins;

import com.google.gson.Gson;
import com.intellij.ide.IdeBundle;
import com.intellij.openapi.application.ApplicationInfo;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.util.SystemProperties;
import com.intellij.util.containers.ContainerUtil;
import com.intellij.util.net.HttpConfigurable;
import consulo.container.plugin.PluginDescriptor;
import consulo.ide.eap.EarlyAccessProgramManager;
import consulo.ide.eap.plugins.ExperimentalPluginsDescriptor;
import consulo.ide.plugins.PluginJsonNode;
import consulo.ide.updateSettings.UpdateChannel;
import consulo.ide.updateSettings.impl.PlatformOrPluginUpdateChecker;
import consulo.externalService.impl.WebServiceApi;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.GZIPInputStream;

/**
 * @author stathik
 * @since Mar 28, 2003
 */
public class RepositoryHelper {
  @Nonnull
  public static String buildUrlForList(@Nonnull UpdateChannel channel, @Nonnull String platformVersion) {
    return WebServiceApi.REPOSITORY_API.buildUrl("list") + "?platformVersion=" + platformVersion + "&channel=" + channel;
  }

  @Nonnull
  public static String buildUrlForDownload(@Nonnull UpdateChannel channel, @Nonnull String pluginId, @Nullable String platformVersion, boolean noTracking, boolean viaUpdate) {
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
    return builder.toString();
  }

  /**
   * Load & return only plugins from repository
   */
  @Nonnull
  public static List<PluginDescriptor> loadOnlyPluginsFromRepository(@Nullable ProgressIndicator indicator, @Nonnull UpdateChannel channel, @Nonnull EarlyAccessProgramManager eapManager)
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
  public static List<PluginDescriptor> loadPluginsFromRepository(@Nullable ProgressIndicator indicator, @Nonnull UpdateChannel channel) throws Exception {
    return loadPluginsFromRepository(indicator, channel, null);
  }

  /**
   * @param indicator
   * @param channel
   * @param buildNumber if null used app build number
   * @return
   * @throws Exception
   */
  @Nonnull
  public static List<PluginDescriptor> loadPluginsFromRepository(@Nullable ProgressIndicator indicator, @Nonnull UpdateChannel channel, @Nullable String buildNumber) throws Exception {
    if (buildNumber == null) {
      ApplicationInfo appInfo = ApplicationInfo.getInstance();
      buildNumber = appInfo.getBuild().asString();
    }

    String url = buildUrlForList(channel, buildNumber);

    if (indicator != null) {
      indicator.setText2(IdeBundle.message("progress.connecting.to.plugin.manager", WebServiceApi.REPOSITORY_API.buildUrl()));
    }

    HttpURLConnection connection = HttpConfigurable.getInstance().openHttpConnection(url);
    connection.setRequestProperty("Accept-Encoding", "gzip");

    if (indicator != null) {
      indicator.setText2(IdeBundle.message("progress.waiting.for.reply.from.plugin.manager", WebServiceApi.REPOSITORY_API.buildUrl()));
    }

    connection.connect();
    try {
      if (indicator != null) {
        indicator.checkCanceled();
      }

      String encoding = connection.getContentEncoding();
      InputStream is = connection.getInputStream();
      try {
        if ("gzip".equalsIgnoreCase(encoding)) {
          is = new GZIPInputStream(is);
        }

        if (indicator != null) {
          indicator.setText2(IdeBundle.message("progress.downloading.list.of.plugins"));
        }

        return readPluginsStream(is, indicator);
      }
      finally {
        is.close();
      }
    }
    finally {
      connection.disconnect();
    }
  }

  private static List<PluginDescriptor> readPluginsStream(InputStream is, ProgressIndicator indicator) throws Exception {
    ByteArrayOutputStream os = new ByteArrayOutputStream();
    try {
      byte[] buffer = new byte[1024];
      int size;
      while ((size = is.read(buffer)) > 0) {
        os.write(buffer, 0, size);
        if (indicator != null) {
          indicator.checkCanceled();
        }
      }
    }
    finally {
      os.close();
    }

    PluginJsonNode[] nodes = new Gson().fromJson(new InputStreamReader(new ByteArrayInputStream(os.toByteArray()), StandardCharsets.UTF_8), PluginJsonNode[].class);

    List<PluginDescriptor> pluginDescriptors = new ArrayList<>(nodes.length);
    for (PluginJsonNode jsonPlugin : nodes) {
      pluginDescriptors.add(new PluginNode(jsonPlugin));
    }
    return pluginDescriptors;
  }
}
