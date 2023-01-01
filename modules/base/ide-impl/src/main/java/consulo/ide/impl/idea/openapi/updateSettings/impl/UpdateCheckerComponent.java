/*
 * Copyright 2000-2012 JetBrains s.r.o.
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
package consulo.ide.impl.idea.openapi.updateSettings.impl;

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ServiceAPI;
import consulo.annotation.component.ServiceImpl;
import consulo.application.util.DateFormatUtil;
import consulo.application.util.concurrent.AppExecutorUtil;
import consulo.disposer.Disposable;
import consulo.externalService.update.UpdateSettings;
import consulo.ide.impl.idea.ide.actions.SettingsEntryPointAction;
import consulo.ide.impl.updateSettings.UpdateSettingsImpl;
import consulo.ide.impl.updateSettings.impl.PlatformOrPluginUpdateChecker;
import consulo.ide.impl.updateSettings.impl.PlatformOrPluginUpdateResult;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;

import javax.annotation.Nonnull;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

/**
 * @author yole
 */
@Singleton
@ServiceAPI(value = ComponentScope.APPLICATION, lazy = false)
@ServiceImpl
public class UpdateCheckerComponent implements Disposable {
  private static final long ourCheckInterval = DateFormatUtil.DAY;

  private final UpdateSettingsImpl myUpdateSettings;
  private final Runnable myCheckRunnable;

  private Future<?> myCheckFuture = CompletableFuture.completedFuture(null);

  @Inject
  public UpdateCheckerComponent(@Nonnull UpdateSettings updateSettings) {
    myUpdateSettings = (UpdateSettingsImpl)updateSettings;

    myCheckRunnable = () -> PlatformOrPluginUpdateChecker.updateAndShowResult().doWhenDone(() -> {
      myUpdateSettings.setLastTimeCheck(System.currentTimeMillis());
      queueNextUpdateCheck(ourCheckInterval);
    });

    final long interval = myUpdateSettings.getLastTimeCheck() + ourCheckInterval - System.currentTimeMillis();
    queueNextUpdateCheck(PlatformOrPluginUpdateChecker.checkNeeded() ? ourCheckInterval : Math.max(interval, DateFormatUtil.MINUTE));

    // reset on restart
    PlatformOrPluginUpdateResult.Type lastCheckResult = myUpdateSettings.getLastCheckResult();
    if(lastCheckResult == PlatformOrPluginUpdateResult.Type.RESTART_REQUIRED) {
      myUpdateSettings.setLastCheckResult(PlatformOrPluginUpdateResult.Type.NO_UPDATE);
      SettingsEntryPointAction.updateState(updateSettings);
    }
  }

  private void queueNextUpdateCheck(long interval) {
    myCheckFuture = AppExecutorUtil.getAppScheduledExecutorService().schedule(myCheckRunnable, interval, TimeUnit.MILLISECONDS);
  }

  @Override
  public void dispose() {
    myCheckFuture.cancel(false);
  }
}
