/*
 * Copyright 2013-2022 consulo.io
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
package consulo.desktop.awt.internal.notification;

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ServiceAPI;
import consulo.annotation.component.ServiceImpl;
import consulo.disposer.Disposable;
import consulo.ide.impl.idea.notification.impl.NotificationsConfigurationImpl;
import consulo.project.Project;
import consulo.project.startup.StartupManager;
import consulo.project.ui.notification.Notification;
import consulo.project.ui.wm.StatusBar;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.toolWindow.ToolWindow;
import consulo.util.collection.Lists;
import consulo.util.lang.ObjectUtil;
import consulo.util.lang.ShutDownTracker;
import jakarta.annotation.Nonnull;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;

import java.util.Collection;
import java.util.List;

/**
 * @author VISTALL
 * @since 2022-06-18
 */
@Singleton
@ServiceAPI(ComponentScope.PROJECT)
@ServiceImpl
public class NotificationProjectTracker implements Disposable {
    static NotificationProjectTracker getInstance(Project project) {
        return project.getInstance(NotificationProjectTracker.class);
    }

    private final EventLogConsole myEventLogConsole;
    private final List<Notification> myInitial = Lists.newLockFreeCopyOnWriteList();
    private final Project myProject;
    private final EventLog myEventLog;

    protected LogModel myProjectModel;

    @Inject
    public NotificationProjectTracker(@Nonnull Project project, EventLog eventLog) {
        myProject = project;
        myEventLog = eventLog;
        myProjectModel = new LogModel(project.getApplication(), project);
        myEventLogConsole = new EventLogConsole(myProjectModel);
    }

    public EventLogConsole getEventLogConsole() {
        return myEventLogConsole;
    }

    public void printToProjectEventLog() {
        for (Notification notification : myEventLog.myModel.takeNotifications()) {
            printNotification(notification);
        }
    }

    void initDefaultContent() {
        for (Notification notification : myInitial) {
            doPrintNotification(notification, ObjectUtil.assertNotNull(myEventLogConsole));
        }
        myInitial.clear();
    }

    @Override
    public void dispose() {
        myEventLog.myModel.setStatusMessage(null, 0);
        StatusBar.Info.set("", null, EventLog.LOG_REQUESTOR);
    }

    protected void printNotification(Notification notification) {
        if (!NotificationsConfigurationImpl.getSettings(notification.getGroupId()).isShouldLog()) {
            return;
        }
        myProjectModel.addNotification(notification);

        EventLogConsole console = myEventLogConsole;
        doPrintNotification(notification, console);
    }

    private void doPrintNotification(@Nonnull Notification notification, @Nonnull EventLogConsole console) {
        StartupManager.getInstance(myProject).runWhenProjectIsInitialized(() -> {
            if (!ShutDownTracker.isShutdownHookRunning() && !myProject.isDisposed()) {
                myProject.getApplication().runReadAction(() -> console.doPrintNotification(notification));
            }
        });
    }

    @RequiredUIAccess
    protected void showNotification(@Nonnull String groupId, @Nonnull List<String> ids) {
        ToolWindow eventLog = EventLog.getEventLog(myProject);
        if (eventLog != null) {
            EventLog.activate(eventLog, groupId, () -> myEventLogConsole.showNotification(ids));
        }
    }

    protected void clearNMore(@Nonnull Collection<String> groups) {
        myEventLogConsole.clearNMore();
    }
}
