/*
 * Copyright 2013-2020 consulo.io
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
package consulo.application.impl.internal.plugin;

import consulo.container.plugin.PluginId;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * @author VISTALL
 * @since 2020-05-23
 */
public class PluginsInitializeInfo {
  public static final String DISABLE = "disable";
  public static final String ENABLE = "enable";
  public static final String EDIT = "edit";

  private List<CompositeMessage> myPluginErrors = null;
  private Set<PluginId> myPlugins2Disable = null;
  private Set<PluginId> myPlugins2Enable = null;

  public void addPluginErrors(List<CompositeMessage> problems) {
    if (myPluginErrors == null) {
      myPluginErrors = new ArrayList<>(problems);
    }
    else {
      myPluginErrors.addAll(problems);
    }
  }

  public void setPluginsForDisable(Set<PluginId> pluginsForDisable) {
    myPlugins2Disable = pluginsForDisable;
  }

  public void setPluginsForEnable(Set<PluginId> pluginsForEnable) {
    myPlugins2Enable = pluginsForEnable;
  }

  public List<CompositeMessage> getPluginErrors() {
    return myPluginErrors;
  }

  public Set<PluginId> getPlugins2Disable() {
    return myPlugins2Disable;
  }

  public Set<PluginId> getPlugins2Enable() {
    return myPlugins2Enable;
  }
}
