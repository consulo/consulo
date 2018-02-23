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
package consulo.ide.plugins.pluginsAdvertisement;

import com.intellij.ide.plugins.IdeaPluginDescriptor;
import com.intellij.ide.plugins.PluginManager;
import com.intellij.ide.plugins.RepositoryHelper;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.util.containers.ContainerUtil;
import consulo.ide.plugins.InstalledPluginsState;
import consulo.ide.updateSettings.UpdateSettings;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;

/**
 * @author VISTALL
 * @since 05-May-17
 */
public class PluginsAdvertiserHolder {
  private static List<IdeaPluginDescriptor> ourLoadedPluginDescriptors;

  @Nonnull
  public static List<IdeaPluginDescriptor> getLoadedPluginDescriptors() {
    return ourLoadedPluginDescriptors == null ? Collections.emptyList() : ourLoadedPluginDescriptors;
  }

  public static void update(@Nullable List<IdeaPluginDescriptor> list) {
    ourLoadedPluginDescriptors = ContainerUtil.isEmpty(list) ? null : list;

    if (list != null) {
      InstalledPluginsState pluginsState = InstalledPluginsState.getInstance();

      for (IdeaPluginDescriptor newPluginDescriptor : list) {
        final IdeaPluginDescriptor installed = PluginManager.getPlugin(newPluginDescriptor.getPluginId());
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

  public static void initialize(@Nonnull Consumer<List<IdeaPluginDescriptor>> consumer) {
    UpdateSettings updateSettings = UpdateSettings.getInstance();
    if (!updateSettings.isEnable()) {
      return;
    }

    if (ourLoadedPluginDescriptors != null) {
      ApplicationManager.getApplication().executeOnPooledThread(() -> consumer.accept(ourLoadedPluginDescriptors));
      return;
    }

    ApplicationManager.getApplication().executeOnPooledThread(() -> {
      List<IdeaPluginDescriptor> pluginDescriptors = Collections.emptyList();
      try {
        pluginDescriptors = RepositoryHelper.loadPluginsFromRepository(null, updateSettings.getChannel());
      }
      catch (Exception ignored) {
      }

      if(ApplicationManager.getApplication().isDisposed()) {
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
