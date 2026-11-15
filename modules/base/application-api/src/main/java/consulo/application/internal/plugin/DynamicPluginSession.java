/*
 * Copyright 2013-2026 consulo.io
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
package consulo.application.internal.plugin;

import consulo.container.plugin.PluginId;

import java.nio.file.Path;

/**
 * Collects plugins to install and uninstall, then applies them together: dependents are unloaded and loaded once,
 * and listeners see one {@link consulo.application.plugin.DynamicPluginChange}.
 *
 * @author VISTALL
 * @since 2026-11-13
 */
public interface DynamicPluginSession {
    /**
     * Adds a plugin unpacked into the plugins directory during this run.
     */
    DynamicPluginSession install(Path pluginPath);

    /**
     * Adds a plugin to unload, its directory is deleted.
     */
    DynamicPluginSession uninstall(PluginId pluginId);

    /**
     * Applies the collected changes. Plugins which need a restart are left as they are: an installed one stays in its
     * directory and loads with the next start, an uninstalled one is not touched.
     */
    DynamicPluginSessionResult apply();
}
