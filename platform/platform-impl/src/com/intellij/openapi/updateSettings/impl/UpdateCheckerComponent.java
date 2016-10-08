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

import com.intellij.openapi.components.ApplicationComponent;
import com.intellij.openapi.util.Disposer;
import com.intellij.util.Alarm;
import com.intellij.util.text.DateFormatUtil;
import org.jetbrains.annotations.NotNull;

/**
 * @author yole
 */
public class UpdateCheckerComponent implements ApplicationComponent {
  private static final long ourCheckInterval = DateFormatUtil.DAY;
  private final Alarm myCheckForUpdatesAlarm = new Alarm(Alarm.ThreadToUse.SWING_THREAD);
  private final Runnable myCheckRunnable = () -> UpdateChecker.updateAndShowResult().doWhenDone(() -> queueNextUpdateCheck(ourCheckInterval));

  @Override
  public void initComponent() {
    final long interval = consulo.ide.updateSettings.UpdateSettings.getInstance().getLastTimeCheck() + ourCheckInterval - System.currentTimeMillis();
    queueNextUpdateCheck(UpdateChecker.checkNeeded() ? ourCheckInterval : Math.max(interval, DateFormatUtil.MINUTE));
  }

  private void queueNextUpdateCheck(long interval) {
    myCheckForUpdatesAlarm.addRequest(myCheckRunnable, interval);
  }

  @Override
  public void disposeComponent() {
    Disposer.dispose(myCheckForUpdatesAlarm);
  }

  @NotNull
  @Override
  public String getComponentName() {
    return "UpdateCheckerComponent";
  }
}
