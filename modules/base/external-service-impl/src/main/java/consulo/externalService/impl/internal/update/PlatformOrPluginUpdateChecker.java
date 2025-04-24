/*
 * Copyright 2013-2016 consulo.io
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

import consulo.application.Application;
import consulo.application.ApplicationPropertiesComponent;
import consulo.application.internal.ApplicationInfo;
import consulo.application.progress.ProgressIndicator;
import consulo.application.util.DateFormatUtil;
import consulo.component.ProcessCanceledException;
import consulo.container.boot.ContainerPathManager;
import consulo.container.plugin.PluginDescriptor;
import consulo.container.plugin.PluginId;
import consulo.container.plugin.PluginIds;
import consulo.container.plugin.PluginManager;
import consulo.externalService.impl.internal.PluginIconHolder;
import consulo.externalService.impl.internal.plugin.InstalledPluginsState;
import consulo.externalService.impl.internal.plugin.PluginNode;
import consulo.externalService.impl.internal.plugin.ui.PluginTab;
import consulo.externalService.impl.internal.pluginAdvertiser.PluginAdvertiserRequester;
import consulo.externalService.impl.internal.repository.RepositoryHelper;
import consulo.externalService.internal.PlatformOrPluginUpdateResultType;
import consulo.externalService.localize.ExternalServiceLocalize;
import consulo.externalService.update.UpdateChannel;
import consulo.localize.LocalizeValue;
import consulo.logging.Logger;
import consulo.project.Project;
import consulo.project.ui.notification.*;
import consulo.ui.Alert;
import consulo.ui.Alerts;
import consulo.ui.UIAccess;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.action.AnActionEvent;
import consulo.util.collection.ContainerUtil;
import consulo.util.concurrent.AsyncResult;
import consulo.util.lang.StringUtil;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.io.File;
import java.util.*;

/**
 * @author VISTALL
 * @since 10-Oct-16
 */
public class PlatformOrPluginUpdateChecker {
    private static final Logger LOG = Logger.getInstance(PlatformOrPluginUpdateChecker.class);

    public static final NotificationGroup ourGroup =
        new NotificationGroup("Platform Or Plugins Update", NotificationDisplayType.STICKY_BALLOON, false);

    private static final String ourForceJREBuild = "force.jre.build.on.update";
    private static final String ourForceJREBuildVersion = "force.jre.build.on.update.version";

    @Nonnull
    public static PluginId getPlatformPluginId() {
        return PlatformPluginId.find().getPluginId();
    }

    public static boolean isJreBuild() {
        return new File(ContainerPathManager.get().getHomePath(), "jre").exists() || isForceBundledJreAtUpdate();
    }

    public static boolean isForceBundledJreAtUpdate() {
        validateForceBundledJreVersion();
        return ApplicationPropertiesComponent.getInstance().getBoolean(ourForceJREBuild);
    }

    public static void setForceBundledJreAtUpdate() {
        ApplicationPropertiesComponent.getInstance().setValue(ourForceJREBuildVersion, ApplicationInfo.getInstance().getBuild().toString());
        ApplicationPropertiesComponent.getInstance().setValue(ourForceJREBuild, true);
    }

    /**
     * Validate force bundle jre flag. If flag set version changed - it will be dropped
     */
    private static void validateForceBundledJreVersion() {
        String oldVer = ApplicationPropertiesComponent.getInstance().getValue(ourForceJREBuildVersion);

        String curVer = ApplicationInfo.getInstance().getBuild().toString();

        if (!Objects.equals(oldVer, curVer)) {
            ApplicationPropertiesComponent.getInstance().unsetValue(ourForceJREBuild);
        }
    }

    public static boolean isPlatform(@Nonnull PluginId pluginId) {
        return pluginId.toString().startsWith("consulo.dist.");
    }

    public static boolean checkNeeded() {
        UpdateSettingsImpl updateSettings = UpdateSettingsImpl.getInstance();
        if (!updateSettings.isEnable()) {
            return false;
        }

        final long timeDelta = System.currentTimeMillis() - updateSettings.getLastTimeCheck();
        return Math.abs(timeDelta) >= DateFormatUtil.DAY;
    }

    @Nonnull
    public static AsyncResult<PlatformOrPluginUpdateResultType> updateAndShowResult() {
        final AsyncResult<PlatformOrPluginUpdateResultType> result = AsyncResult.undefined();
        final Application app = Application.get();

        UIAccess lastUIAccess = app.getLastUIAccess();

        final UpdateSettingsImpl updateSettings = UpdateSettingsImpl.getInstance();
        if (updateSettings.isEnable()) {
            app.executeOnPooledThread(() -> checkAndNotifyForUpdates(null, false, null, lastUIAccess, result));
        }
        else {
            registerSettingsGroupUpdate(result);

            result.setDone(PlatformOrPluginUpdateResultType.NO_UPDATE);
        }
        return result;
    }

