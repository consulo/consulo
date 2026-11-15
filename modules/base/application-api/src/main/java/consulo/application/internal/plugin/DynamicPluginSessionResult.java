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

import consulo.container.plugin.PluginDescriptor;
import consulo.container.plugin.PluginId;

import java.util.List;
import java.util.Set;

/**
 * @author VISTALL
 * @since 2026-11-13
 */
public interface DynamicPluginSessionResult {
    /**
     * @return installed plugins which are loaded now
     */
    List<PluginDescriptor> getLoaded();

    /**
     * @return uninstalled plugins which are unloaded and deleted now
     */
    Set<PluginId> getUninstalled();

    /**
     * @return installed or uninstalled plugins which need a restart
     */
    Set<PluginId> getRestartRequired();
}
