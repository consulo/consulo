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
package consulo.ide.customize;

import com.intellij.util.containers.ContainerUtil;
import consulo.container.plugin.PluginDescriptor;
import consulo.container.plugin.PluginId;
import consulo.desktop.startup.customize.PluginTemplate;

import javax.annotation.Nonnull;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * @author VISTALL
 * @since 22/09/2021
 */
public class CustomizeWizardContext {
  private final Map<PluginId, PluginDescriptor> myPluginDescriptors;
  private final Map<String, PluginTemplate> myPredefinedTemplateSets;

  private String myEmail;

  private Set<String> myEnabledPluginSets = Set.of();
  private Set<PluginId> myPluginsForDownload = Set.of();

  public CustomizeWizardContext(List<PluginDescriptor> pluginDescriptors, Map<String, PluginTemplate> predefinedTemplateSets) {
    myPluginDescriptors = ContainerUtil.newMapFromValues(pluginDescriptors.iterator(), PluginDescriptor::getPluginId);
    myPredefinedTemplateSets = predefinedTemplateSets;
  }

  @Nonnull
  public Map<PluginId, PluginDescriptor> getPluginDescriptors() {
    return myPluginDescriptors;
  }

  @Nonnull
  public Map<String, PluginTemplate> getPredefinedTemplateSets() {
    return myPredefinedTemplateSets;
  }

  public void setEmail(String email) {
    myEmail = email;
  }

  public String getEmail() {
    return myEmail;
  }

  @Nonnull
  public Set<String> getEnabledPluginSets() {
    return myEnabledPluginSets;
  }

  public void setEnabledPluginSets(@Nonnull Set<String> enabledPluginSets) {
    myEnabledPluginSets = enabledPluginSets;
  }

  @Nonnull
  public Set<PluginId> getPluginsForDownload() {
    return myPluginsForDownload;
  }

  public void setPluginsForDownload(@Nonnull Set<PluginId> pluginsForDownload) {
    myPluginsForDownload = pluginsForDownload;
  }
}
