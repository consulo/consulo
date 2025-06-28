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
package consulo.externalService.impl.internal.statistic;

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ServiceAPI;
import consulo.annotation.component.ServiceImpl;
import consulo.application.Application;
import consulo.application.internal.ApplicationInfo;
import consulo.application.util.concurrent.AppExecutorUtil;
import consulo.disposer.Disposable;
import consulo.externalService.impl.internal.PermanentInstallationID;
import consulo.externalService.impl.internal.WebServiceApi;
import consulo.externalService.impl.internal.WebServiceApiSender;
import consulo.externalService.impl.internal.repository.api.StatisticsBean;
import consulo.localize.LocalizeValue;
import consulo.logging.Logger;
import consulo.project.Project;
import consulo.project.event.ProjectManagerListener;
import consulo.project.ui.notification.Notification;
import consulo.project.ui.notification.NotificationGroup;
import consulo.project.ui.notification.NotificationService;
import consulo.project.ui.notification.event.NotificationListener;
import consulo.ui.UIAccess;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import jakarta.inject.Inject;
import jakarta.inject.Provider;
import jakarta.inject.Singleton;

import java.io.IOException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

@Singleton
@ServiceAPI(value = ComponentScope.APPLICATION, lazy = false)
@ServiceImpl
public class StatisticsSendManager implements Disposable {
    private static final Logger LOG = Logger.getInstance(StatisticsSendManager.class);

    private static final int DELAY_IN_MIN = 10;

    @Nonnull
    private final Provider<UsageStatisticsPersistenceComponent> myUsageStatisticsComponent;
    @Nonnull
    private final Application myApplication;
    @Nonnull
    private final NotificationService myNotificationService;

    private Future<?> myFuture;

    @Inject
    StatisticsSendManager(
        @Nonnull Application application,
        @Nonnull Provider<UsageStatisticsPersistenceComponent> usageStatisticsComponent,
        @Nonnull NotificationService notificationService
    ) {
        myApplication = application;
        myUsageStatisticsComponent = usageStatisticsComponent;
        myNotificationService = notificationService;

        application.getMessageBus().connect().subscribe(ProjectManagerListener.class, new ProjectManagerListener() {
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
        if (!myUsageStatisticsComponent.get().isAllowed()) {
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
        if (oneDayAfterStart > System.currentTimeMillis()) {
            return;
        }

        if (myUsageStatisticsComponent.get().isTimeToSend()) {
            runWithDelay();
        }
    }

    private void runWithDelay() {
        myFuture = AppExecutorUtil.getAppScheduledExecutorService().schedule((Runnable) () -> {
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
    public Notification createNotification(@Nonnull NotificationGroup group, @Nullable NotificationListener listener) {
        LocalizeValue fullProductName = myApplication.getName();
        String companyName = ApplicationInfo.getInstance().getCompanyName();

        return myNotificationService.newInfo(group)
            .title(LocalizeValue.localizeTODO("Help improve " + fullProductName + " by sending anonymous usage statistics to " + companyName))
            .content(LocalizeValue.localizeTODO(
                "<html>Please click <a href='allow'>I agree</a> if you want to help make " + fullProductName +
                    " better or <a href='decline'>I don't agree</a> otherwise. <a href='settings'>more...</a></html>"
            ))
            .optionalHyperlinkListener(listener)
            .create();
    }
}
