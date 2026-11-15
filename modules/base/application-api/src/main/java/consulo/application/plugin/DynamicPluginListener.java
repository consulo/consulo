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
package consulo.application.plugin;

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.TopicAPI;
import consulo.container.plugin.PluginDescriptor;

/**
 * Plugins loaded or unloaded while the application runs, without a restart.
 * <p>
 * A session loads and unloads several plugins at once - a plugin with its dependencies, or the dependents of an
 * uninstalled one. The per-plugin callbacks only drop or pick up what belongs to the plugin; anything which refreshes the
 * whole ide - restarting the daemon, announcing changed settings, file types or roots, reloading a state - belongs to
 * {@link #pluginsChanged}, which runs once per session.
 *
 * @author VISTALL
 * @since 2026-11-12
 */
@TopicAPI(ComponentScope.APPLICATION)
public interface DynamicPluginListener {
    /**
     * Called once before a session unloads and loads plugins, before any {@link #beforePluginUnload} of it.
     */
    default void beforePluginsChange(DynamicPluginChange change) {
    }

    /**
     * Called once after a session unloaded and loaded its plugins, after every {@link #pluginLoaded} of it.
     */
    default void pluginsChanged(DynamicPluginChange change) {
    }

    /**
     * Called after the plugin extensions, texts and icons are available.
     */
    default void pluginLoaded(PluginDescriptor pluginDescriptor) {
    }

    /**
     * Called before the plugin is unloaded, while its classes, extensions and services are still available.
     */
    default void beforePluginUnload(PluginDescriptor pluginDescriptor) {
    }

    /**
     * Called after the plugin extensions, services and actions were removed.
     */
    default void pluginUnloaded(PluginDescriptor pluginDescriptor) {
    }
}
