/*
 * Copyright 2000-2011 JetBrains s.r.o.
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

import consulo.application.Application;
import consulo.disposer.Disposable;
import consulo.disposer.Disposer;
import consulo.ide.impl.idea.notification.impl.NotificationsConfigurationImpl;
import consulo.project.Project;
import consulo.project.ui.notification.Notification;
import consulo.project.ui.notification.NotificationDisplayType;
import consulo.ui.ex.awt.UIUtil;
import consulo.util.collection.ContainerUtil;
import consulo.util.lang.Trinity;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.util.*;

/**
 * @author peter
 * @see LogModelListener
 */
public class LogModel implements Disposable {
    private final List<Notification> myNotifications = new ArrayList<>();
    private final Map<Notification, Long> myStamps = Collections.synchronizedMap(new WeakHashMap<Notification, Long>());
    @Nonnull
    private final Application myApplication;
    @Nullable
    private final Project myProject;
    final Map<Notification, Runnable> removeHandlers = new HashMap<>();

    LogModel(@Nonnull Application application, @Nullable Project project) {
        myApplication = application;
        myProject = project;
        Disposer.register(project != null ? project : application, this);
    }

    void addNotification(Notification notification) {
        long stamp = System.currentTimeMillis();
        NotificationDisplayType type = NotificationsConfigurationImpl.getSettings(notification.getGroupId()).getDisplayType();
        if (notification.isImportant() || (type != NotificationDisplayType.NONE && type != NotificationDisplayType.TOOL_WINDOW)) {
            synchronized (myNotifications) {
                myNotifications.add(notification);
            }
        }
        myStamps.put(notification, stamp);
        fireModelChanged();
    }

    private void fireModelChanged() {
        myApplication.getMessageBus().syncPublisher(LogModelListener.class).modelChanged(myProject);
    }

    List<Notification> takeNotifications() {
        ArrayList<Notification> result;
        synchronized (myNotifications) {
            result = getNotifications();
            myNotifications.clear();
        }
        fireModelChanged();
        return result;
    }

    void logShown() {
        for (Notification notification : getNotifications()) {
            if (!notification.isImportant()) {
                removeNotification(notification);
            }
        }
    }

    public ArrayList<Notification> getNotifications() {
        synchronized (myNotifications) {
            return new ArrayList<>(myNotifications);
        }
    }

    @Nullable
    public Long getNotificationTime(Notification notification) {
        return myStamps.get(notification);
    }

    void removeNotification(Notification notification) {
        synchronized (myNotifications) {
            myNotifications.remove(notification);
        }

        Runnable handler = removeHandlers.remove(notification);
        if (handler != null) {
            UIUtil.invokeLaterIfNeeded(handler);
        }
        fireModelChanged();
    }

    @Nullable
    public Project getProject() {
        return myProject;
    }

    @Override
    public void dispose() {
    }
}
