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

import consulo.application.Application;
import consulo.disposer.Disposable;
import consulo.localize.LocalizeValue;
import consulo.project.Project;
import consulo.project.ui.internal.NotificationToastService;
import consulo.project.ui.localize.ProjectUILocalize;
import consulo.project.ui.notification.Notification;
import consulo.project.ui.notification.NotificationService;
import consulo.project.ui.notification.event.NotificationServiceListener;
import consulo.ui.Component;
import consulo.ui.HorizontalAlignment;
import consulo.ui.Label;
import consulo.ui.LabelOptions;
import consulo.ui.LabelStyle;
import consulo.ui.Space;
import consulo.ui.Toast;
import consulo.ui.UIAccess;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.layout.DockLayout;
import consulo.ui.layout.PlaceholderLayout;
import consulo.ui.layout.ScrollableLayout;
import consulo.ui.layout.ScrollableLayoutOptions;
import consulo.ui.layout.VerticalLayout;
import consulo.ui.style.ComponentColors;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * The Action Center: the notifications of a project and of the application, newest first - suggestions apart from the
 * timeline. Built of the same cards as the toasts, flat. Rebuilt on every change, in the ui of the project.
 *
 * @author VISTALL
 * @since 2026-10-16
 */
public class NotificationListPanel {
    private final @Nullable Project myProject;
    private final UIAccess myUIAccess;
    private final NotificationService myNotificationService;
    private final NotificationToastService myToastService;

    private final VerticalLayout myList = VerticalLayout.create(Space.MEDIUM);
    private final PlaceholderLayout myRoot = PlaceholderLayout.create();

    private boolean myRebuildQueued;

    @RequiredUIAccess
    public NotificationListPanel(@Nullable Project project, Disposable parentDisposable) {
        myProject = project;
        myUIAccess = UIAccess.current();

        Application application = Application.get();

        myNotificationService = application.getInstance(NotificationService.class);
        myToastService = application.getInstance(NotificationToastService.class);
        Label empty = Label.create(
            ProjectUILocalize.notificationListEmpty(),
            LabelOptions.builder().horizontalAlignment(HorizontalAlignment.CENTER).build()
        );
        empty.setForegroundColor(ComponentColors.INFO_FOREGROUND);
        DockLayout placeholder = DockLayout.create();
        placeholder.center(empty);

        myList.paddingBuilder().allSet(Space.LARGE).apply();
        myRoot.setContent(ScrollableLayout.create(
            myList,
            ScrollableLayoutOptions.builder().horizontalScrollPolicy(ScrollableLayoutOptions.ScrollPolicy.NEVER).build()
        ));
        myRoot.setPlaceholder(placeholder);
        myRoot.setPlaceholderVisibility(() -> myNotificationService.getNotifications(myProject).isEmpty());

        application.getMessageBus().connect(parentDisposable).subscribe(NotificationServiceListener.class, new NotificationServiceListener() {
            @Override
            public void notificationAdded(Notification notification, @Nullable Project project) {
                queueRebuild(project);
            }

            @Override
            public void notificationExpired(Notification notification, @Nullable Project project) {
                queueRebuild(project);
            }

            @Override
            public void notificationRemoved(Notification notification, @Nullable Project project) {
                queueRebuild(project);
            }
        });

        rebuild();
    }

    public Component getComponent() {
        return myRoot;
    }

    private void queueRebuild(@Nullable Project project) {
        if (project != null && project != myProject) {
            return;
        }
        synchronized (this) {
            if (myRebuildQueued) {
                return;
            }
            myRebuildQueued = true;
        }
        myUIAccess.give(() -> {
            synchronized (this) {
                myRebuildQueued = false;
            }
            rebuild();
        });
    }

    @RequiredUIAccess
    private void rebuild() {
        myList.removeAll();

        List<Notification> suggestions = new ArrayList<>();
        List<Notification> timeline = new ArrayList<>();
        for (Notification notification : myNotificationService.getNotifications(myProject).reversed()) {
            (notification.isSuggestion() ? suggestions : timeline).add(notification);
        }

        myRoot.testPlaceholder();

        addSection(ProjectUILocalize.notificationListSuggestions(), suggestions);
        addSection(ProjectUILocalize.notificationListTimeline(), timeline);
    }

    @RequiredUIAccess
    private void addSection(LocalizeValue title, List<Notification> notifications) {
        if (notifications.isEmpty()) {
            return;
        }

        Label header = Label.create(title);
        header.addStyle(LabelStyle.FONT_BOLD);
        myList.add(header);

        for (Notification notification : notifications) {
            Toast toast = myToastService.createToast(notification, myProject, () -> myNotificationService.remove(notification));
            if (notification.isExpired()) {
                toast.setEnabled(false);
            }
            myList.add(toast);
        }
    }
}
