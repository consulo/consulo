/*
 * Copyright 2000-2013 JetBrains s.r.o.
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

import consulo.application.Application;
import consulo.application.internal.ApplicationEx;
import consulo.application.internal.plugin.PluginsLoader;
import consulo.application.internal.start.StartupActionScriptManager;
import consulo.application.util.TempFileService;
import consulo.container.plugin.PluginDescriptor;
import consulo.container.plugin.PluginId;
import consulo.container.plugin.PluginManager;
import consulo.externalService.impl.internal.PlatformOrPluginsNotificationGroupContributor;
import consulo.externalService.localize.ExternalServiceLocalize;
import consulo.localize.LocalizeValue;
import consulo.logging.Logger;
import consulo.project.Project;
import consulo.project.ui.notification.NotificationService;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.awt.Messages;
import consulo.ui.ex.awt.UIUtil;
import consulo.util.io.FileUtil;
import consulo.util.io.zip.ZipUtil;
import consulo.util.lang.xml.XmlStringUtil;
import org.jspecify.annotations.Nullable;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * @author stathik
 * @since 2003-11-29
 */
public class PluginInstallUtil {
    private static final Logger LOG = Logger.getInstance(PluginInstallUtil.class);

    private PluginInstallUtil() {
    }

    @Messages.YesNoResult
    @RequiredUIAccess
    public static int showShutDownDialog() {
        return showShutDownDialog(ExternalServiceLocalize.titlePluginsChanged());
    }

    @Messages.YesNoResult
    @RequiredUIAccess
    private static int showShutDownDialog(LocalizeValue title) {
        return Messages.showYesNoDialog(
            ExternalServiceLocalize.messageIdeaShutdownRequired(Application.get().getName()).get(),
            title.get(),
            ExternalServiceLocalize.actionShutdownText().get(),
            ExternalServiceLocalize.actionPostponeText().get(),
            UIUtil.getQuestionIcon()
        );
    }

    @Messages.YesNoResult
    @RequiredUIAccess
    public static int showRestartDialog() {
        return showRestartDialog(ExternalServiceLocalize.titlePluginsChanged());
    }

    @Messages.YesNoResult
    @RequiredUIAccess
    private static int showRestartDialog(LocalizeValue title) {
        return Messages.showYesNoDialog(
            ExternalServiceLocalize.messageIdeaRestartRequired(Application.get().getName()).get(),
            title.get(),
            ExternalServiceLocalize.actionRestartText().get(),
            ExternalServiceLocalize.actionPostponeText().get(),
            UIUtil.getQuestionIcon()
        );
    }

    @RequiredUIAccess
    public static void shutdownOrRestartApp(LocalizeValue title) {
        ApplicationEx app = (ApplicationEx) Application.get();
        int response = app.isRestartCapable() ? showRestartDialog(title) : showShutDownDialog(title);
        if (response == Messages.YES) {
            app.restart(true);
        }
    }

    
    public static Set<PluginDescriptor> getPluginsForInstall(List<PluginDescriptor> pluginsToInstall, List<PluginDescriptor> allPlugins) {
        List<PluginId> pluginIds = new ArrayList<>();
        for (PluginDescriptor pluginNode : pluginsToInstall) {
            pluginIds.add(pluginNode.getPluginId());
        }

        Set<PluginDescriptor> toInstallAll = new LinkedHashSet<>();

        for (PluginDescriptor toInstall : pluginsToInstall) {
            Set<PluginNode> depends = new LinkedHashSet<>();
            collectDepends(toInstall, pluginIds, depends, allPlugins);

            toInstallAll.addAll(depends);
            toInstallAll.add(toInstall);
        }

        if (toInstallAll.isEmpty()) {
            throw new IllegalArgumentException("No plugins for install");
        }
        return toInstallAll;
    }

