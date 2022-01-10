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
package consulo.plugins.internal;

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

  private List<String> myPluginErrors = null;
  private List<String> myPlugins2Disable = null;
  private Set<String> myPlugins2Enable = null;

  public void addPluginErrors(List<String> problems) {
    if (myPluginErrors == null) {
      myPluginErrors = new ArrayList<>(problems);
    }
    else {
      myPluginErrors.addAll(problems);
    }
  }

  public void setPluginsForDisable(List<String> pluginsForDisable) {
    myPlugins2Disable = pluginsForDisable;
  }

  public void setPluginsForEnable(Set<String> pluginsForEnable) {
    myPlugins2Enable = pluginsForEnable;
  }

  public List<String> getPluginErrors() {
    return myPluginErrors;
  }

  public List<String> getPlugins2Disable() {
    return myPlugins2Disable;
  }

  public Set<String> getPlugins2Enable() {
    return myPlugins2Enable;
  }
}
