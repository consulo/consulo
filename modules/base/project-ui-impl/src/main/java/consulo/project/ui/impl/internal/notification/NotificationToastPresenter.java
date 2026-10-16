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

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.TopicImpl;
import consulo.project.Project;
import consulo.project.startup.StartupManager;
import consulo.project.ui.internal.IdeFrameEx;
import consulo.project.ui.internal.NotificationToastService;
import consulo.project.ui.internal.NotificationsConfiguration;
import consulo.project.ui.notification.Notification;
import consulo.project.ui.notification.NotificationDisplayType;
import consulo.project.ui.notification.event.NotificationServiceListener;
import consulo.project.ui.wm.IdeFrame;
import consulo.project.ui.wm.ToolWindowManager;
import consulo.project.ui.wm.WelcomeFrameManager;
import consulo.project.ui.wm.WindowManager;
import consulo.ui.UIAccess;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.util.lang.xml.XmlStringUtil;
import jakarta.inject.Inject;
import jakarta.inject.Provider;
import org.jspecify.annotations.Nullable;

import java.util.List;

/**
 * Floats every new notification over the frames which show it, each in its own ui: the frame of its project, or -
 * for a notification of the application - the welcome screen of every ui, else the project frame of that ui.
 * A group shown by a tool window points at that tool window instead.
 *
 * @author VISTALL
 * @since 2026-10-16
 */
@TopicImpl(ComponentScope.APPLICATION)
public class NotificationToastPresenter implements NotificationServiceListener {
    private static final String ACTION_HREF_PREFIX = "notification-action:";

    private final NotificationsConfiguration myConfiguration;
    private final Provider<NotificationToastService> myToastService;

    @Inject
    public NotificationToastPresenter(NotificationsConfiguration configuration, Provider<NotificationToastService> toastService) {
        myConfiguration = configuration;
        myToastService = toastService;
    }

    @Override
    public void notificationAdded(Notification notification, @Nullable Project project) {
        if (!myConfiguration.isShowBalloons()) {
            return;
        }

        String groupId = notification.getGroupId();
        NotificationDisplayType displayType = myConfiguration.getDisplayType(groupId);
        if (displayType == NotificationDisplayType.NONE) {
            return;
        }

        if (project == null) {
            List<UIAccess> uiAccesses = UIAccess.listAll();
            if (uiAccesses.isEmpty()) {
                myToastService.get().showLater(notification);
            }
            for (UIAccess uiAccess : uiAccesses) {
                uiAccess.give(() -> showInUI(uiAccess, notification));
            }
            return;
        }

        String toolWindowId = displayType == NotificationDisplayType.TOOL_WINDOW ? myConfiguration.getToolWindowId(groupId) : null;
        StartupManager.getInstance(project).runWhenProjectIsInitialized(() -> project.getUIAccess().give(() -> {
            if (project.isDisposed() || notification.isExpired()) {
                return;
            }
            if (toolWindowId != null && showInToolWindow(project, toolWindowId, notification)) {
                return;
            }
            if (WindowManager.getInstance().getIdeFrame(project) instanceof IdeFrameEx frame) {
                frame.showNotification(notification);
            }
        }));
    }

    @Override
    public void notificationExpired(Notification notification, @Nullable Project project) {
        myToastService.get().hideEverywhere(notification);
    }

    @Override
    public void notificationRemoved(Notification notification, @Nullable Project project) {
        myToastService.get().hideEverywhere(notification);
    }

    @RequiredUIAccess
    private void showInUI(UIAccess uiAccess, Notification notification) {
        if (notification.isExpired()) {
            return;
        }
        if (frameOf(uiAccess) instanceof IdeFrameEx frame) {
            frame.showNotification(notification);
        }
        else {
            myToastService.get().showLater(notification);
        }
    }

    @RequiredUIAccess
    private static @Nullable IdeFrame frameOf(UIAccess uiAccess) {
        IdeFrame welcomeFrame = WelcomeFrameManager.getInstance().getCurrentFrame();
        if (welcomeFrame != null) {
            return welcomeFrame;
        }

        WindowManager windowManager = WindowManager.getInstance();
        IdeFrame visibleFrame = windowManager.findVisibleIdeFrame();
        if (visibleFrame != null && belongsTo(visibleFrame, uiAccess)) {
            return visibleFrame;
        }
        for (IdeFrame frame : windowManager.getAllProjectFrames()) {
            if (belongsTo(frame, uiAccess)) {
                return frame;
            }
        }
        return null;
    }

    private static boolean belongsTo(IdeFrame frame, UIAccess uiAccess) {
        Project project = frame.getProject();
        return project != null && project.getUIAccess() == uiAccess;
    }

    @RequiredUIAccess
    private static boolean showInToolWindow(Project project, String toolWindowId, Notification notification) {
        ToolWindowManager toolWindowManager = ToolWindowManager.getInstance(project);
        if (!toolWindowManager.canShowNotification(toolWindowId)) {
            return false;
        }

        StringBuilder html = new StringBuilder();
        if (notification.hasTitle()) {
            html.append("<b>").append(XmlStringUtil.escapeText(notification.getTitle())).append("</b>");
        }
        if (notification.hasContent()) {
            if (!html.isEmpty()) {
                html.append("<br>");
            }
            html.append(notification.getContent());
        }
        List<consulo.ui.ex.action.AnAction> actions = notification.getActions();
        for (int i = 0; i < actions.size(); i++) {
            html.append(i == 0 ? "<br>" : "&nbsp;&nbsp;&nbsp;")
                .append("<a href=\"").append(ACTION_HREF_PREFIX).append(i).append("\">")
                .append(XmlStringUtil.escapeText(actions.get(i).getTemplatePresentation().getText()))
                .append("</a>");
        }

        toolWindowManager.notifyByBalloon(toolWindowId, notification.getType().toUI(), html.toString(), notification.getIcon(), event -> {
            String href = event.getDescription();
            if (href.startsWith(ACTION_HREF_PREFIX)) {
                int index = Integer.parseInt(href.substring(ACTION_HREF_PREFIX.length()));
                if (index < actions.size()) {
                    Notification.fire(notification, actions.get(index), null);
                }
            }
            else if (notification.getListener() != null) {
                notification.getListener().hyperlinkActivated(notification, event);
            }
        });
        return true;
    }
}
