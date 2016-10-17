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

import com.intellij.ide.plugins.*;
import com.intellij.openapi.application.ex.ApplicationInfoEx;
import com.intellij.openapi.application.impl.ApplicationInfoImpl;
import com.intellij.openapi.extensions.PluginId;
import com.intellij.openapi.progress.ProcessCanceledException;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.updateSettings.impl.UpdateChecker;
import com.intellij.openapi.util.ActionCallback;
import com.intellij.openapi.util.Couple;
import com.intellij.openapi.util.SystemInfo;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.util.containers.ContainerUtil;
import com.intellij.util.ui.UIUtil;
import consulo.ide.updateSettings.UpdateChannel;
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
  @NotNull
  public static String getPlatformPluginId() {
    if (SystemInfo.isWindows) {
      return "consulo-win-no-jre";
    }
    else if (SystemInfo.isMac) {
      return "consulo-mac-no-jre";
    }
    else {
      return "consulo-linux-no-jre";
    }
  }

  public static void showUpdateResult(final PlatformOrPluginUpdateResult targetsForUpdate,
                                      final boolean showConfirmation,
                                      final boolean enableLink,
                                      final boolean alwaysShowResults) {
    PlatformOrPluginUpdateResult.Type type = targetsForUpdate.getType();
    switch (type) {
      case PLATFORM_UPDATE:
        UpdateChecker.showUpdateResult(targetsForUpdate.getPlugins(), showConfirmation, enableLink, alwaysShowResults);
        break;
      case PLUGIN_UPDATE:
        UpdateChecker.showUpdateResult(targetsForUpdate.getPlugins(), showConfirmation, enableLink, alwaysShowResults);
        break;
    }
  }

  public static ActionCallback checkAndNotifyForUpdates(boolean silent, @Nullable ProgressIndicator indicator) {
    ActionCallback actionCallback = new ActionCallback();
    PlatformOrPluginUpdateResult updateResult = checkForUpdates(silent, indicator);
    if (updateResult == PlatformOrPluginUpdateResult.CANCELED) {
      actionCallback.setDone();
      return actionCallback;
    }

    UIUtil.invokeLaterIfNeeded(() -> {
      actionCallback.setDone();

      showUpdateResult(updateResult, true, true, !silent);
    });
    return actionCallback;
  }

  @NotNull
  private static PlatformOrPluginUpdateResult checkForUpdates(final boolean showErrorDialog, @Nullable ProgressIndicator indicator) {
    PluginId platformPluginId = PluginId.getId(getPlatformPluginId());

    ApplicationInfoEx appInfo = ApplicationInfoImpl.getShadowInstance();
    String currentBuildNumber = appInfo.getBuild().asString();

    List<IdeaPluginDescriptor> remotePlugins = Collections.emptyList();
    UpdateChannel channel = consulo.ide.updateSettings.UpdateSettings.getInstance().getChannel();
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
          break;
        }
      }
    }

    final List<Couple<IdeaPluginDescriptor>> targets = new ArrayList<>();
    if(newPlatformPlugin != null) {
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
        UpdateChecker.showErrorMessage(showErrorDialog, e.getMessage());
      }
    }

    if (newPlatformPlugin != null) {
      return new PlatformOrPluginUpdateResult(PlatformOrPluginUpdateResult.Type.PLATFORM_UPDATE, targets);
    }
    return new PlatformOrPluginUpdateResult(PlatformOrPluginUpdateResult.Type.PLUGIN_UPDATE, targets);
  }
}
