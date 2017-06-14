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

import com.intellij.openapi.application.Application;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.extensions.PluginId;
import org.jetbrains.annotations.NotNull;

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
public class InstalledPluginsState {
  @NotNull
  public static InstalledPluginsState getInstance() {
    Application application = ApplicationManager.getApplication();
    return application == null ? new InstalledPluginsState() : ServiceManager.getService(InstalledPluginsState.class);
  }

  private Set<PluginId> myOutdatedPlugins = new TreeSet<>();
  private Set<PluginId> myInstalledPlugins = new TreeSet<>();

  public Set<PluginId> getInstalledPlugins() {
    return myInstalledPlugins;
  }

  public Set<PluginId> getOutdatedPlugins() {
    return myOutdatedPlugins;
  }
}
