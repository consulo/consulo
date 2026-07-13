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
package consulo.component.store.internal;

import consulo.application.Application;
import consulo.application.ApplicationManager;
import consulo.component.ComponentManager;
import consulo.component.util.PluginExceptionUtil;
import consulo.container.plugin.PluginId;
import consulo.logging.Logger;
import consulo.ui.NotificationType;
import consulo.ui.UIAccess;
import consulo.util.concurrent.coroutine.Continuation;
import consulo.util.lang.ExceptionUtil;
import consulo.util.lang.ShutDownTracker;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.function.Consumer;

public final class StoreUtil {
  private static final Logger LOG = Logger.getInstance(StoreUtil.class);

  private StoreUtil() {
  }

  public static Continuation<?> saveAsync(IComponentStore stateStore, UIAccess uiAccess, @Nullable ComponentManager project) {
    Thread stopperThread = Thread.currentThread();
    ShutDownTracker.getInstance().registerStopperThread(stopperThread);

    return attachSaveHandlers(stateStore.saveAsync(uiAccess, new ArrayList<>()), stopperThread, uiAccess, project);
  }

  private static <T> Continuation<T> attachSaveHandlers(Continuation<T> continuation, Thread stopperThread, UIAccess uiAccess, @Nullable ComponentManager project) {
    return continuation
      .onFinish(c -> ShutDownTracker.getInstance().unregisterStopperThread(stopperThread))
      .onError(onSaveError(uiAccess, project));
  }

  public static <T> Consumer<Continuation<T>> onSaveError(UIAccess uiAccess, @Nullable ComponentManager project) {
    return continuation -> {
      Throwable e = continuation.getError();
      if (e == null) {
        return;
      }

      handleSaveError(uiAccess, project, e);
      continuation.errorHandled();
    };
  }

  public static void handleSaveError(UIAccess uiAccess, @Nullable ComponentManager project, Throwable e) {
    if (e instanceof IComponentStore.SaveCancelledException) {
      LOG.info(e);
      return;
    }

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
        StorageNotificationService.getInstance()
                .notify(NotificationType.ERROR, "Unable to save plugin settings", "<p>The plugin <i>" + pluginId + "</i> failed to save settings." + messagePostfix,
                        project);
      }
    });
  }
}
