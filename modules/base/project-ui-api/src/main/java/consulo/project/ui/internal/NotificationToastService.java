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
package consulo.project.ui.internal;

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ServiceAPI;
import consulo.project.Project;
import consulo.project.ui.notification.Notification;
import consulo.ui.Toast;
import consulo.ui.annotation.RequiredUIAccess;
import org.jspecify.annotations.Nullable;

/**
 * The cards of notifications: built once for the toasts floating over frames and for the Notifications tool window.
 * What {@link IdeFrameEx#showNotification(Notification)} and {@link IdeFrameEx#hideNotification(Notification)} do by
 * default.
 *
 * @author VISTALL
 * @since 2026-10-16
 */
@ServiceAPI(ComponentScope.APPLICATION)
public interface NotificationToastService {
    /**
     * @param close what the close button of the card does; {@code null} - the card has no close button
     */
    @RequiredUIAccess
    Toast createToast(Notification notification, @Nullable Project project, @Nullable Runnable close);

    /**
     * Floats the notification over the frame until it is hidden, it times out or the frame shows too many.
     */
    @RequiredUIAccess
    void show(IdeFrameEx frame, Notification notification);

    @RequiredUIAccess
    void hide(IdeFrameEx frame, Notification notification);

    /**
     * Takes every notification off the frame - the Notifications tool window of its project just showed them all.
     */
    @RequiredUIAccess
    void hideAll(IdeFrameEx frame);

    /**
     * Takes the notification off every frame it floats over, each in its own ui. Safe from any thread.
     */
    void hideEverywhere(Notification notification);

    /**
     * Keeps a notification of the application which had no frame to float over - one fired while the application
     * is still coming up - for the next frame which appears. Safe from any thread.
     */
    void showLater(Notification notification);

    /**
     * Floats the kept notifications over a frame which just appeared.
     */
    @RequiredUIAccess
    void showLaterOver(IdeFrameEx frame);
}
