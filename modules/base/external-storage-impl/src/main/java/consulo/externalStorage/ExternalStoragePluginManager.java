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
import consulo.container.plugin.PluginIds;
import consulo.container.plugin.PluginManager;
import consulo.externalService.ExternalService;
import consulo.externalService.ExternalServiceConfiguration;
import consulo.externalService.impl.WebServiceApi;
import consulo.externalService.impl.WebServiceApiSender;
import consulo.externalStorage.plugin.StoragePlugin;
import consulo.externalStorage.plugin.StoragePluginState;
import consulo.ide.eap.EarlyAccessProgramManager;
import consulo.ide.plugins.InstalledPluginsState;
import consulo.ide.plugins.PluginActionListener;
import consulo.ide.updateSettings.UpdateSettings;
import consulo.localize.LocalizeValue;
import consulo.logging.Logger;
import consulo.util.lang.ThreeState;

import javax.annotation.Nonnull;
import java.util.*;

/**
 * @author VISTALL
 * @since 13/09/2021
 */
public class ExternalStoragePluginManager implements PluginActionListener {
  private static class PluginActionInfo {
    private boolean enabled;

    // YES - install
    // UNSURE - left as is
    // NO - uninstall
    private ThreeState installOrUninstall;

    private PluginActionInfo(boolean enabled, ThreeState installOrUninstall) {
      this.enabled = enabled;
      this.installOrUninstall = installOrUninstall;
    }

    @Override
    public String toString() {
      return "PluginActionInfo{" + "enabled=" + enabled + ", installOrUninstall=" + installOrUninstall + '}';
    }
  }

  private static final Logger LOG = Logger.getInstance(ExternalStoragePluginManager.class);

  private final ExternalServiceConfiguration myExternalServiceConfiguration;

  public ExternalStoragePluginManager(Application application, ExternalServiceConfiguration externalServiceConfiguration) {
    myExternalServiceConfiguration = externalServiceConfiguration;
    application.getMessageBus().connect().subscribe(PluginActionListener.TOPIC, this);
  }

  @Override
  public void pluginInstalled(@Nonnull PluginId pluginId) {
    sendAction("/plugins/add", pluginId, StoragePluginState.ENABLED);
  }

  @Override
  public void pluginUninstalled(@Nonnull PluginId pluginId) {
    sendAction("/plugins/delete", pluginId, StoragePluginState.UNINSTALLED);
  }

  private void sendAction(String action, PluginId pluginId, StoragePluginState state) {
    try {
      if (myExternalServiceConfiguration.getState(ExternalService.STORAGE) != ThreeState.YES) {
        return;
      }

      List<StoragePlugin> inPlugins = List.of(new StoragePlugin(pluginId.toString(), state));

      WebServiceApiSender.doPost(WebServiceApi.STORAGE_API, action, inPlugins, StoragePlugin[].class);
    }
    catch (Exception e) {
      LOG.warn(e);
    }
  }

  private static boolean toEnabledState(StoragePluginState state) {
    switch (state) {
      case ENABLED:
        return true;
      case DISABLED:
        return false;
      default:
        throw new IllegalArgumentException(state.name());
    }
  }

