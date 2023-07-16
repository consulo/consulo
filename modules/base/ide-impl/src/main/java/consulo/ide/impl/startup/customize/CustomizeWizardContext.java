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
package consulo.ide.impl.startup.customize;

import consulo.container.plugin.PluginDescriptor;
import consulo.container.plugin.PluginId;
import consulo.externalService.update.UpdateChannel;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.util.*;

/**
 * @author VISTALL
 * @since 22/09/2021
 */
public class CustomizeWizardContext {
  private final Map<String, Collection<PluginDescriptor>> myPluginDescriptorsByTag = new TreeMap<>();
  private final Map<PluginId, PluginDescriptor> myPluginDescriptors = new LinkedHashMap<>();
  private final Map<String, PluginTemplate> myPredefinedTemplateSets = new LinkedHashMap<>();
  private final boolean myIsInitialDarkTheme;
  @Nullable
  private final UpdateChannel myUpdateChannel;

  private String myEmail;

  private Set<String> myEnabledPluginSets = Set.of();
  private final Set<PluginId> myPluginsForDownload = new LinkedHashSet<>();

  public CustomizeWizardContext(boolean isInitialDarkTheme, @Nullable UpdateChannel updateChannel) {
    myIsInitialDarkTheme = isInitialDarkTheme;
    myUpdateChannel = updateChannel;
  }

  @Nonnull
  public Map<PluginId, PluginDescriptor> getPluginDescriptors() {
    return myPluginDescriptors;
  }

  @Nonnull
  public Map<String, Collection<PluginDescriptor>> getPluginDescriptorsByTag() {
    return myPluginDescriptorsByTag;
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

  public boolean isInitialDarkTheme() {
    return myIsInitialDarkTheme;
  }

  @Nullable
  public UpdateChannel getUpdateChannel() {
    return myUpdateChannel;
  }

  public void addPluginDescriptor(PluginDescriptor plugin) {
    myPluginDescriptors.put(plugin.getPluginId(), plugin);
    Set<String> tags = plugin.getTags();

    for (String tag : tags) {
      Collection<PluginDescriptor> list = myPluginDescriptorsByTag.computeIfAbsent(tag, s -> new ArrayList<>());
      list.add(plugin);
    }
  }
}