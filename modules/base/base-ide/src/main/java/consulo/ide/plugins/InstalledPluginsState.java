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
package consulo.ide.plugins;

import com.intellij.ide.plugins.PluginManager;
import com.intellij.openapi.application.Application;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.ServiceManager;
import consulo.container.plugin.PluginId;
import com.intellij.openapi.util.text.StringUtil;
import consulo.container.plugin.PluginDescriptor;
import jakarta.inject.Singleton;

import javax.annotation.Nonnull;
import java.util.HashSet;
import java.util.Set;
import java.util.TreeSet;

/**
 * @author VISTALL
 * @since 14-Jun-17
 * <p>
 * Main idea from IDEA version com.intellij.ide.plugins.InstalledPluginsState
 * <p>
 * A service to hold a state of plugin changes in a current session (i.e. before the changes are applied on restart).
 */
@Singleton
public class InstalledPluginsState {
  @Nonnull
  public static InstalledPluginsState getInstance() {
    Application application = ApplicationManager.getApplication();
    if (application == null) {
      throw new IllegalArgumentException("app is not loaded");
    }
    return ServiceManager.getService(InstalledPluginsState.class);
  }

  private Set<PluginId> myOutdatedPlugins = new TreeSet<>();
  private Set<PluginId> myInstalledPlugins = new TreeSet<>();
  private Set<PluginId> myUpdatedPlugins = new HashSet<>();

  private Set<PluginId> myNewVersions = new HashSet<>();

  private final Set<PluginDescriptor> myAllPlugins = new HashSet<>();

  public void updateExistingPlugin(@Nonnull PluginDescriptor descriptor, @Nonnull PluginDescriptor installed) {
    updateExistingPluginInfo(descriptor, installed);
    myUpdatedPlugins.add(installed.getPluginId());
  }

  public void updateExistingPluginInfo(PluginDescriptor descr, PluginDescriptor existing) {
    int state = StringUtil.compareVersionNumbers(descr.getVersion(), existing.getVersion());
    final PluginId pluginId = existing.getPluginId();
    final Set<PluginId> installedPlugins = InstalledPluginsState.getInstance().getInstalledPlugins();
    if (!installedPlugins.contains(pluginId) && !existing.isDeleted()) {
      installedPlugins.add(pluginId);
    }
    if (state > 0 && !PluginManager.isIncompatible(descr) && !myUpdatedPlugins.contains(descr.getPluginId())) {
      myNewVersions.add(pluginId);

      myOutdatedPlugins.add(pluginId);
    }
    else {
      myOutdatedPlugins.remove(pluginId);

      if (myNewVersions.remove(pluginId)) {
        myUpdatedPlugins.add(pluginId);
      }
    }
  }

  public boolean hasNewerVersion(PluginId descr) {
    return !wasUpdated(descr) && (myNewVersions.contains(descr) || myOutdatedPlugins.contains(descr));
  }

  public boolean wasUpdated(PluginId descr) {
    return myUpdatedPlugins.contains(descr);
  }

  public Set<PluginId> getUpdatedPlugins() {
    return myUpdatedPlugins;
  }

  public Set<PluginId> getInstalledPlugins() {
    return myInstalledPlugins;
  }

  public Set<PluginId> getOutdatedPlugins() {
    return myOutdatedPlugins;
  }

  public Set<PluginDescriptor> getAllPlugins() {
    return myAllPlugins;
  }
}