  /**
   * Return true if restart required
   */
  public boolean updatePlugins(@Nonnull ProgressIndicator indicator) {
    try {
      indicator.setTextValue(LocalizeValue.localizeTODO("Checking plugins state..."));

      List<PluginDescriptor> plugins = PluginManager.getPlugins();
      List<StoragePlugin> inPlugins = new ArrayList<>();
      for (PluginDescriptor plugin : plugins) {
        // skip platform plugins
        if(PluginIds.isPlatformPlugin(plugin.getPluginId())) {
          continue;
        }

        inPlugins.add(new StoragePlugin(plugin.getPluginId().toString(), plugin.isEnabled() ? StoragePluginState.ENABLED : StoragePluginState.DISABLED));
      }

      StoragePlugin[] beans = WebServiceApiSender.doPost(WebServiceApi.STORAGE_API, "/plugins/merge", inPlugins, StoragePlugin[].class);

      assert beans != null;

      Map<PluginId, PluginActionInfo> pluginActions = new LinkedHashMap<>();

      boolean wantAnyInstall = false;

      for (StoragePlugin bean : beans) {
        PluginId pluginId = PluginId.getId(bean.id);
        PluginDescriptor plugin = PluginManager.findPlugin(pluginId);

        switch (bean.state) {
          case UNINSTALLED:
            if (plugin != null) {
              pluginActions.put(pluginId, new PluginActionInfo(false, ThreeState.NO));
            }
            break;
          case ENABLED:
          case DISABLED:
            if (plugin == null) {
              if(InstalledPluginsState.getInstance().getInstalledPlugins().contains(pluginId)) {
                if (bean.state == StoragePluginState.ENABLED) {
                  PluginManager.enablePlugin(pluginId.toString());
                }
                else {
                  PluginManager.disablePlugin(pluginId.toString());
                }
                continue;
              }

              wantAnyInstall = true;

              pluginActions.put(pluginId, new PluginActionInfo(bean.state == StoragePluginState.ENABLED, ThreeState.YES));
            }
            else {
              if (plugin.isEnabled() == toEnabledState(bean.state)) {
                continue;
              }

              pluginActions.put(pluginId, new PluginActionInfo(bean.state == StoragePluginState.ENABLED, ThreeState.UNSURE));
            }
            break;
        }
      }

      if (pluginActions.isEmpty()) {
        return false;
      }

      LOG.info("Requesting update: " + pluginActions);

      if (wantAnyInstall) {
        List<PluginDescriptor> repositoryPlugins = RepositoryHelper.loadOnlyPluginsFromRepository(indicator, UpdateSettings.getInstance().getChannel(), EarlyAccessProgramManager.getInstance());

        Map<PluginId, PluginDescriptor> repo = ContainerUtil.newMapFromValues(repositoryPlugins.iterator(), PluginDescriptor::getPluginId);

        Set<PluginId> unresolvedPlugins = new HashSet<>();

        // validate for new dependencies
        for (Map.Entry<PluginId, PluginActionInfo> entry : pluginActions.entrySet()) {
          if (entry.getValue().installOrUninstall != ThreeState.YES) {
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
            PluginActionInfo state = pluginActions.get(pluginDescriptor.getPluginId());
            if (state == null) {
              pluginActions.put(pluginDescriptor.getPluginId(), new PluginActionInfo(true, ThreeState.YES));
            }
          }
        }

        pluginActions.keySet().removeAll(unresolvedPlugins);

        for (Map.Entry<PluginId, PluginActionInfo> entry : pluginActions.entrySet()) {
          ThreeState installOrUninstall = entry.getValue().installOrUninstall;
          boolean pluginEnabled = entry.getValue().enabled;
          PluginId pluginId = entry.getKey();

          switch (installOrUninstall) {
            case YES:
              PluginDescriptor repositoryPlugin = repo.get(pluginId);
              assert repositoryPlugin != null;

              PluginDownloader downloader = PluginDownloader.createDownloader(repositoryPlugin, false);
              indicator.setTextValue(LocalizeValue.localizeTODO("Downloading new plugin '" + repositoryPlugin.getName() + "'..."));
              downloader.download(indicator);

              InstalledPluginsState.getInstance().getInstalledPlugins().add(pluginId);
              downloader.install(indicator, true);

              if (pluginEnabled) {
                PluginManager.enablePlugin(entry.getKey().toString());
              }
              else {
                PluginManager.disablePlugin(entry.getKey().toString());
              }
              break;
            case NO:
              PluginInstallUtil.prepareToUninstall(pluginId);
              break;
          }
        }
      }

      return !pluginActions.isEmpty();
    }
    catch (Exception e) {
      LOG.warn(e);
    }

    return false;
  }
}
