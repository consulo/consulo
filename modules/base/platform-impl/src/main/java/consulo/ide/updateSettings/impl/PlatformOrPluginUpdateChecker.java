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
package consulo.ide.updateSettings.impl;

import com.intellij.ide.IdeBundle;
import com.intellij.ide.plugins.*;
import com.intellij.ide.util.PropertiesComponent;
import com.intellij.notification.*;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.application.Application;
import com.intellij.openapi.application.ApplicationInfo;
import com.intellij.openapi.application.PathManager;
import com.intellij.openapi.extensions.PluginId;
import com.intellij.openapi.progress.ProcessCanceledException;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.util.AsyncResult;
import com.intellij.openapi.util.SystemInfo;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.util.ArrayUtil;
import com.intellij.util.containers.ContainerUtil;
import com.intellij.util.text.DateFormatUtil;
import com.intellij.util.ui.UIUtil;
import consulo.container.plugin.PluginDescriptor;
import consulo.container.plugin.PluginIds;
import consulo.ide.plugins.InstalledPluginsState;
import consulo.ide.plugins.pluginsAdvertisement.PluginsAdvertiserHolder;
import consulo.ide.updateSettings.UpdateChannel;
import consulo.ide.updateSettings.UpdateSettings;
import consulo.logging.Logger;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.File;
import java.util.*;

/**
 * @author VISTALL
 * @since 10-Oct-16
 */
public class PlatformOrPluginUpdateChecker {
  private static final Logger LOG = Logger.getInstance(PlatformOrPluginUpdateChecker.class);

  private static final NotificationGroup ourGroup = new NotificationGroup("Platform Or Plugins Update", NotificationDisplayType.STICKY_BALLOON, false);

  private static final PluginId ourWinNoJre = PluginId.getId("consulo-win-no-jre");
  private static final PluginId ourWin = PluginId.getId("consulo-win");
  private static final PluginId ourWin64 = PluginId.getId("consulo-win64");
  // dummy zip ids
  private static final PluginId ourWinNoJreZip = PluginId.getId("consulo-win-no-jre-zip");
  private static final PluginId ourWinZip = PluginId.getId("consulo-win-zip");
  private static final PluginId ourWin64Zip = PluginId.getId("consulo-win64-zip");
  private static final PluginId ourLinuxNoJre = PluginId.getId("consulo-linux-no-jre");
  private static final PluginId ourLinux = PluginId.getId("consulo-linux");
  private static final PluginId ourLinux64 = PluginId.getId("consulo-linux64");
  private static final PluginId ourMacNoJre = PluginId.getId("consulo-mac-no-jre");
  private static final PluginId ourMac64 = PluginId.getId("consulo-mac64");

  private static final PluginId[] ourPlatformIds = {ourWinNoJre, ourWin, ourWin64, ourLinuxNoJre, ourLinux, ourLinux64, ourMacNoJre, ourMac64, ourWinNoJreZip, ourWinZip, ourWin64Zip};

  private static final String ourForceJREBuild = "force.jre.build.on.update";
  private static final String ourForceJREBuildVersion = "force.jre.build.on.update.version";

  @Nonnull
  public static PluginId getPlatformPluginId() {
    boolean isJreBuild = isJreBuild();
    boolean is64Bit = SystemInfo.is64Bit;
    if (SystemInfo.isWindows) {
      return isJreBuild ? (is64Bit ? ourWin64 : ourWin) : ourWinNoJre;
    }
    else if (SystemInfo.isMac) {
      return isJreBuild ? ourMac64 : ourMacNoJre;
    }
    else {
      return isJreBuild ? (is64Bit ? ourLinux64 : ourLinux) : ourLinuxNoJre;
    }
  }

  public static boolean isJreBuild() {
    return new File(PathManager.getHomePath(), "jre").exists() || isForceBundledJreAtUpdate();
  }

  public static boolean isForceBundledJreAtUpdate() {
    validateForceBundledJreVersion();
    return PropertiesComponent.getInstance().getBoolean(ourForceJREBuild);
  }

