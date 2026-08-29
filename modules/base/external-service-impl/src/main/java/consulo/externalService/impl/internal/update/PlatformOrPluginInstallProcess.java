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
package consulo.externalService.impl.internal.update;

import consulo.application.AccessToken;
import consulo.application.Application;
import consulo.application.internal.ApplicationInfo;
import consulo.application.plugin.PluginActionListener;
import consulo.application.progress.ProgressIndicator;
import consulo.application.progress.Task;
import consulo.container.plugin.PluginDescriptor;
import consulo.container.plugin.PluginId;
import consulo.container.plugin.PluginIds;
import consulo.container.plugin.PluginManager;
import consulo.externalService.impl.internal.plugin.InstalledPluginsState;
import consulo.externalService.impl.internal.plugin.PluginNode;
import consulo.externalService.impl.internal.pluginHistory.UpdateHistory;
import consulo.externalService.internal.PlatformOrPluginUpdateResultType;
import consulo.externalService.internal.UpdateSettingsEx;
import consulo.externalService.localize.ExternalServiceLocalize;
import consulo.externalService.update.UpdateSettings;
import consulo.logging.Logger;
import consulo.project.Project;
import consulo.ui.Alert;
import consulo.ui.Alerts;
import consulo.ui.UIAccess;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.util.lang.StringUtil;
import org.jspecify.annotations.Nullable;

import java.io.IOException;
import java.util.*;
import java.util.function.Consumer;

/**
 * The download and install work behind the update and install dialogs, shared by the frontends - a dialog only
 * decides what to show, this one does what was confirmed.
 */
public final class PlatformOrPluginInstallProcess {
    private static final Logger LOG = Logger.getInstance(PlatformOrPluginInstallProcess.class);

    private PlatformOrPluginInstallProcess() {
    }

