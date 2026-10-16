/*
 * Copyright 2013-2026 consulo.io
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
package consulo.project.ui.impl.internal.notification;

import consulo.annotation.component.ServiceImpl;
import consulo.application.Application;
import consulo.dataContext.DataContext;
import consulo.disposer.Disposer;
import consulo.logging.Logger;
import consulo.project.Project;
import consulo.project.ui.internal.NotificationsConfiguration;
import consulo.project.ui.notification.Notification;
import consulo.project.ui.notification.NotificationDisplayType;
import consulo.project.ui.notification.NotificationService;
import consulo.project.ui.notification.event.NotificationServiceListener;
import consulo.ui.UIAccess;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.action.ActionPlaces;
import consulo.ui.ex.action.AnAction;
import consulo.ui.ex.action.AnActionEvent;
import consulo.ui.ex.impl.internal.action.ActionImplUtil;
import consulo.ui.ex.impl.internal.action.ActionRunnerAsync;
import consulo.util.dataholder.Key;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

/**
 * The only notification service, the same on every frontend. Notifications are kept until they are removed; an
 * expired one stays, greyed out, unless it asked to be removed. The store is guarded by one lock and every listener
 * is told on the calling thread, outside the lock.
 *
 * @author VISTALL
 * @since 2026-10-16
 */
@Singleton
@ServiceImpl
public class NotificationServiceImpl implements NotificationService {
    private static final Logger LOG = Logger.getInstance(NotificationServiceImpl.class);

    private static class Entries {
        final List<Notification> myNotifications = new ArrayList<>();
        final List<Notification> myUnread = new ArrayList<>();

        void clear() {
            myNotifications.clear();
            myUnread.clear();
        }
    }

    private final Application myApplication;
    private final NotificationsConfiguration myConfiguration;

    private final Object myLock = new Object();
    private final Entries myApplicationEntries = new Entries();
    private final Map<Project, Entries> myProjectEntries = new HashMap<>();
    private final Map<Notification, Project> myOwners = new IdentityHashMap<>();

    @Inject
    public NotificationServiceImpl(Application application, NotificationsConfiguration configuration) {
        myApplication = application;
        myConfiguration = configuration;
    }

    @Override
    public void notify(Notification notification, @Nullable Project project) {
        Project owner = project == null || project.isDefault() || project.isDisposed() ? null : project;

        String groupId = notification.getGroupId();
        if (!myConfiguration.isRegistered(groupId)) {
            LOG.error("Notification group '" + groupId + "' is not registered. Register it by NotificationGroupContributor");
        }

        NotificationDisplayType displayType = myConfiguration.getDisplayType(groupId);
        boolean shouldLog = myConfiguration.isShouldLog(groupId);
        boolean shown = displayType != NotificationDisplayType.NONE && myConfiguration.isShowBalloons();
        if (!shouldLog && !shown) {
            notification.expire();
            return;
        }

        if (shouldLog) {
            synchronized (myLock) {
                Entries entries = owner == null ? myApplicationEntries : entriesOf(owner);
                entries.myNotifications.add(notification);
                if (notification.isImportant() || displayType != NotificationDisplayType.NONE && displayType != NotificationDisplayType.TOOL_WINDOW) {
                    entries.myUnread.add(notification);
                }
                if (owner != null) {
                    myOwners.put(notification, owner);
                }
            }
        }

        NotificationServiceListener publisher = publisher();
        publisher.notificationAdded(notification, owner);
        if (shouldLog) {
            publisher.stateChanged(owner);
        }
    }

    private Entries entriesOf(Project project) {
        Entries entries = myProjectEntries.get(project);
        if (entries == null) {
            entries = new Entries();
            myProjectEntries.put(project, entries);
            Disposer.register(project, () -> projectDisposed(project));
        }
        return entries;
    }

    private void projectDisposed(Project project) {
        List<Notification> notifications;
        synchronized (myLock) {
            Entries entries = myProjectEntries.remove(project);
            notifications = entries == null ? List.of() : new ArrayList<>(entries.myNotifications);
            notifications.forEach(myOwners::remove);
        }
        notifications.forEach(Notification::expire);
    }

