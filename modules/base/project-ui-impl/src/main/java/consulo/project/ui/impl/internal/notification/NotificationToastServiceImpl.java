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
import consulo.configurable.internal.ShowConfigurableService;
import consulo.dataContext.DataContext;
import consulo.dataContext.DataManager;
import consulo.localize.LocalizeValue;
import consulo.platform.base.icon.PlatformIconGroup;
import consulo.platform.base.localize.CommonLocalize;
import consulo.project.Project;
import consulo.project.ui.internal.IdeFrameEx;
import consulo.project.ui.internal.NotificationToastService;
import consulo.project.ui.internal.NotificationsConfiguration;
import consulo.project.ui.localize.ProjectUILocalize;
import consulo.project.ui.notification.Notification;
import consulo.project.ui.notification.NotificationDisplayType;
import consulo.project.ui.notification.NotificationService;
import consulo.project.ui.notification.NotificationType;
import consulo.project.ui.notification.event.NotificationListener;
import consulo.ui.ModalityState;
import consulo.ui.Toast;
import consulo.ui.ToastStyle;
import consulo.ui.UIAccess;
import consulo.ui.Window;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.action.ActionGroup;
import consulo.ui.ex.action.ActionPlaces;
import consulo.ui.ex.action.ActionToolbar;
import consulo.ui.ex.action.ActionToolbarFactory;
import consulo.ui.ex.action.AnAction;
import consulo.ui.ex.action.AnActionEvent;
import consulo.ui.ex.action.DumbAwareAction;
import consulo.ui.ex.action.MoreActionGroup;
import consulo.ui.internal.ToastOverlay;
import consulo.util.dataholder.Key;
import jakarta.inject.Inject;
import jakarta.inject.Provider;
import jakarta.inject.Singleton;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * The card of a notification - style by type, title, HTML content, actions as links, and a toolbar with the options
 * ("⋮": settings, do not show again) and close - and the toasts floating over frames, which are glassy. At most {@link #MAX_VISIBLE}
 * toasts stay over a frame; a balloon goes after {@link #BALLOON_DELAY_SECONDS}, a sticky one after
 * {@link #STICKY_DELAY_SECONDS}. Hiding a toast does not expire the notification - it stays in the Notifications tool
 * window - unless the notification is not kept there.
 *
 * @author VISTALL
 * @since 2026-10-16
 */
@Singleton
@ServiceImpl
public class NotificationToastServiceImpl implements NotificationToastService {
    private static final int MAX_VISIBLE = 3;
    private static final int BALLOON_DELAY_SECONDS = 10;
    private static final int STICKY_DELAY_SECONDS = 300;

    private static final Key<Map<Notification, Toast>> SHOWN = Key.create("NotificationToastService.shown");

    private final NotificationsConfiguration myConfiguration;
    private final Provider<NotificationService> myNotificationService;
    private final DataManager myDataManager;
    private final ShowConfigurableService myShowConfigurableService;
    private final ActionToolbarFactory myActionToolbarFactory;

    private final Map<Notification, List<IdeFrameEx>> myFrames = new IdentityHashMap<>();
    private final List<Notification> myLater = new ArrayList<>();

    @Inject
    public NotificationToastServiceImpl(
        NotificationsConfiguration configuration,
        Provider<NotificationService> notificationService,
        DataManager dataManager,
        ShowConfigurableService showConfigurableService,
        ActionToolbarFactory actionToolbarFactory
    ) {
        myConfiguration = configuration;
        myNotificationService = notificationService;
        myDataManager = dataManager;
        myShowConfigurableService = showConfigurableService;
        myActionToolbarFactory = actionToolbarFactory;
    }

    @RequiredUIAccess
    @Override
    public Toast createToast(Notification notification, @Nullable Project project, @Nullable Runnable close) {
        Toast toast = Toast.create();
        toast.addStyle(styleOf(notification.getType()));
        if (notification.getIcon() != null) {
            toast.setIcon(notification.getIcon());
        }

        LocalizeValue title = notification.getTitleValue();
        LocalizeValue subtitle = notification.getSubtitleValue();
        if (title.isNotEmpty() && subtitle.isNotEmpty()) {
            toast.setTitle(LocalizeValue.join(title, LocalizeValue.of(" "), subtitle));
        }
        else {
            toast.setTitle(title.isNotEmpty() ? title : subtitle);
        }

        if (notification.hasContent()) {
            toast.setHtmlText(notification.getContentValue());
            NotificationListener listener = notification.getListener();
            if (listener != null) {
                toast.addHyperlinkListener(event -> listener.hyperlinkActivated(notification, event));
            }
        }

        if (!notification.isExpired()) {
            for (AnAction action : notification.getActions()) {
                toast.addAction(action.getTemplatePresentation().getTextValue(), () -> {
                    if (!notification.isExpired()) {
                        Notification.fire(notification, action, contextOf(toast, project));
                    }
                });
            }
        }

        ActionGroup.Builder group = ActionGroup.newImmutableBuilder();
        MoreActionGroup options = new MoreActionGroup(false);
        options.setPopup(true);
        options.add(new DumbAwareAction(ProjectUILocalize.actionNotificationSettingsText()) {
            @RequiredUIAccess
            @Override
            public void actionPerformed(AnActionEvent e) {
                myShowConfigurableService.showAndSelect(project, NotificationsConfigurable.class);
            }
        });
        options.add(new DumbAwareAction(CommonLocalize.dialogOptionsDoNotShowShort()) {
            @RequiredUIAccess
            @Override
            public void actionPerformed(AnActionEvent e) {
                myConfiguration.changeDisplayType(notification.getGroupId(), NotificationDisplayType.NONE);
                notification.expire();
            }
        });
        group.add(options);

        if (close != null) {
            group.add(new DumbAwareAction(CommonLocalize.actionClose(), LocalizeValue.empty(), PlatformIconGroup.actionsClose()) {
                @RequiredUIAccess
                @Override
                public void actionPerformed(AnActionEvent e) {
                    close.run();
                }
            });
        }

        ActionToolbar toolbar = myActionToolbarFactory.createActionToolbar(ActionPlaces.NOTIFICATION, group.build(), ActionToolbar.Style.INPLACE);
        toolbar.setTargetUIComponent(toast);
        toolbar.updateActionsAsync();
        toast.setToolbarComponent(toolbar.getUIComponent());
        return toast;
    }

    @RequiredUIAccess
    @Override
    public void show(IdeFrameEx frame, Notification notification) {
        Window window = frame.getWindow();
        if (!(window instanceof ToastOverlay overlay) || notification.isExpired()) {
            return;
        }

        Map<Notification, Toast> shown = window.getUserData(SHOWN);
        if (shown == null) {
            shown = new LinkedHashMap<>();
            window.putUserData(SHOWN, shown);
        }
        if (shown.containsKey(notification)) {
            return;
        }

        Toast toast = createToast(notification, frame.getProject(), () -> close(frame, notification));
        toast.addStyle(ToastStyle.GLASSY);
        shown.put(notification, toast);
        synchronized (myFrames) {
            myFrames.computeIfAbsent(notification, it -> new ArrayList<>()).add(frame);
        }
        overlay.showToast(toast);

        while (shown.size() > MAX_VISIBLE) {
            hide(frame, shown.keySet().iterator().next());
        }

        NotificationDisplayType displayType = myConfiguration.getDisplayType(notification.getGroupId());
        int delay = displayType == NotificationDisplayType.STICKY_BALLOON ? STICKY_DELAY_SECONDS : BALLOON_DELAY_SECONDS;
        UIAccess.current().getScheduler().schedule(() -> close(frame, notification), ModalityState.any(), delay, TimeUnit.SECONDS);
    }

    @RequiredUIAccess
    @Override
    public void hide(IdeFrameEx frame, Notification notification) {
        Window window = frame.getWindow();
        Map<Notification, Toast> shown = window == null ? null : window.getUserData(SHOWN);
        Toast toast = shown == null ? null : shown.remove(notification);
        synchronized (myFrames) {
            List<IdeFrameEx> frames = myFrames.get(notification);
            if (frames != null) {
                frames.remove(frame);
                if (frames.isEmpty()) {
                    myFrames.remove(notification);
                }
            }
        }
        if (toast != null && window instanceof ToastOverlay overlay) {
            overlay.hideToast(toast);
        }
    }

    @RequiredUIAccess
    @Override
    public void hideAll(IdeFrameEx frame) {
        Window window = frame.getWindow();
        Map<Notification, Toast> shown = window == null ? null : window.getUserData(SHOWN);
        if (shown != null) {
            for (Notification notification : new ArrayList<>(shown.keySet())) {
                close(frame, notification);
            }
        }
    }

    @Override
    public void hideEverywhere(Notification notification) {
        List<IdeFrameEx> frames;
        synchronized (myFrames) {
            List<IdeFrameEx> shownIn = myFrames.get(notification);
            frames = shownIn == null ? List.of() : new ArrayList<>(shownIn);
        }
        for (IdeFrameEx frame : frames) {
            Window window = frame.getWindow();
            UIAccess uiAccess = window == null ? null : window.getUIAccess();
            if (uiAccess != null && uiAccess.isValid()) {
                uiAccess.give(() -> frame.hideNotification(notification));
            }
        }
    }

    @Override
    public void showLater(Notification notification) {
        synchronized (myLater) {
            if (!myLater.contains(notification)) {
                myLater.add(notification);
            }
        }
    }

    @RequiredUIAccess
    @Override
    public void showLaterOver(IdeFrameEx frame) {
        List<Notification> later;
        synchronized (myLater) {
            later = new ArrayList<>(myLater);
            myLater.clear();
        }
        for (Notification notification : later) {
            if (!notification.isExpired()) {
                frame.showNotification(notification);
            }
        }
    }

    @RequiredUIAccess
    private void close(IdeFrameEx frame, Notification notification) {
        frame.hideNotification(notification);

        if (!myNotificationService.get().getNotifications(frame.getProject()).contains(notification)) {
            notification.expire();
        }
    }

    private DataContext contextOf(Toast toast, @Nullable Project project) {
        DataContext context = myDataManager.getDataContext(toast);
        return new DataContext() {
            @Override
            @SuppressWarnings("unchecked")
            public <T> @Nullable T getData(Key<T> key) {
                T data = context.getData(key);
                if (data == null && key == Project.KEY) {
                    return (T) project;
                }
                return data;
            }
        };
    }

    private static ToastStyle styleOf(NotificationType type) {
        return switch (type) {
            case INFORMATION -> ToastStyle.INFO;
            case SUCCESS -> ToastStyle.SUCCESS;
            case WARNING -> ToastStyle.WARNING;
            case ERROR -> ToastStyle.ERROR;
        };
    }
}