    @RequiredUIAccess
    public static void run(
        @Nullable Project project,
        List<PlatformOrPluginNode> nodes,
        PlatformOrPluginUpdateResultType type,
        @Nullable String platformVersion,
        @Nullable Consumer<Collection<PluginDescriptor>> afterCallback,
        boolean modalProgress
    ) {
        if (UpdateBusyLocker.isBusy()) {
            return;
        }

        Application application = Application.get();
        UIAccess uiAccess = UIAccess.current();

        Consumer<ProgressIndicator> processor = indicator -> {
            List<PluginDescriptor> installed = new ArrayList<>(nodes.size());

            int installCount = (int) nodes
                .stream()
                .filter(it -> it.getFutureDescriptor() != null)
                .count();

            List<PluginDownloader> forInstall = new ArrayList<>(nodes.size());
            int i = 0;
            for (PlatformOrPluginNode platformOrPluginNode : nodes) {
                PluginDescriptor pluginDescriptor = platformOrPluginNode.getFutureDescriptor();
                // update list contains broken plugins
                if (pluginDescriptor == null) {
                    continue;
                }

                try {
                    PluginDownloader downloader = PluginDownloader.createDownloader(
                        pluginDescriptor,
                        platformVersion,
                        type != PlatformOrPluginUpdateResultType.PLUGIN_INSTALL
                    );

                    forInstall.add(downloader);

                    downloader.download(new CompositePluginInstallIndicator(indicator, i++, installCount));
                }
                catch (PluginDownloadFailedException e) {
                    LOG.warn(e);
                    uiAccess.give(() -> Alerts.okError(e).showAsync());
                    return;
                }
            }

            indicator.setText(ExternalServiceLocalize.progressInstallingPlugins());

            UpdateHistory updateHistory = application.getInstance(UpdateHistory.class);

            InstalledPluginsState installedPluginsState = InstalledPluginsState.getInstance();
            for (PluginDownloader downloader : forInstall) {
                try {
                    // already was installed
                    if (installedPluginsState.wasUpdated(downloader.getPluginId())) {
                        continue;
                    }

                    installedPluginsState.getUpdatedPlugins().add(downloader.getPluginId());

                    downloader.install(indicator, true);

                    PluginDescriptor pluginDescriptor = downloader.getPluginDescriptor();

                    if (pluginDescriptor instanceof PluginNode pluginNode) {
                        pluginNode.setInstallStatus(PluginNode.STATUS_DOWNLOADED);

                        if (type == PlatformOrPluginUpdateResultType.PLUGIN_INSTALL && pluginDescriptor.isExperimental()) {
                            updateHistory.setShowExperimentalWarning(true);
                        }
                    }

                    installed.add(pluginDescriptor);
                }
                catch (IOException e) {
                    LOG.warn(e);
                    uiAccess.give(() -> Alerts.okError(e).showAsync());
                    return;
                }
            }

            application.getMessageBus().syncPublisher(PluginActionListener.class).pluginsInstalled(
                installed.stream()
                    .filter(it -> it instanceof PluginNode)
                    .map(PluginDescriptor::getPluginId)
                    .toArray(PluginId[]::new)
            );

            Map<String, String> pluginHistory = new HashMap<>();
            for (PluginDescriptor descriptor : PluginManager.getPlugins()) {
                if (PluginIds.isPlatformPlugin(descriptor.getPluginId())) {
                    continue;
                }

                pluginHistory.put(descriptor.getPluginId().getIdString(), StringUtil.notNullize(descriptor.getVersion()));
            }

            pluginHistory.put(
                PlatformOrPluginUpdateChecker.getPlatformPluginId().getIdString(),
                ApplicationInfo.getInstance().getBuild().toString()
            );

            updateHistory.replaceHistory(pluginHistory);

            if (afterCallback != null) {
                afterCallback.accept(installed);
            }

            if (type != PlatformOrPluginUpdateResultType.PLUGIN_INSTALL) {
                uiAccess.give(() -> {
                    UpdateSettingsEx updateSettings = (UpdateSettingsEx) UpdateSettings.getInstance();
                    updateSettings.setLastCheckResult(PlatformOrPluginUpdateResultType.RESTART_REQUIRED);

                    askRestart();
                });
            }
        };

        if (modalProgress) {
            Task.Modal.queue(
                project,
                ExternalServiceLocalize.progressDownloadPlugins(),
                true,
                progressIndicator -> {
                    try (AccessToken ignored = UpdateBusyLocker.block()) {
                        processor.accept(progressIndicator);
                    }
                }
            );
        }
        else {
            Task.Backgroundable.queue(
                project,
                ExternalServiceLocalize.progressDownloadPlugins(),
                true,
                progressIndicator -> {
                    try (AccessToken ignored = UpdateBusyLocker.block()) {
                        processor.accept(progressIndicator);
                    }
                }
            );
        }
    }

    /**
     * The unified counterpart of {@code PluginInstallUtil#showRestartDialog} - asks and, when agreed, restarts
     * or exits depending on what the platform is capable of.
     */
    @RequiredUIAccess
    public static void askRestart() {
        Application application = Application.get();

        boolean restartCapable = application.isRestartCapable();

        Alert<Boolean> alert = Alert.create();
        alert.asQuestion();
        alert.title(ExternalServiceLocalize.titlePluginsChanged());
        alert.text(
            restartCapable
                ? ExternalServiceLocalize.messageIdeaRestartRequired(application.getName())
                : ExternalServiceLocalize.messageIdeaShutdownRequired(application.getName())
        );

        alert.button(
            restartCapable ? ExternalServiceLocalize.actionRestartText() : ExternalServiceLocalize.actionShutdownText(),
            () -> Boolean.TRUE
        );
        alert.asDefaultButton();

        alert.button(ExternalServiceLocalize.actionPostponeText(), () -> Boolean.FALSE);
        alert.asExitButton();

        alert.showAsync().whenComplete((agreed, error) -> {
            if (Boolean.TRUE.equals(agreed)) {
                application.restart(true);
            }
        });
    }
}
