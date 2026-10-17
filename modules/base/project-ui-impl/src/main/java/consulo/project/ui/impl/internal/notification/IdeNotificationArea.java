/*
 * Copyright 2000-2014 JetBrains s.r.o.
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
import consulo.application.ui.UISettings;
import consulo.application.ui.event.UISettingsListener;
import consulo.component.messagebus.MessageBusConnection;
import consulo.localize.LocalizeValue;
import consulo.project.Project;
import consulo.project.ui.internal.NotificationIconBuilder;
import consulo.project.ui.notification.Notification;
import consulo.project.ui.notification.NotificationService;
import consulo.project.ui.notification.event.NotificationServiceListener;
import consulo.project.ui.wm.StatusBar;
import consulo.project.ui.wm.StatusBarWidget;
import consulo.project.ui.wm.StatusBarWidgetFactory;
import consulo.project.ui.wm.ToolWindowManager;
import consulo.ui.Component;
import consulo.ui.UIAccess;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.event.ClickEvent;
import consulo.ui.event.ComponentEventListener;
import consulo.ui.ex.toolWindow.ToolWindow;
import consulo.ui.ex.toolWindow.ToolWindowSettings;
import consulo.ui.image.Image;
import org.jspecify.annotations.Nullable;

import java.util.List;

/**
 * @author spleaner
 */
public class IdeNotificationArea implements StatusBarWidget, StatusBarWidget.IconPresentation {
    private final StatusBarWidgetFactory myFactory;
    private @Nullable StatusBar myStatusBar;

    private @Nullable Image myIcon;
    private LocalizeValue myTooltip = LocalizeValue.empty();

    public IdeNotificationArea(StatusBarWidgetFactory factory) {
        myFactory = factory;

        Application application = Application.get();
        MessageBusConnection connection = application.getMessageBus().connect(this);
        connection.subscribe(UISettingsListener.class, source -> updateStatus());
        connection.subscribe(NotificationServiceListener.class, new NotificationServiceListener() {
            @Override
            public void stateChanged(@Nullable Project project) {
                Project own = getProject();
                if (own == null || project != null && project != own) {
                    return;
                }

                UIAccess uiAccess = own.getUIAccess();
                if (uiAccess.isValid()) {
                    uiAccess.give(IdeNotificationArea.this::updateStatus);
                }
            }
        });
    }

    @Override
    public String getId() {
        return myFactory.getId();
    }

    @Override
    public WidgetPresentation getPresentation() {
        return this;
    }

    @Override
    public void dispose() {
    }

    @Override
    @RequiredUIAccess
    public void install(StatusBar statusBar) {
        myStatusBar = statusBar;
        updateStatus();
    }

    private @Nullable Project getProject() {
        StatusBar statusBar = myStatusBar;
        return statusBar == null ? null : statusBar.getProject();
    }

    @RequiredUIAccess
    private void updateStatus() {
        StatusBar statusBar = myStatusBar;
        Project project = getProject();
        if (statusBar == null || project == null) {
            return;
        }

        List<Notification> notifications = Application.get().getInstance(NotificationService.class).getUnreadNotifications(project);

        boolean shown = ToolWindowSettings.getInstance(project).isHideToolStripes() || UISettings.getInstance().PRESENTATION_MODE;
        myIcon = shown ? NotificationIconBuilder.getIcon(notifications.stream().map(Notification::getType).toList()) : null;

        int count = notifications.size();
        myTooltip = LocalizeValue.localizeTODO(
            count > 0 ? String.format("%s notification%s pending", count, count == 1 ? "" : "s") : "No new notifications"
        );

        statusBar.updateWidget(getId());
    }

    @Override
    public @Nullable Image getIcon() {
        return myIcon;
    }

    @Override
    public LocalizeValue getTooltipText() {
        return myTooltip;
    }

    @Override
    public ComponentEventListener<Component, ClickEvent> getClickListener() {
        return event -> toggleActionCenter();
    }

    @RequiredUIAccess
    private void toggleActionCenter() {
        Project project = getProject();
        if (project == null) {
            return;
        }

        ToolWindow toolWindow = ToolWindowManager.getInstance(project).getToolWindow(NotificationsToolWindowFactory.ID);
        if (toolWindow == null) {
            return;
        }
        if (toolWindow.isVisible()) {
            toolWindow.hide();
        }
        else {
            toolWindow.activate(null);
        }
    }
}