  public static void setForceBundledJreAtUpdate() {
    PropertiesComponent.getInstance().setValue(ourForceJREBuildVersion, ApplicationInfo.getInstance().getBuild().toString());
    PropertiesComponent.getInstance().setValue(ourForceJREBuild, true);
  }

  /**
   * Validate force bundle jre flag. If flag set version changed - it will be dropped
   */
  private static void validateForceBundledJreVersion() {
    String oldVer = PropertiesComponent.getInstance().getValue(ourForceJREBuildVersion);

    String curVer = ApplicationInfo.getInstance().getBuild().toString();

    if (!Objects.equals(oldVer, curVer)) {
      PropertiesComponent.getInstance().unsetValue(ourForceJREBuild);
    }
  }

  public static boolean isPlatform(@Nonnull PluginId pluginId) {
    return ArrayUtil.contains(pluginId, ourPlatformIds);
  }

  public static boolean checkNeeded() {
    UpdateSettings updateSettings = UpdateSettings.getInstance();
    if (!updateSettings.isEnable()) {
      return false;
    }

    final long timeDelta = System.currentTimeMillis() - updateSettings.getLastTimeCheck();
    return Math.abs(timeDelta) >= DateFormatUtil.DAY;
  }

  @Nonnull
  public static AsyncResult<Void> updateAndShowResult() {
    final AsyncResult<Void> result = AsyncResult.undefined();
    final Application app = Application.get();
    final UpdateSettings updateSettings = UpdateSettings.getInstance();
    if (!updateSettings.isEnable()) {
      result.setDone();
      return result;
    }
    app.executeOnPooledThread(() -> checkAndNotifyForUpdates(null, false, null).notify(result));
    return result;
  }

  public static void showErrorMessage(boolean showErrorDialog, final String failedMessage) {
    if (showErrorDialog) {
      UIUtil.invokeLaterIfNeeded(() -> Messages.showErrorDialog(failedMessage, IdeBundle.message("title.connection.error")));
    }
    else {
      LOG.info(failedMessage);
    }
  }

  private static void showUpdateResult(@Nullable Project project, final PlatformOrPluginUpdateResult targetsForUpdate, final boolean showResults) {
    PlatformOrPluginUpdateResult.Type type = targetsForUpdate.getType();
    switch (type) {
      case NO_UPDATE:
        if (showResults) {
          ourGroup.createNotification(IdeBundle.message("update.available.group"), IdeBundle.message("update.there.are.no.updates"), NotificationType.INFORMATION, null).notify(project);
        }
        break;
      case UPDATE_RESTART:
        PluginManagerMain.notifyPluginsWereInstalled(Collections.emptyList(), null);
        break;
      case PLUGIN_UPDATE:
      case PLATFORM_UPDATE:
        if (showResults) {
          new PlatformOrPluginDialog(project, targetsForUpdate, null, null).show();
        }
        else {
          Notification notification = ourGroup.createNotification(IdeBundle.message("update.available.group"), IdeBundle.message("update.available"), NotificationType.INFORMATION, null);
          notification.addAction(new NotificationAction(IdeBundle.message("update.view.updates")) {
            @Override
            public void actionPerformed(@Nonnull AnActionEvent e, @Nonnull Notification notification) {
              new PlatformOrPluginDialog(project, targetsForUpdate, null, null).show();
            }
          });
          notification.notify(project);
        }
        break;
    }
  }

  public static AsyncResult<Void> checkAndNotifyForUpdates(@Nullable Project project, boolean showResults, @Nullable ProgressIndicator indicator) {
    AsyncResult<Void> actionCallback = AsyncResult.undefined();
    PlatformOrPluginUpdateResult updateResult = checkForUpdates(showResults, indicator);
    if (updateResult == PlatformOrPluginUpdateResult.CANCELED) {
      actionCallback.setDone();
      return actionCallback;
    }

    UIUtil.invokeLaterIfNeeded(() -> {
      actionCallback.setDone();

      showUpdateResult(project, updateResult, showResults);
    });
    return actionCallback;
  }

