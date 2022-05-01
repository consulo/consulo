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
package consulo.ide.impl.idea.internal.statistic.updater;

import consulo.ide.impl.idea.internal.statistic.persistence.UsageStatisticsPersistenceComponent;
import consulo.project.ui.notification.Notification;
import consulo.project.ui.notification.event.NotificationListener;
import consulo.project.ui.notification.NotificationType;
import consulo.application.Application;
import consulo.application.impl.internal.ApplicationInfo;
import consulo.application.impl.internal.ApplicationNamesInfo;
import consulo.ide.impl.idea.openapi.application.PermanentInstallationID;
import consulo.project.Project;
import consulo.project.ProjectManager;
import consulo.project.event.ProjectManagerListener;
import consulo.application.util.concurrent.AppExecutorUtil;
import consulo.disposer.Disposable;
import consulo.ide.impl.external.api.StatisticsBean;
import consulo.ide.impl.externalService.impl.WebServiceApi;
import consulo.ide.impl.externalService.impl.WebServiceApiSender;
import consulo.ide.impl.externalService.impl.statistics.SendStatisticsUtil;
import consulo.logging.Logger;
import consulo.ui.UIAccess;
import jakarta.inject.Inject;
import jakarta.inject.Provider;
import jakarta.inject.Singleton;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.IOException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

@Singleton
public class StatisticsSendManager implements Disposable {
  private static final Logger LOG = Logger.getInstance(StatisticsSendManager.class);

  private static final int DELAY_IN_MIN = 10;

  private final Provider<UsageStatisticsPersistenceComponent> myUsageStatisticsComponent;

  private Future<?> myFuture;

  @Inject
  private StatisticsSendManager(Application application, Provider<UsageStatisticsPersistenceComponent> usageStatisticsComponent) {
    myUsageStatisticsComponent = usageStatisticsComponent;

    application.getMessageBus().connect().subscribe(ProjectManager.TOPIC, new ProjectManagerListener() {
      @Override
      public void projectOpened(@Nonnull Project project, @Nonnull UIAccess uiAccess) {
        runStatisticsService();
      }
    });
  }

  @Override
  public void dispose() {
    if (myFuture != null) {
      myFuture.cancel(false);
      myFuture = null;
    }
  }

  public void sheduleRunIfStarted() {
    if (myFuture != null) {
      myFuture.cancel(false);
      myFuture = null;

      runStatisticsService();
    }
  }

  private void runStatisticsService() {
    if(!myUsageStatisticsComponent.get().isAllowed()) {
      if (myFuture != null) {
        myFuture.cancel(false);
        myFuture = null;
      }
      return;
    }

    if (myFuture != null) {
      return;
    }

    long oneDayAfterStart = PermanentInstallationID.date() + TimeUnit.DAYS.toMillis(1);

    // one day after installation
    if(oneDayAfterStart > System.currentTimeMillis()) {
      return;
    }

    if (myUsageStatisticsComponent.get().isTimeToSend()) {
      runWithDelay();
    }
  }

  private void runWithDelay() {
    myFuture = AppExecutorUtil.getAppScheduledExecutorService().schedule((Runnable)() -> {
      try {
        UsageStatisticsPersistenceComponent component = myUsageStatisticsComponent.get();
        
        sendNow(component);
      }
      finally {
        myFuture = null;
      }
    }, DELAY_IN_MIN, TimeUnit.MINUTES);
  }

  public void sendNow(UsageStatisticsPersistenceComponent component) {
    StatisticsBean bean = SendStatisticsUtil.getBean(component);

    if (bean.groups.length == 0) {
      component.setSentTime(System.currentTimeMillis());
      return;
    }

    try {
      WebServiceApiSender.doPost(WebServiceApi.STATISTICS_API, "push", bean, Object.class);

      component.setSentTime(System.currentTimeMillis());
    }
    catch (IOException e) {
      LOG.warn(e);
    }
  }

  // FIXME [VISTALL] at current moment we not show this notification
  public Notification createNotification(@Nonnull final String groupDisplayId, @Nullable NotificationListener listener) {
    final String fullProductName = ApplicationNamesInfo.getInstance().getFullProductName();
    final String companyName = ApplicationInfo.getInstance().getCompanyName();

    String text = "<html>Please click <a href='allow'>I agree</a> if you want to help make " +
                  fullProductName +
                  " better or <a href='decline'>I don't agree</a> otherwise. <a href='settings'>more...</a></html>";

    String title = "Help improve " + fullProductName + " by sending anonymous usage statistics to " + companyName;

    return new Notification(groupDisplayId, title, text, NotificationType.INFORMATION, listener);
  }
}
