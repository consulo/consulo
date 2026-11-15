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

import consulo.container.plugin.PluginDescriptor;

import java.util.List;

/**
 * One applied install/uninstall session: plugins unloaded and loaded together.
 *
 * @author VISTALL
 * @since 2026-11-13
 */
public interface DynamicPluginChange {
    /**
     * @return loaded plugins which the session unloads, dependents first; plugins reloaded by the session are here too
     */
    List<PluginDescriptor> getUnloading();

    /**
     * @return plugins which the session loads, dependencies first, as read from their directories; plugins reloaded by
     * the session are here too
     */
    List<PluginDescriptor> getLoading();
}
