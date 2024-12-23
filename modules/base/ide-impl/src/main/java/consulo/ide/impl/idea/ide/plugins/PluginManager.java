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
import consulo.application.impl.internal.start.StartupUtil;
import consulo.container.plugin.PluginDescriptor;
import consulo.container.plugin.PluginId;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.io.File;

/**
 * @author mike
 */
@Deprecated
@DeprecationInfo("Use consulo.container.plugin.PluginManager")
public class PluginManager extends PluginManagerCore {
    public static boolean isPluginInstalled(PluginId id) {
        return consulo.container.plugin.PluginManager.findPlugin(id) != null;
    }

    @Nullable
    public static PluginDescriptor getPlugin(PluginId id) {
        return consulo.container.plugin.PluginManager.findPlugin(id);
    }

    @Nullable
    @Deprecated
    public static File getPluginPath(@Nonnull Class<?> pluginClass) {
        return consulo.container.plugin.PluginManager.getPluginPath(pluginClass);
    }

    @Deprecated
    public static void handleComponentError(@Nonnull Throwable t, @Nullable Class componentClass, @Nullable Object config) {
        StartupUtil.handleComponentError(t, componentClass, config);
    }
}
