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
package consulo.component.store.impl.internal;

import consulo.application.Application;
import consulo.application.ApplicationManager;
import consulo.application.ApplicationProperties;
import consulo.component.ComponentManager;
import consulo.component.util.PluginExceptionUtil;
import consulo.container.plugin.PluginId;
import consulo.container.plugin.PluginManager;
import consulo.logging.Logger;
import consulo.ui.NotificationType;
import consulo.ui.UIAccess;
import consulo.util.lang.ExceptionUtil;
import consulo.util.lang.ShutDownTracker;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import java.util.ArrayList;

public final class StoreUtil {
  private static final Logger LOG = Logger.getInstance(StoreUtil.class);

  private StoreUtil() {
  }

  public static void save(@Nonnull IComponentStore stateStore, boolean force, @Nullable ComponentManager project) {
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

      String messagePostfix = " Please restart " + Application.get().getName() + "</p>" + (Application.get().isInternal() ? "<p>" + ExceptionUtil.getThrowableText(e) + "</p>" : "");

      PluginId pluginId = PluginExceptionUtil.findFirstPluginId(e);

      if (pluginId == null) {
        StorageNotificationService.getInstance().notify(NotificationType.ERROR, "Unable to save settings", "<p>Failed to save settings." + messagePostfix, project);
      }
      else {
        if (!ApplicationProperties.isInSandbox()) {
          PluginManager.disablePlugin(pluginId);
        }

        StorageNotificationService.getInstance()
                .notify(NotificationType.ERROR, "Unable to save plugin settings", "<p>The plugin <i>" + pluginId + "</i> failed to save settings and has been " + "disabled." + messagePostfix,
                        project);
      }
    }
    finally {
      ShutDownTracker.getInstance().unregisterStopperThread(Thread.currentThread());
    }
  }

  public static void saveAsync(@Nonnull IComponentStore stateStore, @Nonnull UIAccess uiAccess, @Nullable ComponentManager project) {
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
        String messagePostfix = " Please restart " + Application.get().getName() + "</p>" + (Application.get().isInternal() ? "<p>" + ExceptionUtil.getThrowableText(e) + "</p>" : "");

        PluginId pluginId = PluginExceptionUtil.findFirstPluginId(e);

        if (pluginId == null) {
          StorageNotificationService.getInstance().notify(NotificationType.ERROR, "Unable to save settings", "<p>Failed to save settings." + messagePostfix, project);
        }
        else {
          if (!ApplicationProperties.isInSandbox()) {
            PluginManager.disablePlugin(pluginId);
          }

          StorageNotificationService.getInstance()
                  .notify(NotificationType.ERROR, "Unable to save plugin settings", "<p>The plugin <i>" + pluginId + "</i> failed to save settings and has been " + "disabled." + messagePostfix,
                          project);
        }
      });
    }
    finally {
      ShutDownTracker.getInstance().unregisterStopperThread(Thread.currentThread());
    }
  }
}
