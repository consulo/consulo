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
import com.intellij.ide.plugins.*;
import com.intellij.ide.util.PropertiesComponent;
import com.intellij.notification.*;
import com.intellij.openapi.application.Application;
import com.intellij.openapi.application.ApplicationInfo;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.PermanentInstallationID;
import com.intellij.openapi.extensions.PluginId;
import com.intellij.openapi.progress.ProcessCanceledException;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.project.ProjectManager;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.util.ActionCallback;
import com.intellij.openapi.util.Condition;
import com.intellij.openapi.util.Couple;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.util.Function;
import com.intellij.util.containers.ContainerUtil;
import com.intellij.util.text.DateFormatUtil;
import com.intellij.util.ui.UIUtil;
import consulo.annotations.DeprecationInfo;
import consulo.lombok.annotations.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.event.HyperlinkEvent;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author mike
 *         Date: Oct 31, 2002
 */
@Logger
public final class UpdateChecker {
  private UpdateChecker() {
  }

  public static boolean checkNeeded() {
    final UpdateSettings settings = UpdateSettings.getInstance();
    if (settings == null) return false;

    final long timeDelta = System.currentTimeMillis() - settings.getLastTimeChecked();
    if (Math.abs(timeDelta) < DateFormatUtil.DAY) return false;

    return settings.isCheckNeeded();
  }

  public static ActionCallback updateAndShowResult() {
    final ActionCallback result = new ActionCallback();
    final Application app = ApplicationManager.getApplication();
    final UpdateSettings updateSettings = UpdateSettings.getInstance();
    if (!updateSettings.isCheckNeeded()) {
      result.setDone();
      return result;
    }
    app.executeOnPooledThread(new Runnable() {
      @Override
      public void run() {
        final List<Couple<IdeaPluginDescriptor>> updatedPlugins = loadPluginsForUpdate(false, null);
        app.invokeLater(new Runnable() {
          @Override
          public void run() {
            showUpdateResult(updatedPlugins, true, true, false);
            result.setDone();
          }
        });
      }
    });
    return result;
  }

  /**
   * Return list of couple PluginDescriptor. First is current plugin, Second is target for update
   */
  @Nullable
  public static List<Couple<IdeaPluginDescriptor>> loadPluginsForUpdate(final boolean showErrorDialog,
                                                                        @Nullable ProgressIndicator indicator) {
    final List<Couple<IdeaPluginDescriptor>> targets = new ArrayList<>();
    List<IdeaPluginDescriptor> remotePluginDescriptors = new ArrayList<>();
    try {
      remotePluginDescriptors.addAll(RepositoryHelper.loadPluginsFromRepository(indicator, consulo.ide.updateSettings.UpdateSettings.getInstance().getChannel()));
    }
    catch (ProcessCanceledException e) {
      return null;
    }
    catch (Exception e) {
      LOGGER.info(e);
    }

    final Map<PluginId, IdeaPluginDescriptor> ourPlugins = new HashMap<PluginId, IdeaPluginDescriptor>();
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

          List<IdeaPluginDescriptor> filter = ContainerUtil.filter(remotePluginDescriptors, new Condition<IdeaPluginDescriptor>() {
            @Override
            public boolean value(IdeaPluginDescriptor ideaPluginDescriptor) {
              return pluginId.equals(ideaPluginDescriptor.getPluginId());
            }
          });

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
        return null;
      }
      catch (Exception e) {
        showErrorMessage(showErrorDialog, e.getMessage());
      }
    }

    return targets;
  }

  private static void showErrorMessage(boolean showErrorDialog, final String failedMessage) {
    if (showErrorDialog) {
      UIUtil.invokeLaterIfNeeded(new Runnable() {
        @Override
        public void run() {
          Messages.showErrorDialog(failedMessage, IdeBundle.message("title.connection.error"));
        }
      });
    }
    else {
      LOGGER.info(failedMessage);
    }
  }

  private static boolean ourUpdateInfoDialogShown = false;

  public static void showUpdateResult(final List<Couple<IdeaPluginDescriptor>> targetsForUpdate,
                                      final boolean showConfirmation,
                                      final boolean enableLink,
                                      final boolean alwaysShowResults) {
    final boolean showBalloonNotification = !alwaysShowResults && ProjectManager.getInstance().getOpenProjects().length > 0;


    final Runnable showPluginsUpdateDialogRunnable = new Runnable() {
      @Override
      public void run() {
        final NoUpdatesDialog dialog = new NoUpdatesDialog(true, targetsForUpdate, enableLink) {
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
      final String updatedPluginsList = StringUtil.join(targetsForUpdate, new Function<Couple<IdeaPluginDescriptor>, String>() {
        @Override
        public String fun(Couple<IdeaPluginDescriptor> downloader) {
          return downloader.getSecond().getName();
        }
      }, ", ");
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