    private static void collectDepends(
        PluginDescriptor toInstall,
        List<PluginId> toInstallOthers,
        Set<PluginNode> depends,
        List<PluginDescriptor> repoPlugins
    ) {
        PluginId[] dependentPluginIds = toInstall.getDependentPluginIds();
        InstalledPluginsState pluginsState = InstalledPluginsState.getInstance();

        for (PluginId dependentPluginId : dependentPluginIds) {
            if (PluginManager.findPlugin(dependentPluginId) != null || toInstallOthers.contains(dependentPluginId)) {
                // ignore installed or installing plugins
                continue;
            }

            if (pluginsState.getInstalledPlugins().contains(dependentPluginId)) {
                // downloaded plugin
                continue;
            }

            PluginNode dependInRepo = (PluginNode) findDescriptionInRepo(dependentPluginId, repoPlugins);
            if (dependInRepo != null) {
                depends.add(dependInRepo);

                collectDepends(dependInRepo, toInstallOthers, depends, repoPlugins);
            }
        }
    }

    private static @Nullable PluginDescriptor findDescriptionInRepo(PluginId depPluginId, List<PluginDescriptor> allPlugins) {
        for (PluginDescriptor plugin : allPlugins) {
            if (plugin.getPluginId().equals(depPluginId)) {
                return plugin;
            }
        }
        return null;
    }

    public static void prepareToUninstall(PluginId pluginId) throws IOException {
        if (PluginManager.findPlugin(pluginId) != null) {
            // add command to delete the 'action script' file
            PluginDescriptor pluginDescriptor = PluginManager.findPlugin(pluginId);
            if (pluginDescriptor != null) {
                StartupActionScriptManager.ActionCommand deleteOld =
                    new StartupActionScriptManager.DeleteCommand(pluginDescriptor.getPath());
                StartupActionScriptManager.addActionCommand(deleteOld);
            }
            else {
                LOG.error("Plugin not found: " + pluginId);
            }
        }
    }

    public static void markInstalled(Collection<PluginDescriptor> installed) {
        InstalledPluginsState state = InstalledPluginsState.getInstance();
        for (PluginDescriptor descriptor : installed) {
            state.getInstalledPlugins().add(descriptor.getPluginId());
            state.getOutdatedPlugins().remove(descriptor.getPluginId());

            PluginDescriptor alreadyInstalled = PluginManager.findPlugin(descriptor.getPluginId());
            if (alreadyInstalled != null) {
                state.updateExistingPlugin(descriptor, alreadyInstalled);
            }
            else {
                state.getAllPlugins().add(descriptor);
            }
        }
    }

    public static void notifyPluginsWereInstalled(Collection<? extends PluginDescriptor> installed, @Nullable Project project) {
        String pluginName = installed.size() == 1 ? installed.iterator().next().getName() : null;
        notifyPluginsWereUpdated(
            pluginName != null ? "Plugin '" + pluginName + "' was successfully installed" : "Plugins were installed",
            project
        );
    }

    public static void notifyPluginsWereUpdated(String title, @Nullable Project project) {
        ApplicationEx app = (ApplicationEx) Application.get();
        LocalizeValue appName = app.getName();
        boolean restartCapable = app.isRestartCapable();
        String message = restartCapable
            ? ExternalServiceLocalize.messageIdeaRestartRequired(appName).get()
            : ExternalServiceLocalize.messageIdeaShutdownRequired(appName).get();
        message += "<br><a href=" + (restartCapable ? "\"restart\">Restart now" : "\"shutdown\">Shutdown") + "</a>";
        NotificationService.getInstance()
            .newInfo(PlatformOrPluginsNotificationGroupContributor.ourPluginsLifecycleGroup)
            .title(LocalizeValue.localizeTODO(title))
            .content(LocalizeValue.localizeTODO(XmlStringUtil.wrapInHtml(message)))
            .hyperlinkListener((notification, event) -> {
                notification.expire();
                if (restartCapable) {
                    app.restart(true);
                }
                else {
                    app.exit(true, true);
                }
            })
            .notify(project);
    }

    public static @Nullable PluginDescriptor loadDescriptorFromArchive(File file) throws IOException {
        PluginDescriptor descriptor = null;

        TempFileService tempFileService = Application.get().getInstance(TempFileService.class);
        Path outputDirPath = tempFileService.createTempDirectory("plugin", "");
        File outputDir = outputDirPath.toFile();
        try {
            ZipUtil.extract(file, outputDir, null);
            File[] files = outputDir.listFiles();
            if (files != null && files.length == 1) {
                descriptor = PluginsLoader.loadPluginDescriptor(files[0]);
            }
        }
        finally {
            FileUtil.delete(outputDir);
        }
        return descriptor;
    }
}
