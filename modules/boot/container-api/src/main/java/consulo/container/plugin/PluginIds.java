/*
 * Copyright 2013-2019 consulo.io
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
package consulo.container.plugin;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * @author VISTALL
 * @since 2019-07-16
 */
public final class PluginIds {
  public static final PluginId CONSULO_BASE = PluginId.getId("consulo");

  public static final PluginId CONSULO_DESKTOP_AWT = PluginId.getId("consulo.desktop.awt");

  public static final PluginId CONSULO_DESKTOP_SWT = PluginId.getId("consulo.desktop.swt");

  public static final PluginId CONSULO_WEB = PluginId.getId("consulo.web");

  /**
   * ID of repo analyzer, since it's running without AWT/SWT/Web dependencies
   */
  private static final PluginId CONSULO_REPO_ANALYZER = PluginId.getId("consulo.repo.analyzer");

  private static final Set<PluginId> ourMergedObsoletePlugins =
    new HashSet<PluginId>(Arrays.asList(PluginId.getId("org.intellij.intelliLang"),
                                        PluginId.getId("consulo.presentationAssistant")));

  private static final Set<PluginId> ourPlatformIds =
    new HashSet<PluginId>(Arrays.asList(CONSULO_DESKTOP_AWT, CONSULO_DESKTOP_SWT, CONSULO_WEB, CONSULO_REPO_ANALYZER));

  public static boolean isPlatformImplementationPlugin(PluginId pluginId) {
    return ourPlatformIds.contains(pluginId);
  }

  public static boolean isPlatformPlugin(PluginId pluginId) {
    return CONSULO_BASE.equals(pluginId) || isPlatformImplementationPlugin(pluginId);
  }

  public static Set<PluginId> getObsoletePlugins() {
    return ourMergedObsoletePlugins;
  }
}
