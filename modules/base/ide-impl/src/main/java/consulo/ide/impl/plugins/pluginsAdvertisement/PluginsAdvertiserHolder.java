/*
 * Copyright 2013-2017 consulo.io
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
package consulo.ide.impl.plugins.pluginsAdvertisement;

import consulo.application.Application;
import consulo.application.eap.EarlyAccessProgramManager;
import consulo.container.plugin.PluginDescriptor;
import consulo.externalService.update.UpdateSettings;
import consulo.ide.impl.idea.ide.plugins.PluginManager;
import consulo.ide.impl.idea.ide.plugins.RepositoryHelper;
import consulo.util.lang.StringUtil;
import consulo.ide.impl.idea.util.containers.ContainerUtil;
import consulo.ide.impl.plugins.InstalledPluginsState;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;

/**
 * @author VISTALL
 * @since 05-May-17
 */
public class PluginsAdvertiserHolder {
  private static List<PluginDescriptor> ourLoadedPluginDescriptors;

  @Nonnull
  public static List<PluginDescriptor> getLoadedPluginDescriptors() {
    return ourLoadedPluginDescriptors == null ? Collections.emptyList() : ourLoadedPluginDescriptors;
  }

  public static void update(@Nullable List<PluginDescriptor> list) {
    ourLoadedPluginDescriptors = ContainerUtil.isEmpty(list) ? null : list;

    if (list != null) {
      InstalledPluginsState pluginsState = InstalledPluginsState.getInstance();

      for (PluginDescriptor newPluginDescriptor : list) {
        final PluginDescriptor installed = PluginManager.getPlugin(newPluginDescriptor.getPluginId());
        if (installed != null) {
          int state = StringUtil.compareVersionNumbers(newPluginDescriptor.getVersion(), installed.getVersion());

          if (state > 0 &&
              !PluginManager.isIncompatible(newPluginDescriptor) &&
              !pluginsState.getUpdatedPlugins().contains(newPluginDescriptor.getPluginId())) {
            pluginsState.getOutdatedPlugins().add(newPluginDescriptor.getPluginId());
          }
        }
      }
    }
  }

  public static void initialize(@Nonnull Consumer<List<PluginDescriptor>> consumer) {
    UpdateSettings updateSettings = UpdateSettings.getInstance();
    if (!updateSettings.isEnable()) {
      return;
    }

    if (ourLoadedPluginDescriptors != null) {
      Application.get().executeOnPooledThread(() -> consumer.accept(ourLoadedPluginDescriptors));
      return;
    }

    Application.get().executeOnPooledThread(() -> {
      List<PluginDescriptor> pluginDescriptors = Collections.emptyList();
      try {
        pluginDescriptors = RepositoryHelper.loadOnlyPluginsFromRepository(null, updateSettings.getChannel(), EarlyAccessProgramManager.getInstance());
      }
      catch (Exception ignored) {
      }

      if(Application.get().isDisposed()) {
        return;
      }

      update(pluginDescriptors);

      if (pluginDescriptors.isEmpty()) {
        return;
      }

      consumer.accept(pluginDescriptors);
    });
  }
}
