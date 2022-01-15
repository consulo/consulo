/*
 * Copyright 2013-2021 consulo.io
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
package consulo.externalService.impl.repository.history;

import consulo.externalService.impl.WebServiceApi;
import consulo.externalService.impl.WebServiceApiSender;
import consulo.logging.Logger;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.util.Map;

/**
 * @author VISTALL
 * @since 20/11/2021
 */
public class PluginHistoryManager {
  private static final Logger LOG = Logger.getInstance(PluginHistoryManager.class);

  @Nonnull
  public static PluginHistoryEntry[] fetchHistory(@Nonnull String pluginId, @Nonnull String fromVersion, @Nonnull String toVersion) {
    Map<String, String> params;
    String baseUrl;
    if (fromVersion.equals(toVersion)) {
      baseUrl = "history/listByVersion";
      params = Map.of("id", pluginId, "version", fromVersion/*, "includeFromVersion", "false"*/);
    }
    else {
      baseUrl = "history/listByVersionRange";
      params = Map.of("id", pluginId, "fromVersion", fromVersion, "toVersion", toVersion/*, "includeFromVersion", "false"*/);
    }

    try {
      return WebServiceApiSender.doGet(WebServiceApi.REPOSITORY_API, baseUrl, params, PluginHistoryEntry[].class);
    }
    catch (IOException e) {
      LOG.warn(e);
      return new PluginHistoryEntry[0];
    }
  }
}
