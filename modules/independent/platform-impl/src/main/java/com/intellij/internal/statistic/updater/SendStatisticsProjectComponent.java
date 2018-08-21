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
package com.intellij.internal.statistic.updater;

import com.intellij.featureStatistics.FeatureUsageTracker;
import com.intellij.featureStatistics.FeatureUsageTrackerImpl;
import com.intellij.internal.statistic.StatisticsUploadAssistant;
import com.intellij.internal.statistic.connect.StatisticsService;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.ProjectComponent;
import com.intellij.openapi.project.DumbService;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.startup.StartupManager;
import com.intellij.util.Alarm;
import com.intellij.util.Time;
import javax.annotation.Nonnull;

public class SendStatisticsProjectComponent implements ProjectComponent {
  private static final int DELAY_IN_MIN = 10;

  private Project myProject;
  private Alarm myAlarm;

  public SendStatisticsProjectComponent(Project project) {
    myProject = project;
    myAlarm = new Alarm(Alarm.ThreadToUse.POOLED_THREAD, myProject);
  }

  @Override
  public void projectOpened() {
    if (ApplicationManager.getApplication().isUnitTestMode()) return;

    StartupManager.getInstance(myProject).runWhenProjectIsInitialized(new Runnable() {
      @Override
      public void run() {
        runStatisticsService();
      }
    });
  }

  private void runStatisticsService() {
    StatisticsService statisticsService = StatisticsUploadAssistant.getStatisticsService();

    if (System.currentTimeMillis() - Time.DAY > ((FeatureUsageTrackerImpl)FeatureUsageTracker.getInstance()).getFirstRunTime()) {
      return;
    }

    if (StatisticsUploadAssistant.isSendAllowed() && StatisticsUploadAssistant.isTimeToSend()) {
      runWithDelay(statisticsService);
    }
  }

  private void runWithDelay(final @Nonnull StatisticsService statisticsService) {
    myAlarm.addRequest(new Runnable() {
      @Override
      public void run() {
        if (DumbService.isDumb(myProject)) {
          runWithDelay(statisticsService);
        }
        else {
          statisticsService.send();
        }
      }
    }, DELAY_IN_MIN * 60 * 1000);
  }
}
