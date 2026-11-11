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
package consulo.externalService.impl.internal.plugin;

import consulo.annotation.component.ExtensionImpl;
import consulo.configurable.search.SearchableOptionContributor;
import consulo.configurable.search.SearchableOptionProcessor;
import consulo.container.plugin.PluginManager;
import consulo.externalService.localize.ExternalServiceLocalize;
import consulo.externalService.plugin.PluginsConfigurable;
import consulo.localize.LocalizeValue;

/**
 * The enabled plugins, found in settings search by their names and descriptions - on the Plugins page.
 *
 * @author VISTALL
 * @since 2026-11-11
 */
@ExtensionImpl
public class PluginSearchableOptionContributor extends SearchableOptionContributor {
    @Override
    public void processOptions(SearchableOptionProcessor processor) {
        PluginManager.forEachEnabledPlugin(plugin -> {
            LocalizeValue name = LocalizeValue.of(plugin.getName());
            processor.addOption(name, name, PluginsConfigurable.CONFIGURABLE_ID, ExternalServiceLocalize.titlePlugins());
            String description = plugin.getDescription();
            if (description != null) {
                processor.addOption(LocalizeValue.of(description), name, PluginsConfigurable.CONFIGURABLE_ID, ExternalServiceLocalize.titlePlugins());
            }
        });
    }
}
