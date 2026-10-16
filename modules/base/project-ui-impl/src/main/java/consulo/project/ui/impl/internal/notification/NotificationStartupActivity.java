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
import consulo.project.Project;
import consulo.project.startup.PostStartupActivity;
import consulo.project.ui.internal.IdeFrameEx;
import consulo.project.ui.internal.NotificationToastService;
import consulo.project.ui.wm.WindowManager;
import consulo.ui.UIAccess;
import jakarta.inject.Inject;
import jakarta.inject.Provider;

/**
 * Floats the notifications of the application which came before any frame over the frame of the project opened first.
 *
 * @author VISTALL
 * @since 2026-10-17
 */
@ExtensionImpl
public class NotificationStartupActivity implements PostStartupActivity, DumbAware {
    private final Provider<NotificationToastService> myToastService;
    private final Provider<WindowManager> myWindowManager;

    @Inject
    public NotificationStartupActivity(Provider<NotificationToastService> toastService, Provider<WindowManager> windowManager) {
        myToastService = toastService;
        myWindowManager = windowManager;
    }

    @Override
    public void runActivity(Project project, UIAccess uiAccess) {
        uiAccess.give(() -> {
            if (!project.isDisposed() && myWindowManager.get().getIdeFrame(project) instanceof IdeFrameEx frame) {
                myToastService.get().showLaterOver(frame);
            }
        });
    }
}
