/*
 * Copyright 2013-2023 consulo.io
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
package consulo.externalService.impl.internal.repository.api.pluginHistory;

import java.util.List;

/**
 * @author VISTALL
 * @since 19/08/2023
 */
public class PluginHistoryRequest {
  public static class PluginInfo {
    public String id;

    public String fromVersion;

    public String toVersion;

    public boolean includeFromVersion = true;


    public PluginInfo(String id, String version) {
      this.id = id;
      this.fromVersion = version;
      this.toVersion = version;
    }

    public PluginInfo(String id, String fromVersion, String toVersion, boolean includeFromVersion) {
      this.id = id;
      this.fromVersion = fromVersion;
      this.toVersion = toVersion;
      this.includeFromVersion = includeFromVersion;
    }
  }

  public List<PluginInfo> plugins = List.of();

  public PluginHistoryRequest(List<PluginInfo> plugins) {
    this.plugins = plugins;
  }
}