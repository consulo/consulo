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
import consulo.disposer.Disposer;
import consulo.project.Project;
import consulo.project.ui.internal.NotificationIconBuilder;
import consulo.project.ui.notification.Notification;
import consulo.project.ui.notification.NotificationService;
import consulo.project.ui.notification.event.NotificationServiceListener;
import consulo.ui.Button;
import consulo.ui.LightPopup;
import consulo.ui.PopupOptions;
import consulo.ui.PopupPosition;
import consulo.ui.UIAccess;
import consulo.ui.annotation.RequiredUIAccess;
import org.jspecify.annotations.Nullable;

/**
 * The Action Center of a welcome screen: a button which shows while the application holds notifications, marked
 * while some are unread, and opens their list in a popup.
 *
 * @author VISTALL
 * @since 2026-10-16
 */
public final class WelcomeNotifications {
    private static final int POPUP_WIDTH = 420;

    @RequiredUIAccess
    public static void install(Button button, Disposable parentDisposable) {
        NotificationService service = Application.get().getInstance(NotificationService.class);
        UIAccess uiAccess = UIAccess.current();

        Runnable refresh = () -> {
            button.setVisible(!service.getNotifications(null).isEmpty());
            button.setIcon(NotificationIconBuilder.getIcon(service.getUnreadNotifications(null).stream().map(Notification::getType).toList()));
        };

        Application.get().getMessageBus().connect(parentDisposable).subscribe(NotificationServiceListener.class, new NotificationServiceListener() {
            @Override
            public void stateChanged(@Nullable Project project) {
                if (project == null) {
                    uiAccess.give(refresh);
                }
            }
        });

        button.addClickListener(event -> showPopup(button, service));
        refresh.run();
    }

    @RequiredUIAccess
    private static void showPopup(Button button, NotificationService service) {
        Disposable disposable = Disposable.newDisposable("WelcomeNotifications");
        NotificationListPanel panel = new NotificationListPanel(null, disposable);

        LightPopup popup = LightPopup.create(PopupOptions.builder().position(PopupPosition.BOTTOM).resizable().build());
        popup.setContent(panel.getComponent());
        popup.setMinimumWidth(POPUP_WIDTH);
        popup.addCloseListener(event -> {
            Disposer.dispose(disposable);
            service.markAllRead(null);
        });
        popup.showBy(button);
    }

    private WelcomeNotifications() {
    }
}