  @Nonnull
  private static PlatformOrPluginUpdateResult checkForUpdates(final boolean showResults, @Nullable ProgressIndicator indicator) {
    PluginId platformPluginId = getPlatformPluginId();

    ApplicationInfo appInfo = ApplicationInfo.getInstance();
    String currentBuildNumber = appInfo.getBuild().asString();

    List<PluginDescriptor> remotePlugins = Collections.emptyList();
    UpdateChannel channel = UpdateSettings.getInstance().getChannel();
    try {
      remotePlugins = RepositoryHelper.loadPluginsFromRepository(indicator, channel);
      PluginsAdvertiserHolder.update(remotePlugins);
    }
    catch (ProcessCanceledException e) {
      return PlatformOrPluginUpdateResult.CANCELED;
    }
    catch (Exception e) {
      LOG.info(e);
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

    final List<PlatformOrPluginNode> targets = new ArrayList<>();
    if (newPlatformPlugin != null) {
      PluginNode thisPlatform = new PluginNode(platformPluginId);
      thisPlatform.setVersion(appInfo.getBuild().asString());
      thisPlatform.setName(newPlatformPlugin.getName());

      targets.add(new PlatformOrPluginNode(platformPluginId, thisPlatform, newPlatformPlugin));

      // load new plugins with new app build
      try {
        remotePlugins = RepositoryHelper.loadPluginsFromRepository(indicator, channel, currentBuildNumber);
      }
      catch (ProcessCanceledException e) {
        return PlatformOrPluginUpdateResult.CANCELED;
      }
      catch (Exception e) {
        LOG.info(e);
      }
    }

    final Map<PluginId, PluginDescriptor> ourPlugins = new HashMap<>();
    final List<PluginDescriptor> installedPlugins = consulo.container.plugin.PluginManager.getPlugins();
    final List<String> disabledPlugins = PluginManagerCore.getDisabledPlugins();
    for (PluginDescriptor installedPlugin : installedPlugins) {
      if (PluginIds.isPlatformPlugin(installedPlugin.getPluginId())) {
        continue;
      }

      if (!disabledPlugins.contains(installedPlugin.getPluginId().getIdString())) {
        ourPlugins.put(installedPlugin.getPluginId(), installedPlugin);
      }
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
              targets.add(new PlatformOrPluginNode(pluginId, entry.getValue(), null));
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

            targets.add(new PlatformOrPluginNode(pluginId, entry.getValue(), filtered));
          }
        }
      }
      catch (ProcessCanceledException ignore) {
        return PlatformOrPluginUpdateResult.CANCELED;
      }
      catch (Exception e) {
        showErrorMessage(showResults, e.getMessage());
      }
    }

    if (newPlatformPlugin != null) {
      return new PlatformOrPluginUpdateResult(PlatformOrPluginUpdateResult.Type.PLATFORM_UPDATE, targets);
    }

    if (alreadyVisited && targets.isEmpty()) {
      return PlatformOrPluginUpdateResult.UPDATE_RESTART;
    }
    return targets.isEmpty() ? PlatformOrPluginUpdateResult.NO_UPDATE : new PlatformOrPluginUpdateResult(PlatformOrPluginUpdateResult.Type.PLUGIN_UPDATE, targets);
  }

  private static void processDependencies(@Nonnull PluginDescriptor target, List<PlatformOrPluginNode> targets, List<PluginDescriptor> remotePlugins) {
    PluginId[] dependentPluginIds = target.getDependentPluginIds();
    for (PluginId pluginId : dependentPluginIds) {
      PluginDescriptor depPlugin = PluginManager.getPlugin(pluginId);
      // if plugin is not installed
      if (depPlugin == null) {
        PluginDescriptor filtered = ContainerUtil.find(remotePlugins, it -> pluginId.equals(it.getPluginId()));

        if (filtered != null) {
          targets.add(new PlatformOrPluginNode(filtered.getPluginId(), null, filtered));
        }
      }
    }
  }
}
