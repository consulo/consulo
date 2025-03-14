/*
 * Copyright 2000-2013 JetBrains s.r.o.
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

import consulo.annotation.DeprecationInfo;
import consulo.application.internal.plugin.PluginsLoader;
import consulo.application.internal.StartupProgress;
import consulo.container.boot.ContainerPathManager;
import consulo.container.classloader.PluginClassLoader;
import consulo.container.internal.plugin.PluginDescriptorImpl;
import consulo.container.internal.plugin.PluginDescriptorLoader;
import consulo.container.plugin.PluginDescriptor;
import consulo.container.plugin.PluginId;
import consulo.container.plugin.PluginIds;
import consulo.container.plugin.PluginManager;
import consulo.util.lang.StringUtil;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

@Deprecated
@DeprecationInfo("Use consulo.container.plugin.PluginManager")
public class PluginManagerCore {
    public static final PluginId CORE_PLUGIN = PluginIds.CONSULO_BASE;
    public static final String CORE_PLUGIN_ID = CORE_PLUGIN.getIdString();

    private static final float PLUGINS_PROGRESS_MAX_VALUE = 0.3f;

    @Nonnull
    @Deprecated
    @DeprecationInfo("Use consulo.container.plugin.PluginManager#getPlugins()")
    @SuppressWarnings("deprecation")
    public static PluginDescriptor[] getPlugins() {
        List<PluginDescriptor> plugins = PluginManager.getPlugins();
        PluginDescriptor[] array = new PluginDescriptor[plugins.size()];
        for (int i = 0; i < plugins.size(); i++) {
            PluginDescriptor pluginDescriptor = plugins.get(i);
            array[i] = pluginDescriptor;
        }
        return array;
    }

    @Nullable
    public static PluginId getPluginId(@Nonnull Class<?> clazz) {
        ClassLoader loader = clazz.getClassLoader();
        if (!(loader instanceof PluginClassLoader)) {
            return null;
        }
        return ((PluginClassLoader) loader).getPluginId();
    }

    @Nullable
    @Deprecated
    @DeprecationInfo("Must be never used from plugin or platform")
    public static PluginDescriptor loadPluginDescriptor(File file) {
        return PluginDescriptorLoader.loadDescriptor(file, false, PluginsLoader.C_LOG);
    }

    @Deprecated
    @DeprecationInfo("Must be never used from plugin or platform")
    public static void loadDescriptors(String pluginsPath, List<PluginDescriptorImpl> result, @Nullable StartupProgress progress, int pluginsCount, boolean isHeadlessMode, boolean isPreInstalledPath) {
        loadDescriptors(new File(pluginsPath), result, progress, pluginsCount, isHeadlessMode, isPreInstalledPath);
    }

    @Deprecated
    @DeprecationInfo("Must be never used from plugin or platform")
    public static void loadDescriptors(@Nonnull File pluginsHome,
                                       List<PluginDescriptorImpl> result,
                                       @Nullable StartupProgress progress,
                                       int pluginsCount,
                                       boolean isHeadlessMode,
                                       boolean isPreInstalledPath) {
        final File[] files = pluginsHome.listFiles();
        if (files != null) {
            int i = result.size();
            for (File file : files) {
                final PluginDescriptorImpl descriptor = PluginDescriptorLoader.loadDescriptor(file, isPreInstalledPath, PluginsLoader.C_LOG);
                if (descriptor == null) {
                    continue;
                }
                if (progress != null) {
                    progress.showProgress(descriptor.getName(), PLUGINS_PROGRESS_MAX_VALUE * ((float) ++i / pluginsCount));
                }
                int oldIndex = result.indexOf(descriptor);
                if (oldIndex >= 0) {
                    final PluginDescriptorImpl oldDescriptor = result.get(oldIndex);
                    if (StringUtil.compareVersionNumbers(oldDescriptor.getVersion(), descriptor.getVersion()) < 0) {
                        result.set(oldIndex, descriptor);
                    }
                }
                else {
                    result.add(descriptor);
                }
            }
        }
    }

    @Nonnull
    @Deprecated
    @DeprecationInfo("Must be never used from plugin or platform")
    public static List<PluginDescriptorImpl> loadDescriptorsFromPluginPath(@Nullable StartupProgress progress, boolean isHeadlessMode) {
        final List<PluginDescriptorImpl> result = new ArrayList<>();

        int pluginsCount = 0;
        String[] pluginsPaths = ContainerPathManager.get().getPluginsPaths();
        for (String pluginsPath : pluginsPaths) {
            pluginsCount += countPlugins(pluginsPath);
        }

        for (String pluginsPath : pluginsPaths) {
            loadDescriptors(pluginsPath, result, progress, pluginsCount, isHeadlessMode, false);
        }

        return result;
    }

    private static int countPlugins(String pluginsPath) {
        File configuredPluginsDir = new File(pluginsPath);
        if (configuredPluginsDir.exists()) {
            String[] list = configuredPluginsDir.list();
            if (list != null) {
                return list.length;
            }
        }
        return 0;
    }
}
