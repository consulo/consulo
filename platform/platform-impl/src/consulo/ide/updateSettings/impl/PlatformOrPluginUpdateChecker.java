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
import com.intellij.openapi.application.ex.ApplicationInfoEx;
import com.intellij.openapi.application.impl.ApplicationInfoImpl;
import com.intellij.openapi.extensions.PluginId;
import com.intellij.openapi.progress.ProcessCanceledException;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.util.ActionCallback;
import com.intellij.openapi.util.Couple;
import com.intellij.openapi.util.SystemInfo;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.util.containers.ContainerUtil;
import com.intellij.util.text.DateFormatUtil;
import com.intellij.util.ui.UIUtil;
import consulo.ide.updateSettings.UpdateChannel;
import consulo.ide.updateSettings.UpdateSettings;
import consulo.lombok.annotations.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

/**
 * @author VISTALL
 * @since 10-Oct-16
 */
@Logger
public class PlatformOrPluginUpdateChecker {
  private static final NotificationGroup ourGroup = new NotificationGroup("platformOrPluginUpdate", NotificationDisplayType.STICKY_BALLOON, false);

  private static final PluginId ourWinNoJre = PluginId.getId("consulo-win-no-jre");
  private static final PluginId ourMacNoJre = PluginId.getId("consulo-mac-no-jre");
  private static final PluginId ourLinuxNoJre = PluginId.getId("consulo-linux-no-jre");

  public static final PluginId[] ourPlatformIds = {ourWinNoJre, ourLinuxNoJre, ourMacNoJre};

  @NotNull
  public static PluginId getPlatformPluginId() {
    if (SystemInfo.isWindows) {
      return ourWinNoJre;
    }
    else if (SystemInfo.isMac) {
      return ourMacNoJre;
    }
    else {
      return ourLinuxNoJre;
    }
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
    app.executeOnPooledThread(() -> {
      checkAndNotifyForUpdates(null, false, null).notify(result);
    });
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
      case PLATFORM_UPDATE:
        if (showResults) {
          showDialog(project, targetsForUpdate);
        }
        else {
          Notification notification =
                  ourGroup.createNotification(IdeBundle.message("update.available.group"), "Updates available", NotificationType.INFORMATION, null);
          notification.addAction(new NotificationAction("View updates") {
            @Override
            public void actionPerformed(@NotNull AnActionEvent e, @NotNull Notification notification) {
              showDialog(project, targetsForUpdate);
            }
          });
          notification.notify(project);
        }
        break;
    }
  }

  private static void showDialog(@Nullable Project project, PlatformOrPluginUpdateResult targetsForUpdate) {
    new PluginListDialog(project, targetsForUpdate).show();
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

          List<IdeaPluginDescriptor> filter = ContainerUtil.filter(remotePlugins, it -> pluginId.equals(it.getPluginId()));

          if (filter.isEmpty()) {
            continue;
          }

          for (IdeaPluginDescriptor filtered : filter) {
            if (StringUtil.compareVersionNumbers(filtered.getVersion(), entry.getValue().getVersion()) > 0) {
              updateSettings.myOutdatedPlugins.add(pluginId.toString());

              targets.add(Couple.of(entry.getValue(), filtered));
            }
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
}
