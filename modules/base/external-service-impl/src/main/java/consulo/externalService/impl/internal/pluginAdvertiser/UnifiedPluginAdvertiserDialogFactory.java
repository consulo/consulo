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
package consulo.externalService.impl.internal.pluginAdvertiser;

import consulo.annotation.component.ComponentProfiles;
import consulo.annotation.component.ServiceImpl;
import consulo.component.extension.preview.ExtensionPreview;
import consulo.container.plugin.PluginDescriptor;
import consulo.externalService.pluginAdvertiser.PluginAdvertiserHelper;
import consulo.project.Project;
import consulo.ui.annotation.RequiredUIAccess;
import jakarta.inject.Singleton;
import org.jspecify.annotations.Nullable;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutionException;

/**
 * @author VISTALL
 * @since 2026-08-13
 */
@Singleton
@ServiceImpl(profiles = ComponentProfiles.UNIFIED)
public class UnifiedPluginAdvertiserDialogFactory implements PluginAdvertiserDialogFactory {
    @Override
    @RequiredUIAccess
    public void showDialog(@Nullable Project project, List<PluginDescriptor> allPlugins, List<PluginDescriptor> toInstallPlugins) {
        PluginAdvertiserHelper.PluginsInfo info =
            new PluginAdvertiserHelper.PluginsInfo(allPlugins, new LinkedHashSet<>(toInstallPlugins));

        new UnifiedPluginsAdvertiserDialog(() -> info).show();
    }

    @Override
    @RequiredUIAccess
    public void showDialogForExtension(ExtensionPreview preview, PluginAdvertiserHelper helper) {
        new UnifiedPluginsAdvertiserDialog(() -> {
            try {
                return helper.findPluginsForSuggest(preview).get();
            }
            catch (InterruptedException | ExecutionException e) {
                return new PluginAdvertiserHelper.PluginsInfo(List.of(), Set.of());
            }
        }).show();
    }
}
