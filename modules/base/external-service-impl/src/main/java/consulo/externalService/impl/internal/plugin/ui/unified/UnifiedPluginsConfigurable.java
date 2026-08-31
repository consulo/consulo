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
package consulo.externalService.impl.internal.plugin.ui.unified;

import consulo.annotation.component.ComponentProfiles;
import consulo.annotation.component.ExtensionImpl;
import consulo.configurable.ApplicationConfigurable;
import consulo.configurable.Configurable;
import consulo.configurable.ConfigurationException;
import consulo.configurable.SearchableConfigurable;
import consulo.container.plugin.PluginId;
import consulo.disposer.Disposable;
import consulo.disposer.Disposer;
import consulo.externalService.localize.ExternalServiceLocalize;
import consulo.externalService.plugin.PluginsConfigurable;
import consulo.localize.LocalizeValue;
import consulo.ui.Component;
import consulo.ui.annotation.RequiredUIAccess;
import jakarta.inject.Inject;
import org.jspecify.annotations.Nullable;

/**
 * @author VISTALL
 * @since 2026-08-13
 */
@ExtensionImpl(profiles = ComponentProfiles.UNIFIED)
public class UnifiedPluginsConfigurable implements SearchableConfigurable,
    Configurable.NoScroll,
    Configurable.NoMargin,
    ApplicationConfigurable,
    PluginsConfigurable {

    private UnifiedPluginsPanel myPanel;

    @Inject
    public UnifiedPluginsConfigurable() {
    }

    @Override
    @RequiredUIAccess
    public @Nullable Component createUIComponent(Disposable parentDisposable) {
        // built anew per dialog - the settings dialog does not promise disposeUIResources on every close, and a
        // kept panel would come back with the filter and the selection of the previous session
        UnifiedPluginsPanel panel = new UnifiedPluginsPanel();
        myPanel = panel;

        Disposer.register(parentDisposable, panel);
        Disposer.register(parentDisposable, () -> {
            if (myPanel == panel) {
                myPanel = null;
            }
        });

        return panel.getComponent();
    }

    @Override
    public LocalizeValue getDisplayName() {
        return ExternalServiceLocalize.titlePlugins();
    }

    @Override
    public String getId() {
        return CONFIGURABLE_ID;
    }

    @Override
    @RequiredUIAccess
    public boolean isModified() {
        return false;
    }

    @Override
    @RequiredUIAccess
    public void apply() throws ConfigurationException {
    }

    @Override
    @RequiredUIAccess
    public void disposeUIResources() {
        myPanel = null;
    }

    @Override
    @RequiredUIAccess
    public void selectInstalled(PluginId pluginId) {
        if (myPanel != null) {
            myPanel.selectInstalled(pluginId);
        }
    }
}
