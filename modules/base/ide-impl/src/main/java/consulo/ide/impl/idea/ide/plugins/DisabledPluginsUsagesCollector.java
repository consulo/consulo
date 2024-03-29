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
package consulo.ide.impl.idea.ide.plugins;

import consulo.annotation.component.ExtensionImpl;
import consulo.externalService.statistic.UsagesCollector;
import consulo.externalService.statistic.UsageDescriptor;
import consulo.project.Project;
import consulo.ide.impl.idea.util.containers.ContainerUtil;
import consulo.container.plugin.PluginManager;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import java.util.Set;

@ExtensionImpl
public class DisabledPluginsUsagesCollector extends UsagesCollector {
  @Override
  @Nonnull
  public String getGroupId() {
    return "consulo.platform.base:disable.plugins";
  }

  @Override
  @Nonnull
  public Set<UsageDescriptor> getUsages(@Nullable Project project) {
    return ContainerUtil.map2Set(PluginManager.getDisabledPlugins(), id -> new UsageDescriptor(id.getIdString(), 1));
  }
}
