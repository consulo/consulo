/*
 * Copyright 2013-2022 consulo.io
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
package consulo.container.internal.plugin;

import consulo.container.plugin.PluginId;

import java.util.HashSet;
import java.util.Set;

/**
 * @author VISTALL
 * @since 28-Aug-22
 */
public class ClassPathPluginSet {
  private final Set<PluginId> myPluginIdSet = new HashSet<>();

  public void add(PluginId pluginId) {
    myPluginIdSet.add(pluginId);
  }

  public boolean accept(Set<PluginId> enablePluginIds) {
    if (enablePluginIds.containsAll(myPluginIdSet)) {
      return true;
    }
    return false;
  }

  @Override
  public String toString() {
    final StringBuilder sb = new StringBuilder("ClassPathPluginSet{");
    sb.append("myPluginIdSet=").append(myPluginIdSet);
    sb.append('}');
    return sb.toString();
  }
}
