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
import com.intellij.notification.*;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.application.Application;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.PathManager;
import com.intellij.openapi.application.ex.ApplicationInfoEx;
import com.intellij.openapi.application.impl.ApplicationInfoImpl;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.extensions.PluginId;
import com.intellij.openapi.progress.ProcessCanceledException;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.util.ActionCallback;
import com.intellij.openapi.util.Couple;
import com.intellij.openapi.util.SystemInfo;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.util.ArrayUtil;
import com.intellij.util.containers.ContainerUtil;
import com.intellij.util.text.DateFormatUtil;
import com.intellij.util.ui.UIUtil;
import consulo.ide.updateSettings.UpdateChannel;
import consulo.ide.updateSettings.UpdateSettings;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.util.*;

/**
 * @author VISTALL
 * @since 10-Oct-16
 */
public class PlatformOrPluginUpdateChecker {
  public static final Logger LOGGER = Logger.getInstance(PlatformOrPluginUpdateChecker.class);

  private static final NotificationGroup ourGroup = new NotificationGroup("Platform Or Plugins Update", NotificationDisplayType.STICKY_BALLOON, false);

  private static final PluginId ourWinNoJre = PluginId.getId("consulo-win-no-jre");
  private static final PluginId ourWin = PluginId.getId("consulo-win");
  private static final PluginId ourWin64 = PluginId.getId("consulo-win64");
  private static final PluginId ourLinuxNoJre = PluginId.getId("consulo-linux-no-jre");
  private static final PluginId ourLinux = PluginId.getId("consulo-linux");
  private static final PluginId ourLinux64 = PluginId.getId("consulo-linux64");
  private static final PluginId ourMacNoJre = PluginId.getId("consulo-mac-no-jre");
  private static final PluginId ourMac64 = PluginId.getId("consulo-mac64");

  private static final PluginId[] ourPlatformIds = {ourWinNoJre, ourWin, ourWin64, ourLinuxNoJre, ourLinux, ourLinux64, ourMacNoJre, ourMac64};

