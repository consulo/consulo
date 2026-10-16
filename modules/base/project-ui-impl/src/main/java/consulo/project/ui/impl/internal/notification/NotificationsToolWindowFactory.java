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

import consulo.annotation.component.ExtensionImpl;
import consulo.application.dumb.DumbAware;
import consulo.configurable.internal.ShowConfigurableService;
import consulo.disposer.Disposable;
import consulo.localize.LocalizeValue;
import consulo.platform.base.icon.PlatformIconGroup;
import consulo.project.Project;
import consulo.project.ui.internal.IdeFrameEx;
import consulo.project.ui.internal.NotificationIconBuilder;
import consulo.project.ui.internal.NotificationToastService;
import consulo.project.ui.localize.ProjectUILocalize;
import consulo.project.ui.notification.Notification;
import consulo.project.ui.notification.NotificationService;
import consulo.project.ui.wm.ToolWindowFactory;
import consulo.project.ui.wm.ToolWindowManagerListener;
import consulo.project.ui.wm.WindowManager;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.action.ActionGroup;
import consulo.ui.ex.action.AnActionEvent;
import consulo.ui.ex.action.DumbAwareAction;
import consulo.ui.ex.content.Content;
import consulo.ui.ex.content.ContentFactory;
import consulo.ui.ex.toolWindow.ToolWindow;
import consulo.ui.ex.toolWindow.ToolWindowAnchor;
import consulo.ui.image.Image;
import jakarta.inject.Inject;
import jakarta.inject.Provider;

/**
 * The Action Center, the same on every frontend.
 *
 * @author VISTALL
 * @since 2026-10-16
 */
@ExtensionImpl
public class NotificationsToolWindowFactory implements ToolWindowFactory, DumbAware {
    public static final String ID = "Notifications";

    private final NotificationService myNotificationService;
    private final NotificationToastService myToastService;
    private final ShowConfigurableService myShowConfigurableService;
    private final Provider<WindowManager> myWindowManager;

    @Inject
    public NotificationsToolWindowFactory(
        NotificationService notificationService,
        NotificationToastService toastService,
        ShowConfigurableService showConfigurableService,
        Provider<WindowManager> windowManager
    ) {
        myNotificationService = notificationService;
        myToastService = toastService;
        myShowConfigurableService = showConfigurableService;
        myWindowManager = windowManager;
    }

    @Override
    public String getId() {
        return ID;
    }

    @RequiredUIAccess
    @Override
    public void createToolWindowContent(Project project, ToolWindow toolWindow) {
        Disposable disposable = Disposable.newDisposable("NotificationsToolWindow");
        NotificationListPanel panel = new NotificationListPanel(project, disposable);
        Content content = ContentFactory.getInstance().createUIContent(panel.getComponent(), "", false);
        content.setDisposer(disposable);
        toolWindow.getContentManager().addContent(content);

        ActionGroup.Builder gearActions = ActionGroup.newImmutableBuilder();
        gearActions.add(
            new DumbAwareAction(ProjectUILocalize.actionNotificationClearText(), ProjectUILocalize.actionNotificationClearDescription(), PlatformIconGroup.actionsGc()) {
                @RequiredUIAccess
                @Override
                public void actionPerformed(AnActionEvent e) {
                    myNotificationService.clearAll(project);
                }
            }
        );
        gearActions.add(
            new DumbAwareAction(ProjectUILocalize.actionNotificationMarkAsReadText(), ProjectUILocalize.actionNotificationMarkAsReadDescription(), PlatformIconGroup.generalReset()) {
                @RequiredUIAccess
                @Override
                public void actionPerformed(AnActionEvent e) {
                    myNotificationService.markAllRead(project);
                }
            }
        );
        gearActions.addSeparator();
        gearActions.add(
            new DumbAwareAction(ProjectUILocalize.actionNotificationSettingsText(), ProjectUILocalize.actionNotificationSettingsDescription(), PlatformIconGroup.generalSettings()) {
                @RequiredUIAccess
                @Override
                public void actionPerformed(AnActionEvent e) {
                    myShowConfigurableService.showAndSelect(project, NotificationsConfigurable.class);
                }
            }
        );
        toolWindow.setAdditionalGearActions(gearActions.build());

        ToolWindowManagerListener visibilityListener = new ToolWindowManagerListener() {
            private boolean myVisible;

            @Override
            public void stateChanged() {
                boolean visible = toolWindow.isVisible();
                if (myVisible && !visible) {
                    myNotificationService.markAllRead(project);
                }
                else if (!myVisible && visible && myWindowManager.get().getIdeFrame(project) instanceof IdeFrameEx frame) {
                    myToastService.hideAll(frame);
                }
                myVisible = visible;
            }
        };
        project.getMessageBus().connect(disposable).subscribe(ToolWindowManagerListener.class, visibilityListener);
        visibilityListener.stateChanged();
    }

    @Override
    public ToolWindowAnchor getAnchor() {
        return ToolWindowAnchor.RIGHT;
    }

    @Override
    public boolean isSecondary() {
        return true;
    }

    @Override
    public Image getIcon() {
        return PlatformIconGroup.toolwindowsNotifications();
    }

    @Override
    public LocalizeValue getDisplayName() {
        return ProjectUILocalize.toolwindowNotificationsDisplayName();
    }

    public static Image iconOf(NotificationService notificationService, Project project) {
        return NotificationIconBuilder.getIcon(notificationService.getUnreadNotifications(project).stream().map(Notification::getType).toList());
    }
}
