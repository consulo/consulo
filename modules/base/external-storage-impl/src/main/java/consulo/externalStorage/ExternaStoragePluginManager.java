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
package consulo.externalStorage;

import com.intellij.ide.plugins.PluginInstallUtil;
import com.intellij.ide.plugins.RepositoryHelper;
import com.intellij.openapi.application.Application;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.updateSettings.impl.PluginDownloader;
import com.intellij.util.containers.ContainerUtil;
import consulo.container.plugin.PluginDescriptor;
import consulo.container.plugin.PluginId;
import consulo.container.plugin.PluginManager;
import consulo.externalService.impl.WebServiceApi;
import consulo.externalService.impl.WebServiceApiSender;
import consulo.externalStorage.plugin.PluginInfoBean;
import consulo.ide.eap.EarlyAccessProgramManager;
import consulo.ide.plugins.InstalledPluginsState;
import consulo.ide.updateSettings.UpdateSettings;
import consulo.localize.LocalizeValue;
import consulo.logging.Logger;
import consulo.util.lang.Couple;

import javax.annotation.Nonnull;
import java.util.*;

/**
 * @author VISTALL
 * @since 13/09/2021
 */
public class ExternaStoragePluginManager {
  private static final Logger LOG = Logger.getInstance(ExternaStoragePluginManager.class);

  @Nonnull
  public static Map<PluginId, Couple<Boolean>> updatePlugins(@Nonnull ProgressIndicator indicator) {
    try {
      indicator.setTextValue(LocalizeValue.localizeTODO("Updating plugin infos..."));

      List<PluginDescriptor> plugins = PluginManager.getPlugins();
      List<PluginInfoBean> inPlugins = new ArrayList<>();
      for (PluginDescriptor plugin : plugins) {
        inPlugins.add(new PluginInfoBean(plugin.getPluginId().toString(), plugin.isEnabled()));
      }

      PluginInfoBean[] beans = WebServiceApiSender.doPost(WebServiceApi.STORAGE_API, "/plugins/merge", inPlugins, PluginInfoBean[].class);

      assert beans != null;

      // pluginId + [enabled, wantInstall]
      Map<PluginId, Couple<Boolean>> updateList = new LinkedHashMap<>();

      boolean wantAnyInstall = false;

      for (PluginInfoBean bean : beans) {
        PluginId pluginId = PluginId.getId(bean.id);

        PluginDescriptor plugin = PluginManager.findPlugin(pluginId);

        if (plugin == null) {
          wantAnyInstall = true;

          updateList.put(pluginId, Couple.of(bean.enabled, true));
        }
        else {
          if(plugin.isEnabled() == bean.enabled) {
            continue;
          }
          
          updateList.put(pluginId, Couple.of(bean.enabled, false));
        }
      }

      if (updateList.isEmpty()) {
        return Map.of();
      }

      LOG.info("Requesting update: " + updateList);

      if (wantAnyInstall) {
        List<PluginDescriptor> repositoryPlugins = RepositoryHelper.loadOnlyPluginsFromRepository(indicator, UpdateSettings.getInstance().getChannel(), EarlyAccessProgramManager.getInstance());

        Map<PluginId, PluginDescriptor> repo = ContainerUtil.newMapFromValues(repositoryPlugins.iterator(), PluginDescriptor::getPluginId);

        Set<PluginId> unresolvedPlugins = new HashSet<>();

        // validate for new dependencies
        for (Map.Entry<PluginId, Couple<Boolean>> entry : updateList.entrySet()) {
          if (!entry.getValue().getSecond()) {
            continue;
          }

          PluginDescriptor repositoryPlugin = repo.get(entry.getKey());
          if (repositoryPlugin == null) {
            unresolvedPlugins.add(entry.getKey());

            LOG.warn("Unresolved storage plugin: " + entry.getKey());
            continue;
          }

          Set<PluginDescriptor> pluginsForInstall = PluginInstallUtil.getPluginsForInstall(List.of(repositoryPlugin), repositoryPlugins);

          for (PluginDescriptor pluginDescriptor : pluginsForInstall) {
            Couple<Boolean> state = updateList.get(pluginDescriptor.getPluginId());
            if (state == null) {
              updateList.put(pluginDescriptor.getPluginId(), Couple.of(true, true));
            }
          }
        }

        updateList.keySet().removeAll(unresolvedPlugins);

        for (Map.Entry<PluginId, Couple<Boolean>> entry : updateList.entrySet()) {
          if (!entry.getValue().getSecond()) {
            continue;
          }

          PluginDescriptor repositoryPlugin = repo.get(entry.getKey());
          assert repositoryPlugin != null;

          PluginDownloader downloader = PluginDownloader.createDownloader(repositoryPlugin, false);
          indicator.setTextValue(LocalizeValue.localizeTODO("Downloading new plugin '" + repositoryPlugin.getName() + "'..."));
          downloader.prepareToInstall(true, Application.get().getLastUIAccess(), indicator, pluginDownloader -> {
            InstalledPluginsState.getInstance().getInstalledPlugins().add(entry.getKey());
            
            pluginDownloader.install(indicator, true);
          });

        }
      }

      for (Map.Entry<PluginId, Couple<Boolean>> entry : updateList.entrySet()) {
        Couple<Boolean> value = entry.getValue();
        if (value.getFirst()) {
          PluginManager.enablePlugin(entry.getKey().toString());
        }
        else {
          PluginManager.disablePlugin(entry.getKey().toString());
        }
      }

      return updateList;
    }
    catch (Exception e) {
      LOG.warn(e);
    }

    return Map.of();
  }
}
