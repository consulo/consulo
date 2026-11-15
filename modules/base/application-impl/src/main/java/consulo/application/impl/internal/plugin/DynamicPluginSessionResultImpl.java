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
package consulo.application.impl.internal.plugin;

import consulo.application.internal.plugin.DynamicPluginSessionResult;
import consulo.container.plugin.PluginDescriptor;
import consulo.container.plugin.PluginId;

import java.util.List;
import java.util.Set;

/**
 * @author VISTALL
 * @since 2026-11-13
 */
public class DynamicPluginSessionResultImpl implements DynamicPluginSessionResult {
    private final List<PluginDescriptor> myLoaded;
    private final Set<PluginId> myUninstalled;
    private final Set<PluginId> myRestartRequired;

    public DynamicPluginSessionResultImpl(List<PluginDescriptor> loaded, Set<PluginId> uninstalled, Set<PluginId> restartRequired) {
        myLoaded = List.copyOf(loaded);
        myUninstalled = Set.copyOf(uninstalled);
        myRestartRequired = Set.copyOf(restartRequired);
    }

    @Override
    public List<PluginDescriptor> getLoaded() {
        return myLoaded;
    }

    @Override
    public Set<PluginId> getUninstalled() {
        return myUninstalled;
    }

    @Override
    public Set<PluginId> getRestartRequired() {
        return myRestartRequired;
    }
}
