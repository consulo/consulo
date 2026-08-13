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

import consulo.application.internal.start.StartupActionScriptManager;
import consulo.container.internal.PluginValidator;
import consulo.container.plugin.PluginDescriptor;
import consulo.container.plugin.PluginManager;
import consulo.externalService.impl.internal.plugin.InstalledPluginsState;
import consulo.externalService.impl.internal.plugin.ui.InstalledPluginsTab;
import consulo.externalService.impl.internal.update.PluginDownloader;
import consulo.fileChooser.FileChooser;
import consulo.fileChooser.FileChooserDescriptor;
import consulo.localize.LocalizeValue;
import consulo.platform.base.icon.PlatformIconGroup;
import consulo.ui.Alerts;
import consulo.ui.UIAccess;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.action.AnActionEvent;
import consulo.ui.ex.action.DumbAwareAction;
import consulo.virtualFileSystem.VirtualFile;
import consulo.virtualFileSystem.util.VirtualFileUtil;

import java.io.File;
import java.io.IOException;
import java.util.Objects;

/**
 * @author VISTALL
 * @since 2026-08-13
 */
public class UnifiedInstallPluginFromDiskAction extends DumbAwareAction {
    private final UnifiedPluginsPanel myPanel;

    public UnifiedInstallPluginFromDiskAction(UnifiedPluginsPanel panel) {
        super("Install plugin from disk...", null, PlatformIconGroup.nodesPlugin());
        myPanel = panel;
    }

    @Override
    @RequiredUIAccess
    public void actionPerformed(AnActionEvent e) {
        UIAccess uiAccess = UIAccess.current();

        FileChooserDescriptor descriptor = new FileChooserDescriptor(false, false, true, true, false, false) {
            @RequiredUIAccess
            @Override
            public boolean isFileSelectable(VirtualFile file) {
                return Objects.equals(file.getExtension(), PluginManager.CONSULO_PLUGIN_EXTENSION);
            }
        };
        descriptor.setTitle("Choose Plugin File");
        descriptor.setDescription("'consulo-plugin' files are accepted");

        // the parent of the chooser is an awt component, which a unified page has none of
        FileChooser.chooseFile(descriptor, null, null).whenComplete((value, error) -> {
            if (error == null && value != null) {
                uiAccess.give(() -> onFileSelect(value));
            }
        });
    }

    @RequiredUIAccess
    private void onFileSelect(VirtualFile virtualFile) {
        File file = VirtualFileUtil.virtualToIoFile(virtualFile);

        PluginDescriptor pluginDescriptor;
        try {
            pluginDescriptor = InstalledPluginsTab.loadDescriptorFromArchive(file);
        }
        catch (IOException ex) {
            Alerts.okError(LocalizeValue.of(ex.getMessage())).showAsync();
            return;
        }

        if (pluginDescriptor == null) {
            Alerts.okError(LocalizeValue.of("Fail to load plugin descriptor from file " + file.getName())).showAsync();
            return;
        }

        if (PluginValidator.isIncompatible(pluginDescriptor)) {
            Alerts.okError(LocalizeValue.of("Plugin " + pluginDescriptor.getName() + " is incompatible with current installation"))
                .showAsync();
            return;
        }

        InstalledPluginsState state = InstalledPluginsState.getInstance();
        if (state.getAllPlugins().contains(pluginDescriptor)) {
            Alerts.okInfo(LocalizeValue.of("Plugin " + pluginDescriptor.getName() + " was already installed")).showAsync();
            return;
        }

        PluginDescriptor alreadyInstalledPlugin = PluginManager.findPlugin(pluginDescriptor.getPluginId());

        try {
            if (alreadyInstalledPlugin != null) {
                File oldFile = alreadyInstalledPlugin.getPath();
                if (oldFile != null) {
                    StartupActionScriptManager.addActionCommand(new StartupActionScriptManager.DeleteCommand(oldFile));
                }
                state.updateExistingPlugin(pluginDescriptor, alreadyInstalledPlugin);
            }
            else {
                state.getAllPlugins().add(pluginDescriptor);
            }

            PluginDownloader.install(file, file.getName(), false);
        }
        catch (IOException ex) {
            Alerts.okError(LocalizeValue.of(ex.getMessage())).showAsync();
            return;
        }

        state.getInstalledPlugins().add(pluginDescriptor.getPluginId());

        myPanel.selectInstalled(pluginDescriptor.getPluginId());
    }
}
