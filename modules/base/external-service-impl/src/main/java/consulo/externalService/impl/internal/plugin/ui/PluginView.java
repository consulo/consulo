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
package consulo.externalService.impl.internal.plugin.ui;

import consulo.container.plugin.PluginDescriptor;
import consulo.container.plugin.PluginDescriptorStatus;
import consulo.container.plugin.PluginManager;
import consulo.externalService.impl.internal.plugin.InstalledPluginsState;
import consulo.localize.LocalizeValue;

/**
 * Which plugins the list of the plugins page shows.
 *
 * @author VISTALL
 * @since 2026-11-14
 */
public enum PluginView {
    ALL(LocalizeValue.localizeTODO("All")),
    INSTALLED(LocalizeValue.localizeTODO("Installed")),
    UPDATES(LocalizeValue.localizeTODO("Updates")),
    NOT_LOADED(LocalizeValue.localizeTODO("Not Loaded"));

    private final LocalizeValue myName;

    PluginView(LocalizeValue name) {
        myName = name;
    }

    public LocalizeValue getName() {
        return myName;
    }

    public boolean accepts(PluginDescriptor descriptor) {
        PluginDescriptor installed = PluginManager.findPlugin(descriptor.getPluginId());
        return switch (this) {
            case ALL -> true;
            case INSTALLED -> installed != null;
            case UPDATES -> installed != null && InstalledPluginsState.getInstance().hasNewerVersion(descriptor.getPluginId());
            case NOT_LOADED -> installed != null && installed.getStatus() != PluginDescriptorStatus.OK;
        };
    }
}