    public static void showErrorMessage(boolean showErrorDialog, Throwable e, UIAccess uiAccess, @Nullable Project project) {
        LOG.warn(e);

        if (showErrorDialog) {
            uiAccess.give(() -> {
                LocalizeValue className = LocalizeValue.of(e.getClass().getSimpleName());
                LocalizeValue message = LocalizeValue.of(e.getLocalizedMessage());
                Alert<Object> alert = Alerts.okError(LocalizeValue.join(className, LocalizeValue.colon(), LocalizeValue.space(), message));
                if (project != null) {
                    alert.showAsync(project);
                }
                else {
                    alert.showAsync();
                }
            });
        }
    }

    @RequiredUIAccess
    private static void showUpdateResult(@Nullable Project project,
                                         final PlatformOrPluginUpdateResult targetsForUpdate,
                                         final boolean showResults) {
        PlatformOrPluginUpdateResultType type = targetsForUpdate.getType();
        switch (type) {
            case NO_UPDATE:
                if (showResults) {
                    ourGroup.createNotification(ExternalServiceLocalize.updateAvailableGroup().get(),
                        ExternalServiceLocalize.updateThereAreNoUpdates().get(),
                        NotificationType.INFORMATION,
                        null).notify(project);
                }
                break;
            case RESTART_REQUIRED:
                PluginTab.notifyPluginsWereInstalled(Collections.emptyList(), null);
                break;
            case PLUGIN_UPDATE:
            case PLATFORM_UPDATE:
                if (showResults) {
                    new PlatformOrPluginDialog(project, targetsForUpdate, null, null, false).showAsync();
                }
                else {
                    Notification notification = ourGroup.createNotification(ExternalServiceLocalize.updateAvailableGroup().get(),
                        ExternalServiceLocalize.updateAvailable().get(),
                        NotificationType.INFORMATION,
                        null);
                    notification.addAction(new NotificationAction(ExternalServiceLocalize.updateViewUpdates()) {
                        @RequiredUIAccess
                        @Override
                        public void actionPerformed(@Nonnull AnActionEvent e, @Nonnull Notification notification) {
                            new PlatformOrPluginDialog(project, targetsForUpdate, null, null, false).showAsync();
                        }
                    });
                    notification.notify(project);
                }
                break;
        }
    }

    private static void registerSettingsGroupUpdate(@Nonnull AsyncResult<PlatformOrPluginUpdateResultType> result) {
        result.doWhenDone(type -> {
            UpdateSettingsImpl updateSettings = UpdateSettingsImpl.getInstance();
            updateSettings.setLastCheckResult(type);
        });
    }

    public static void checkAndNotifyForUpdates(@Nullable Project project,
                                                boolean showResults,
                                                @Nullable ProgressIndicator indicator,
                                                @Nonnull UIAccess uiAccess,
                                                @Nonnull AsyncResult<PlatformOrPluginUpdateResultType> result) {
        UIAccess.assetIsNotUIThread();

        registerSettingsGroupUpdate(result);

        PlatformOrPluginUpdateResult updateResult = checkForUpdates(showResults, indicator, uiAccess, project);
        if (updateResult == PlatformOrPluginUpdateResult.CANCELED) {
            result.setDone(PlatformOrPluginUpdateResultType.CANCELED);
            return;
        }

        uiAccess.give(() -> {
            result.setDone(updateResult.getType());

            showUpdateResult(project, updateResult, showResults);
        });
    }