  @NotNull
  public static PluginId getPlatformPluginId() {
    boolean isJreBuild = new File(PathManager.getHomePath(), "jre").exists();
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

  public static boolean isPlatform(@NotNull PluginId pluginId) {
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

  public static ActionCallback updateAndShowResult() {
    final ActionCallback result = new ActionCallback();
    final Application app = ApplicationManager.getApplication();
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
      LOGGER.info(failedMessage);
    }
  }

  public static void showUpdateResult(@Nullable Project project, final PlatformOrPluginUpdateResult targetsForUpdate, final boolean showResults) {
    PlatformOrPluginUpdateResult.Type type = targetsForUpdate.getType();
    switch (type) {
      case NO_UPDATE:
        if (showResults) {
          ourGroup.createNotification(IdeBundle.message("update.available.group"), "There no updates", NotificationType.INFORMATION, null).notify(project);
        }
        break;
      case PLUGIN_UPDATE:
      case PLATFORM_UPDATE:
        if (showResults) {
          new PluginListDialog(project, targetsForUpdate, null, null).show();
        }
        else {
          Notification notification =
                  ourGroup.createNotification(IdeBundle.message("update.available.group"), "Updates available", NotificationType.INFORMATION, null);
          notification.addAction(new NotificationAction("View updates") {
            @Override
            public void actionPerformed(@NotNull AnActionEvent e, @NotNull Notification notification) {
              new PluginListDialog(project, targetsForUpdate, null, null).show();
            }
          });
          notification.notify(project);
        }
        break;
    }
  }

  public static ActionCallback checkAndNotifyForUpdates(@Nullable Project project, boolean showResults, @Nullable ProgressIndicator indicator) {
    ActionCallback actionCallback = new ActionCallback();
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

  @NotNull
  private static PlatformOrPluginUpdateResult checkForUpdates(final boolean showResults, @Nullable ProgressIndicator indicator) {
    PluginId platformPluginId = getPlatformPluginId();

    ApplicationInfoEx appInfo = ApplicationInfoImpl.getShadowInstance();
    String currentBuildNumber = appInfo.getBuild().asString();

    List<IdeaPluginDescriptor> remotePlugins = Collections.emptyList();
    UpdateChannel channel = UpdateSettings.getInstance().getChannel();
    try {
      remotePlugins = RepositoryHelper.loadPluginsFromRepository(indicator, channel);
    }
    catch (ProcessCanceledException e) {
      return PlatformOrPluginUpdateResult.CANCELED;
    }
    catch (Exception e) {
      LOGGER.info(e);
    }

    IdeaPluginDescriptor newPlatformPlugin = null;
    // try to search platform number
    for (IdeaPluginDescriptor pluginDescriptor : remotePlugins) {
      PluginId pluginId = pluginDescriptor.getPluginId();
      if (platformPluginId.equals(pluginId)) {
        if (StringUtil.compareVersionNumbers(pluginDescriptor.getVersion(), currentBuildNumber) > 0) {
          // change current build
          currentBuildNumber = pluginDescriptor.getVersion();
          newPlatformPlugin = pluginDescriptor;

          // FIXME [VISTALL]  drop it
          ((PluginNode)pluginDescriptor).setName("Platform");
          break;
        }
      }
    }

    final List<Couple<IdeaPluginDescriptor>> targets = new ArrayList<>();
    if (newPlatformPlugin != null) {
      PluginNode thisPlatform = new PluginNode(platformPluginId);
      thisPlatform.setVersion(appInfo.getBuild().asString());
      thisPlatform.setName(newPlatformPlugin.getName());

      targets.add(Couple.of(thisPlatform, newPlatformPlugin));

      // load new plugins with new app build
      try {
        remotePlugins = RepositoryHelper.loadPluginsFromRepository(indicator, channel, currentBuildNumber);
      }
      catch (ProcessCanceledException e) {
        return PlatformOrPluginUpdateResult.CANCELED;
      }
      catch (Exception e) {
        LOGGER.info(e);
      }
    }

    final Map<PluginId, IdeaPluginDescriptor> ourPlugins = new HashMap<>();
    final IdeaPluginDescriptor[] installedPlugins = PluginManagerCore.getPlugins();
    final List<String> disabledPlugins = PluginManagerCore.getDisabledPlugins();
    for (IdeaPluginDescriptor installedPlugin : installedPlugins) {
      if (!installedPlugin.isBundled() && !disabledPlugins.contains(installedPlugin.getPluginId().getIdString())) {
        ourPlugins.put(installedPlugin.getPluginId(), installedPlugin);
      }
    }

    final PluginManagerUISettings updateSettings = PluginManagerUISettings.getInstance();
    updateSettings.myOutdatedPlugins.clear();
    if (!ourPlugins.isEmpty()) {
      try {
        for (final Map.Entry<PluginId, IdeaPluginDescriptor> entry : ourPlugins.entrySet()) {
          final PluginId pluginId = entry.getKey();

          IdeaPluginDescriptor filtered = ContainerUtil.find(remotePlugins, it -> pluginId.equals(it.getPluginId()));

          if (filtered == null) {
            continue;
          }

          if (StringUtil.compareVersionNumbers(filtered.getVersion(), entry.getValue().getVersion()) > 0) {
            updateSettings.myOutdatedPlugins.add(pluginId.toString());

            processDependencies(filtered, targets, remotePlugins);

            targets.add(Couple.of(entry.getValue(), filtered));
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

    return targets.isEmpty()
           ? PlatformOrPluginUpdateResult.NO_UPDATE
           : new PlatformOrPluginUpdateResult(PlatformOrPluginUpdateResult.Type.PLUGIN_UPDATE, targets);
  }

  private static void processDependencies(@NotNull IdeaPluginDescriptor target,
                                          List<Couple<IdeaPluginDescriptor>> targets,
                                          List<IdeaPluginDescriptor> remotePlugins) {
    PluginId[] dependentPluginIds = target.getDependentPluginIds();
    for (PluginId pluginId : dependentPluginIds) {
      IdeaPluginDescriptor depPlugin = PluginManager.getPlugin(pluginId);
      // if plugin is not installed
      if (depPlugin == null) {
        IdeaPluginDescriptor filtered = ContainerUtil.find(remotePlugins, it -> pluginId.equals(it.getPluginId()));

        if (filtered != null) {
          targets.add(Couple.of(null, filtered));
        }
      }
    }
  }
}
