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

import consulo.application.eap.EarlyAccessProgramManager;
import consulo.container.plugin.PluginDescriptor;
import consulo.disposer.Disposable;
import consulo.externalService.impl.internal.repository.RepositoryHelper;
import consulo.externalService.update.UpdateSettings;
import consulo.logging.Logger;
import consulo.ui.Component;
import consulo.ui.Space;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.layout.DockLayout;
import consulo.ui.layout.LoadingLayout;

import java.util.List;

/**
 * @author VISTALL
 * @since 2026-08-13
 */
public class UnifiedRepositoryPluginsTab extends UnifiedPluginTab {
    private static final Logger LOG = Logger.getInstance(UnifiedRepositoryPluginsTab.class);

    private final LoadingLayout<DockLayout> myLoadingLayout;

    @RequiredUIAccess
    public UnifiedRepositoryPluginsTab(Disposable parentDisposable) {
        super(false);
        myLoadingLayout = LoadingLayout.create(DockLayout.create(Space.NONE), parentDisposable);
    }

    @Override
    public Component getComponent() {
        return myLoadingLayout;
    }

    @Override
    @RequiredUIAccess
    public void reload() {
        // the unified settings dialog does not open a ConfigurableSession yet, so the application instance is
        // read directly - the list just does not follow an unapplied eap toggle of the same dialog
        EarlyAccessProgramManager earlyAccessProgramManager = EarlyAccessProgramManager.getInstance();

        myLoadingLayout.startLoading(
            () -> {
                try {
                    return RepositoryHelper.loadOnlyPluginsFromRepository(
                        null,
                        UpdateSettings.getInstance().getChannel(),
                        earlyAccessProgramManager
                    );
                }
                catch (Exception e) {
                    LOG.warn(e);
                    return List.<PluginDescriptor>of();
                }
            },
            // startLoading empties the host layout, so the content is put back rather than assumed to
            // still hang there
            (layout, plugins) -> {
                setPlugins(plugins);
                layout.center(getContent());
            }
        );
    }
}