    @Nonnull
    private static PlatformOrPluginUpdateResult checkForUpdates(final boolean showResults,
                                                                @Nullable ProgressIndicator indicator,
                                                                @Nonnull UIAccess uiAccess,
                                                                @Nullable Project project) {
        PluginId platformPluginId = getPlatformPluginId();

        ApplicationInfo appInfo = ApplicationInfo.getInstance();
        String currentBuildNumber = appInfo.getBuild().asString();

        List<PluginDescriptor> remotePlugins;
        UpdateChannel channel = UpdateSettingsImpl.getInstance().getChannel();
        try {
            remotePlugins = RepositoryHelper.loadPluginsFromRepository(indicator, channel, null, true);
            Application.get().getInstance(PluginAdvertiserRequester.class).update(remotePlugins);
        }
        catch (ProcessCanceledException e) {
            return PlatformOrPluginUpdateResult.CANCELED;
        }
        catch (Exception e) {
            showErrorMessage(showResults, e, uiAccess, project);
            return PlatformOrPluginUpdateResult.CANCELED;
        }

        boolean alreadyVisited = false;
        final InstalledPluginsState state = InstalledPluginsState.getInstance();

        PluginDescriptor newPlatformPlugin = null;
        // try to search platform number
        for (PluginDescriptor pluginDescriptor : remotePlugins) {
            PluginId pluginId = pluginDescriptor.getPluginId();
            // platform already downloaded for update
            if (state.wasUpdated(pluginId)) {
                alreadyVisited = true;
                break;
            }
            if (platformPluginId.equals(pluginId)) {
                if (StringUtil.compareVersionNumbers(pluginDescriptor.getVersion(), currentBuildNumber) > 0) {
                    // change current build
                    currentBuildNumber = pluginDescriptor.getVersion();
                    newPlatformPlugin = pluginDescriptor;
                    break;
                }
            }
        }

        final Map<PluginId, PlatformOrPluginNode> targets = new LinkedHashMap<>();
        if (newPlatformPlugin != null) {
            PluginNode thisPlatform = new PluginNode(platformPluginId);
            thisPlatform.setVersion(appInfo.getBuild().asString());
            thisPlatform.setName(newPlatformPlugin.getName());

            PluginIconHolder.put(newPlatformPlugin, Application.get().getBigIcon());

            targets.put(platformPluginId, new PlatformOrPluginNode(platformPluginId, thisPlatform, newPlatformPlugin));

            // load new plugins with new app build
            try {
                remotePlugins = RepositoryHelper.loadPluginsFromRepository(indicator, channel, currentBuildNumber, true);
            }
            catch (ProcessCanceledException e) {
                return PlatformOrPluginUpdateResult.CANCELED;
            }
            catch (Exception e) {
                LOG.warn(e);
            }
        }

        final Map<PluginId, PluginDescriptor> ourPlugins = new HashMap<>();
        final List<PluginDescriptor> installedPlugins = PluginManager.getPlugins();
        for (PluginDescriptor installedPlugin : installedPlugins) {
            if (PluginIds.isPlatformPlugin(installedPlugin.getPluginId())) {
                continue;
            }
            ourPlugins.put(installedPlugin.getPluginId(), installedPlugin);
        }

        state.getOutdatedPlugins().clear();
        if (!ourPlugins.isEmpty()) {
            try {
                for (final Map.Entry<PluginId, PluginDescriptor> entry : ourPlugins.entrySet()) {
                    final PluginId pluginId = entry.getKey();

                    PluginDescriptor filtered = ContainerUtil.find(remotePlugins, it -> pluginId.equals(it.getPluginId()));

                    if (filtered == null) {
                        // if platform updated - but we not found new plugin in new remote list, notify user about it
                        if (newPlatformPlugin != null) {
                            targets.put(pluginId, new PlatformOrPluginNode(pluginId, entry.getValue(), null));
                        }
                        continue;
                    }

                    if (state.wasUpdated(filtered.getPluginId())) {
                        alreadyVisited = true;
                        continue;
                    }

                    if (StringUtil.compareVersionNumbers(filtered.getVersion(), entry.getValue().getVersion()) > 0) {
                        state.getOutdatedPlugins().add(pluginId);

                        processDependencies(filtered, targets, remotePlugins);

                        targets.put(pluginId, new PlatformOrPluginNode(pluginId, entry.getValue(), filtered));
                    }
                }
            }
            catch (ProcessCanceledException ignore) {
                return PlatformOrPluginUpdateResult.CANCELED;
            }
            catch (Exception e) {
                showErrorMessage(showResults, e, uiAccess, project);
                return PlatformOrPluginUpdateResult.CANCELED;
            }
        }

        if (newPlatformPlugin != null) {
            return new PlatformOrPluginUpdateResult(PlatformOrPluginUpdateResultType.PLATFORM_UPDATE, new ArrayList<>(targets.values()));
        }

        if (alreadyVisited && targets.isEmpty()) {
            return PlatformOrPluginUpdateResult.RESTART_REQUIRED;
        }
        if (targets.isEmpty()) {
            return PlatformOrPluginUpdateResult.NO_UPDATE;
        }
        else {
            return new PlatformOrPluginUpdateResult(PlatformOrPluginUpdateResultType.PLUGIN_UPDATE, new ArrayList<>(targets.values()));
        }
    }

    private static void processDependencies(@Nonnull PluginDescriptor target,
                                            Map<PluginId, PlatformOrPluginNode> targets,
                                            List<PluginDescriptor> remotePlugins) {
        PluginId[] dependentPluginIds = target.getDependentPluginIds();
        for (PluginId pluginId : dependentPluginIds) {
            if (targets.containsKey(pluginId)) {
                // plugin already marked as new for download
                continue;
            }

            PluginDescriptor depPlugin = PluginManager.findPlugin(pluginId);
            // if plugin is not installed
            if (depPlugin == null) {
                PluginDescriptor filtered = ContainerUtil.find(remotePlugins, it -> pluginId.equals(it.getPluginId()));

                if (filtered != null) {
                    targets.put(filtered.getPluginId(), new PlatformOrPluginNode(filtered.getPluginId(), null, filtered));

                    processDependencies(filtered, targets, remotePlugins);
                }
            }
        }
    }
}
