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
package consulo.externalService.impl.internal.pluginAdvertiser.awt;

import consulo.annotation.component.ComponentProfiles;
import consulo.annotation.component.ServiceImpl;
import consulo.component.extension.preview.ExtensionPreview;
import consulo.container.plugin.PluginDescriptor;
import consulo.externalService.impl.internal.pluginAdvertiser.PluginAdvertiserDialogFactory;
import consulo.externalService.pluginAdvertiser.PluginAdvertiserHelper;
import consulo.project.Project;
import consulo.ui.annotation.RequiredUIAccess;
import jakarta.inject.Singleton;
import org.jspecify.annotations.Nullable;

import java.util.List;

/**
 * @author VISTALL
 * @since 2026-08-13
 */
@Singleton
@ServiceImpl(profiles = ComponentProfiles.AWT)
public class AWTPluginAdvertiserDialogFactory implements PluginAdvertiserDialogFactory {
    @Override
    @RequiredUIAccess
    public void showDialog(@Nullable Project project, List<PluginDescriptor> allPlugins, List<PluginDescriptor> toInstallPlugins) {
        new PreloadedPluginsAdvertiserDialog(project, allPlugins, toInstallPlugins).showAsync();
    }

    @Override
    @RequiredUIAccess
    public void showDialogForExtension(ExtensionPreview preview, PluginAdvertiserHelper helper) {
        new WaitingPluginsAdvertiserDialog(null, preview, helper).showAsync();
    }
}
