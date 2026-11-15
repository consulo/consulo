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

import consulo.application.Application;
import consulo.application.internal.plugin.DynamicPluginManager;
import consulo.application.internal.start.StartupActionScriptManager;
import consulo.application.util.concurrent.AppExecutorUtil;
import consulo.container.internal.PluginValidator;
import consulo.container.plugin.PluginDescriptor;
import consulo.container.plugin.PluginId;
import consulo.container.plugin.PluginManager;
import consulo.externalService.impl.internal.plugin.InstalledPluginsState;
import consulo.externalService.impl.internal.plugin.PluginInstallUtil;
import consulo.externalService.impl.internal.update.PluginDownloader;
import consulo.fileChooser.FileChooser;
import consulo.fileChooser.FileChooserDescriptor;
import consulo.localize.LocalizeValue;
import consulo.logging.Logger;
import consulo.platform.base.icon.PlatformIconGroup;
import consulo.ui.MessageBoxes;
import consulo.ui.UIAccess;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.action.AnActionEvent;
import consulo.ui.ex.action.DumbAwareAction;
import consulo.virtualFileSystem.VirtualFile;
import consulo.virtualFileSystem.util.VirtualFileUtil;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;

/**
 * @author VISTALL
 * @since 2026-08-13
 */
public class UnifiedInstallPluginFromDiskAction extends DumbAwareAction {
    private static final Logger LOG = Logger.getInstance(UnifiedInstallPluginFromDiskAction.class);

    private final UnifiedPluginsPage myPage;

    public UnifiedInstallPluginFromDiskAction(UnifiedPluginsPage page) {
        super("Install plugin from disk…", null, PlatformIconGroup.nodesPlugin());
        myPage = page;
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
            pluginDescriptor = PluginInstallUtil.loadDescriptorFromArchive(file);
        }
        catch (IOException ex) {
            LOG.warn(ex);
            MessageBoxes.okError(ex).showAsync();
            return;
        }

        if (pluginDescriptor == null) {
            MessageBoxes.okError(LocalizeValue.localizeTODO("Fail to load plugin descriptor from file " + file.getName())).showAsync();
            return;
        }

        if (PluginValidator.isIncompatible(pluginDescriptor)) {
            MessageBoxes.okError(LocalizeValue.localizeTODO("Plugin " + pluginDescriptor.getName() + " is incompatible with current installation"))
                .showAsync();
            return;
        }

        InstalledPluginsState state = InstalledPluginsState.getInstance();
        if (state.getAllPlugins().contains(pluginDescriptor)) {
            MessageBoxes.okInfo(LocalizeValue.localizeTODO("Plugin " + pluginDescriptor.getName() + " was already installed")).showAsync();
            return;
        }

        PluginDescriptor alreadyInstalledPlugin = PluginManager.findPlugin(pluginDescriptor.getPluginId());

        try {
            DynamicPluginManager dynamicPluginManager = Application.get().getInstance(DynamicPluginManager.class);
            if (alreadyInstalledPlugin == null && dynamicPluginManager.isEnabled()) {
                Path path = PluginDownloader.installNow(file, file.getName());
                PluginId pluginId = pluginDescriptor.getPluginId();
                CompletableFuture.supplyAsync(
                        () -> dynamicPluginManager.createSession().install(path).apply(),
                        AppExecutorUtil.getAppExecutorService()
                    )
                    .whenCompleteAsync((result, error) -> {
                        if (error != null) {
                            LOG.error(error);
                        }

                        if (result == null || result.getRestartRequired().contains(pluginId)) {
                            state.getAllPlugins().add(pluginDescriptor);
                            state.getInstalledPlugins().add(pluginId);
                        }
                        myPage.select(pluginId);
                    }, UIAccess.current());
                return;
            }
            else {
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
        }
        catch (IOException ex) {
            LOG.warn(ex);
            MessageBoxes.okError(ex).showAsync();
            return;
        }

        state.getInstalledPlugins().add(pluginDescriptor.getPluginId());

        myPage.select(pluginDescriptor.getPluginId());
    }
}