    @Override
    public List<Notification> getNotifications(@Nullable Project project) {
        synchronized (myLock) {
            List<Notification> result = new ArrayList<>(myApplicationEntries.myNotifications);
            Entries entries = project == null ? null : myProjectEntries.get(project);
            if (entries != null) {
                result.addAll(entries.myNotifications);
                result.sort((left, right) -> Long.compare(left.getTimestamp(), right.getTimestamp()));
            }
            return result;
        }
    }

    @Override
    public List<Notification> getUnreadNotifications(@Nullable Project project) {
        synchronized (myLock) {
            List<Notification> result = new ArrayList<>(myApplicationEntries.myUnread);
            Entries entries = project == null ? null : myProjectEntries.get(project);
            if (entries != null) {
                result.addAll(entries.myUnread);
            }
            return result;
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T extends Notification> List<T> getNotificationsOfType(Class<T> type, @Nullable Project project) {
        List<T> result = new ArrayList<>();
        for (Notification notification : getNotifications(project)) {
            if (type.isInstance(notification) && !notification.isExpired()) {
                result.add((T) notification);
            }
        }
        return result;
    }

    @Override
    public void markAllRead(@Nullable Project project) {
        synchronized (myLock) {
            myApplicationEntries.myUnread.clear();
            Entries entries = project == null ? null : myProjectEntries.get(project);
            if (entries != null) {
                entries.myUnread.clear();
            }
        }
        publisher().stateChanged(project);
    }

    @Override
    public void clearAll(@Nullable Project project) {
        removeAll(project, notification -> true);
    }

    @Override
    public void clearTimeline(@Nullable Project project) {
        removeAll(project, notification -> !notification.isSuggestion());
    }

    private void removeAll(@Nullable Project project, Predicate<Notification> filter) {
        for (Notification notification : getNotifications(project)) {
            if (filter.test(notification)) {
                notification.expire();
                remove(notification);
            }
        }
    }

    @Override
    public void remove(Notification notification) {
        Project owner;
        boolean removed;
        synchronized (myLock) {
            owner = myOwners.remove(notification);
            Entries entries = owner == null ? myApplicationEntries : myProjectEntries.get(owner);
            removed = entries != null && entries.myNotifications.remove(notification);
            if (entries != null) {
                entries.myUnread.remove(notification);
            }
        }

        if (removed) {
            NotificationServiceListener publisher = publisher();
            publisher.notificationRemoved(notification, owner);
            publisher.stateChanged(owner);
        }
    }

    @Override
    public void expire(Notification notification) {
        Project owner;
        synchronized (myLock) {
            owner = myOwners.get(notification);
            Entries entries = owner == null ? myApplicationEntries : myProjectEntries.get(owner);
            if (entries != null) {
                entries.myUnread.remove(notification);
            }
        }

        NotificationServiceListener publisher = publisher();
        publisher.notificationExpired(notification, owner);
        publisher.stateChanged(owner);

        if (notification.isRemoveWhenExpired()) {
            remove(notification);
        }
    }

    @RequiredUIAccess
    @Override
    public void fire(Notification notification, AnAction action, @Nullable DataContext context) {
        AnActionEvent event = AnActionEvent.createFromAnAction(action, null, ActionPlaces.NOTIFICATION, new DataContext() {
            @Override
            @SuppressWarnings("unchecked")
            public <T> @Nullable T getData(Key<T> dataId) {
                if (Notification.KEY == dataId) {
                    return (T) notification;
                }
                return context == null ? null : context.getData(dataId);
            }
        });

        UIAccess uiAccess = UIAccess.current();
        ActionRunnerAsync.lastUpdateAndCheckDumbAsync(action, event, false).whenCompleteAsync((enabled, throwable) -> {
            if (Boolean.TRUE.equals(enabled)) {
                ActionImplUtil.performActionDumbAware(action, event);
            }
        }, uiAccess);
    }

    private NotificationServiceListener publisher() {
        return myApplication.getMessageBus().syncPublisher(NotificationServiceListener.class);
    }
}
