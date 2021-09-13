/*
 * Copyright 2000-2014 JetBrains s.r.o.
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
package consulo.components.impl.stores;

import com.intellij.diagnostic.IdeErrorsDialog;
import com.intellij.notification.Notification;
import com.intellij.notification.NotificationType;
import com.intellij.openapi.application.Application;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.ShutDownTracker;
import com.intellij.openapi.util.text.StringUtil;
import consulo.application.ApplicationProperties;
import consulo.container.plugin.PluginId;
import consulo.container.plugin.PluginManager;
import consulo.logging.Logger;
import consulo.ui.UIAccess;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;

public final class StoreUtil {
  private static final Logger LOG = Logger.getInstance(StoreUtil.class);

  private StoreUtil() {
  }

  public static void save(@Nonnull IComponentStore stateStore, boolean force, @Nullable Project project) {
    ShutDownTracker.getInstance().registerStopperThread(Thread.currentThread());
    try {
      stateStore.save(force, new ArrayList<>());
    }
    catch (IComponentStore.SaveCancelledException e) {
      LOG.info(e);
    }
    catch (Throwable e) {
      if (ApplicationManager.getApplication().isUnitTestMode()) {
        LOG.error("Save settings failed", e);
      }
      else {
        LOG.warn("Save settings failed", e);
      }

      String messagePostfix =
              " Please restart " + Application.get().getName() + "</p>" + (Application.get().isInternal() ? "<p>" + StringUtil.getThrowableText(e) + "</p>" : "");

      PluginId pluginId = IdeErrorsDialog.findFirstPluginId(e);
      if (pluginId == null) {
        new Notification("Settings Error", "Unable to save settings", "<p>Failed to save settings." + messagePostfix, NotificationType.ERROR).notify(project);
      }
      else {
        if (!ApplicationProperties.isInSandbox()) {
          PluginManager.disablePlugin(pluginId.getIdString());
        }

        new Notification("Settings Error", "Unable to save plugin settings", "<p>The plugin <i>" + pluginId + "</i> failed to save settings and has been disabled." + messagePostfix,
                         NotificationType.ERROR).notify(project);
      }
    }
    finally {
      ShutDownTracker.getInstance().unregisterStopperThread(Thread.currentThread());
    }
  }

  public static void saveAsync(@Nonnull IComponentStore stateStore, @Nonnull UIAccess uiAccess, @Nullable Project project) {
    ShutDownTracker.getInstance().registerStopperThread(Thread.currentThread());
    try {
      stateStore.saveAsync(uiAccess, new ArrayList<>());
    }
    catch (IComponentStore.SaveCancelledException e) {
      LOG.info(e);
    }
    catch (Throwable e) {
      if (ApplicationManager.getApplication().isUnitTestMode()) {
        LOG.error("Save settings failed", e);
      }
      else {
        LOG.warn("Save settings failed", e);
      }

      uiAccess.give(() -> {
        String messagePostfix =
                " Please restart " + Application.get().getName() + "</p>" + (Application.get().isInternal() ? "<p>" + StringUtil.getThrowableText(e) + "</p>" : "");

        PluginId pluginId = IdeErrorsDialog.findFirstPluginId(e);
        if (pluginId == null) {
          new Notification("Settings Error", "Unable to save settings", "<p>Failed to save settings." + messagePostfix, NotificationType.ERROR).notify(project);
        }
        else {
          if (!ApplicationProperties.isInSandbox()) {
            PluginManager.disablePlugin(pluginId.getIdString());
          }

          new Notification("Settings Error", "Unable to save plugin settings", "<p>The plugin <i>" + pluginId + "</i> failed to save settings and has been disabled." + messagePostfix,
                           NotificationType.ERROR).notify(project);
        }
      });
    }
    finally {
      ShutDownTracker.getInstance().unregisterStopperThread(Thread.currentThread());
    }
  }
}
