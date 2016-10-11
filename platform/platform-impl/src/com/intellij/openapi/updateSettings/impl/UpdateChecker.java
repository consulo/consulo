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
package com.intellij.openapi.updateSettings.impl;

import com.intellij.ide.IdeBundle;
import com.intellij.ide.plugins.IdeaPluginDescriptor;
import com.intellij.ide.plugins.InstalledPluginsTableModel;
import com.intellij.ide.plugins.PluginManager;
import com.intellij.ide.util.PropertiesComponent;
import com.intellij.notification.*;
import com.intellij.openapi.application.Application;
import com.intellij.openapi.application.ApplicationInfo;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.PermanentInstallationID;
import com.intellij.openapi.project.ProjectManager;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.util.ActionCallback;
import com.intellij.openapi.util.Couple;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.util.text.DateFormatUtil;
import com.intellij.util.ui.UIUtil;
import consulo.annotations.DeprecationInfo;
import consulo.ide.updateSettings.impl.PlatformOrPluginUpdateChecker;
import consulo.lombok.annotations.Logger;
import org.jetbrains.annotations.NotNull;

import javax.swing.event.HyperlinkEvent;
import java.io.IOException;
import java.util.List;

/**
 * @author mike
 *         Date: Oct 31, 2002
 */
@Logger
public final class UpdateChecker {
  private UpdateChecker() {
  }

  public static boolean checkNeeded() {
    consulo.ide.updateSettings.UpdateSettings updateSettings = consulo.ide.updateSettings.UpdateSettings.getInstance();
    if (!updateSettings.isEnable()) {
      return false;
    }

    final long timeDelta = System.currentTimeMillis() - updateSettings.getLastTimeCheck();
    return Math.abs(timeDelta) >= DateFormatUtil.DAY;
  }

  public static ActionCallback updateAndShowResult() {
    final ActionCallback result = new ActionCallback();
    final Application app = ApplicationManager.getApplication();
    final consulo.ide.updateSettings.UpdateSettings updateSettings = consulo.ide.updateSettings.UpdateSettings.getInstance();
    if (!updateSettings.isEnable()) {
      result.setDone();
      return result;
    }
    app.executeOnPooledThread(() -> {
      PlatformOrPluginUpdateChecker.checkAndNotifyForUpdates(true, null).notify(result);
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

  private static boolean ourUpdateInfoDialogShown = false;

  @Deprecated
  public static void showUpdateResult(final List<Couple<IdeaPluginDescriptor>> targetsForUpdate,
                                      final boolean showConfirmation,
                                      final boolean enableLink,
                                      final boolean alwaysShowResults) {
    final boolean showBalloonNotification = !alwaysShowResults && ProjectManager.getInstance().getOpenProjects().length > 0;


    final Runnable showPluginsUpdateDialogRunnable = new Runnable() {
      @Override
      public void run() {
        final SelectUpdateDialog dialog = new SelectUpdateDialog(targetsForUpdate, enableLink) {
          @Override
          protected void dispose() {
            ourUpdateInfoDialogShown = false;
            super.dispose();
          }
        };
        dialog.setShowConfirmation(showConfirmation);
        ourUpdateInfoDialogShown = true;
        dialog.show();
      }
    };
    if (showBalloonNotification && targetsForUpdate != null) {
      final String updatedPluginsList = StringUtil.join(targetsForUpdate, downloader -> downloader.getSecond().getName(), ", ");
      String message = "You have the latest version of " + ApplicationInfo.getInstance().getVersionName() + " installed.<br> ";
      message += "The following plugin" + (targetsForUpdate.size() == 1 ? " is" : "s are") + " ready to <a href=\"update\">update</a>: " + updatedPluginsList;
      showBalloonNotification(showPluginsUpdateDialogRunnable, message);
    }
    else if ((targetsForUpdate != null || alwaysShowResults) && !ourUpdateInfoDialogShown) {
      showPluginsUpdateDialogRunnable.run();
    }
  }

  private static void showBalloonNotification(final Runnable showUpdatesDialogRunnable, String message) {
    new NotificationGroup(IdeBundle.message("update.available.group"), NotificationDisplayType.STICKY_BALLOON, true)
            .createNotification(IdeBundle.message("updates.info.dialog.title"), message, NotificationType.INFORMATION, new NotificationListener() {
              @Override
              public void hyperlinkUpdate(@NotNull Notification notification, @NotNull HyperlinkEvent event) {
                notification.expire();
                showUpdatesDialogRunnable.run();
              }
            }).notify(null);
  }

  @Deprecated
  @DeprecationInfo("Use PermanentInstallationID#get()")
  public static String getInstallationUID(final PropertiesComponent propertiesComponent) {
    return PermanentInstallationID.get();
  }

  public static boolean install(List<PluginDownloader> downloaders) {
    boolean installed = false;
    for (PluginDownloader downloader : downloaders) {
      final IdeaPluginDescriptor descriptor = downloader.getDescriptor();
      if (descriptor != null) {
        try {
          InstalledPluginsTableModel.updateExistingPlugin(descriptor, PluginManager.getPlugin(descriptor.getPluginId()));
          downloader.install(true);
          installed = true;
        }
        catch (IOException e) {
          LOGGER.info(e);
        }
      }
    }
    return installed;
  }
}
