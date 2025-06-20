/*
 * Copyright 2000-2009 JetBrains s.r.o.
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

package consulo.project.ui.notification;

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ServiceAPI;
import consulo.application.Application;
import consulo.disposer.Disposable;
import consulo.project.Project;
import consulo.project.ui.wm.IdeFrame;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.popup.Balloon;
import consulo.util.lang.ref.SimpleReference;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import javax.swing.*;
import java.awt.*;

/**
 * @author spleaner
 */
@ServiceAPI(ComponentScope.APPLICATION)
public abstract class NotificationsManager {
    public static NotificationsManager getNotificationsManager() {
        return Application.get().getInstance(NotificationsManager.class);
    }

    public abstract void expire(@Nonnull Notification notification);

    @Nullable
    @RequiredUIAccess
    public abstract Window findWindowForBalloon(@Nullable Project project);

    public abstract <T extends Notification> T[] getNotificationsOfType(Class<T> klass, @Nullable Project project);

    @Nonnull
    public Balloon createBalloon(
        @Nonnull IdeFrame window,
        @Nonnull Notification notification,
        boolean showCallout,
        boolean hideOnClickOutside,
        @Nonnull SimpleReference<Object> layoutDataRef,
        @Nonnull Disposable parentDisposable
    ) {
        return createBalloon(window.getComponent(), notification, showCallout, hideOnClickOutside, layoutDataRef, parentDisposable);
    }

    public abstract Balloon createBalloon(
        @Nullable JComponent windowComponent,
        @Nonnull Notification notification,
        boolean showCallout,
        boolean hideOnClickOutside,
        @Nonnull SimpleReference<Object> layoutDataRef,
        @Nonnull Disposable parentDisposable
    );
}
