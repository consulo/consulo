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

import consulo.application.internal.plugin.DynamicPluginSession;
import consulo.application.internal.plugin.DynamicPluginSessionResult;
import consulo.container.plugin.PluginId;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * @author VISTALL
 * @since 2026-11-13
 */
public class DynamicPluginSessionImpl implements DynamicPluginSession {
    private final DynamicPluginManagerImpl myManager;
    private final List<Path> myInstallPaths = new ArrayList<>();
    private final Set<PluginId> myUninstallIds = new LinkedHashSet<>();

    public DynamicPluginSessionImpl(DynamicPluginManagerImpl manager) {
        myManager = manager;
    }

    @Override
    public DynamicPluginSession install(Path pluginPath) {
        myInstallPaths.add(pluginPath);
        return this;
    }

    @Override
    public DynamicPluginSession uninstall(PluginId pluginId) {
        myUninstallIds.add(pluginId);
        return this;
    }

    @Override
    public DynamicPluginSessionResult apply() {
        return myManager.apply(myInstallPaths, myUninstallIds);
    }
}
