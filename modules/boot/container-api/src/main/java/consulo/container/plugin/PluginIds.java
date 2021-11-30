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

import javax.annotation.Nonnull;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * @author VISTALL
 * @since 2019-07-16
 */
public final class PluginIds {
  // TODO [VISTALL] rename later. consulo.platform.base
  public static final PluginId CONSULO_PLATFORM_BASE = PluginId.getId("com.intellij");

  public static final PluginId CONSULO_PLATFORM_DESKTOP = PluginId.getId("consulo.platform.desktop");

  public static final PluginId CONSULO_PLATFORM_DESKTOP_SWT = PluginId.getId("consulo.platform.desktop.swt");

  public static final PluginId CONSULO_PLATFORM_WEB = PluginId.getId("consulo.platform.web");

  private static final Set<PluginId> ourMergedObsoletePlugins = new HashSet<PluginId>(Arrays.asList(PluginId.getId("org.intellij.intelliLang")));

  private static final Set<PluginId> ourPlatformIds = new HashSet<PluginId>(Arrays.asList(CONSULO_PLATFORM_DESKTOP, CONSULO_PLATFORM_DESKTOP_SWT, CONSULO_PLATFORM_WEB));

  public static boolean isPlatformImplementationPlugin(@Nonnull PluginId pluginId) {
    return ourPlatformIds.contains(pluginId);
  }

  public static boolean isPlatformPlugin(@Nonnull PluginId pluginId) {
    return CONSULO_PLATFORM_BASE.equals(pluginId) || isPlatformImplementationPlugin(pluginId);
  }

  @Nonnull
  public static Set<PluginId> getObsoletePlugins() {
    return ourMergedObsoletePlugins;
  }
}
