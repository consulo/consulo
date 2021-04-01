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
package com.intellij.openapi.updateSettings.impl;

import com.intellij.ide.actions.SettingsEntryPointAction;
import com.intellij.util.concurrency.AppExecutorUtil;
import com.intellij.util.text.DateFormatUtil;
import consulo.disposer.Disposable;
import consulo.ide.updateSettings.UpdateSettings;
import consulo.ide.updateSettings.impl.PlatformOrPluginUpdateChecker;
import consulo.ide.updateSettings.impl.PlatformOrPluginUpdateResult;
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
public class UpdateCheckerComponent implements Disposable {
  private static final long ourCheckInterval = DateFormatUtil.DAY;

  private final UpdateSettings myUpdateSettings;
  private final Runnable myCheckRunnable;

  private Future<?> myCheckFuture = CompletableFuture.completedFuture(null);

  @Inject
  public UpdateCheckerComponent(@Nonnull UpdateSettings updateSettings) {
    myUpdateSettings = updateSettings;

    myCheckRunnable = () -> PlatformOrPluginUpdateChecker.updateAndShowResult().doWhenDone(() -> {
      myUpdateSettings.setLastTimeCheck(System.currentTimeMillis());
      queueNextUpdateCheck(ourCheckInterval);
    });

    final long interval = myUpdateSettings.getLastTimeCheck() + ourCheckInterval - System.currentTimeMillis();
    queueNextUpdateCheck(PlatformOrPluginUpdateChecker.checkNeeded() ? ourCheckInterval : Math.max(interval, DateFormatUtil.MINUTE));

    // reset on restart
    PlatformOrPluginUpdateResult.Type lastCheckResult = updateSettings.getLastCheckResult();
    if(lastCheckResult == PlatformOrPluginUpdateResult.Type.RESTART_REQUIRED) {
      updateSettings.setLastCheckResult(PlatformOrPluginUpdateResult.Type.NO_UPDATE);
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
