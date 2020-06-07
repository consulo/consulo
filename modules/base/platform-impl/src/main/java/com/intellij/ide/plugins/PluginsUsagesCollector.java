/*
 * Copyright 2000-2011 JetBrains s.r.o.
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

import com.intellij.internal.statistic.UsagesCollector;
import com.intellij.internal.statistic.beans.UsageDescriptor;
import com.intellij.openapi.project.Project;
import com.intellij.util.containers.ContainerUtil;
import consulo.container.plugin.PluginDescriptor;
import consulo.container.plugin.PluginIds;
import consulo.container.plugin.PluginManager;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;
import java.util.Set;

public class PluginsUsagesCollector extends UsagesCollector {
  @Override
  @Nonnull
  public Set<UsageDescriptor> getUsages(@Nullable Project project) {
    final List<PluginDescriptor> plugins = PluginManager.getPlugins();
    final List<PluginDescriptor> enabledPlugins = ContainerUtil.filter(plugins, d -> d.isEnabled() && !PluginIds.isPlatformPlugin(d.getPluginId()));

    return ContainerUtil.map2Set(enabledPlugins, descriptor -> new UsageDescriptor(descriptor.getPluginId().getIdString(), 1));
  }

  @Override
  @Nonnull
  public String getGroupId() {
    return "consulo.platform.base:plugins";
  }
}
