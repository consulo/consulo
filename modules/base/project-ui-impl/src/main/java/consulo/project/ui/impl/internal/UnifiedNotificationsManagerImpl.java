/*
 * Copyright 2013-2025 consulo.io
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
package consulo.project.ui.impl.internal;

import consulo.annotation.component.ComponentProfiles;
import consulo.annotation.component.ServiceImpl;
import consulo.disposer.Disposable;
import consulo.project.Project;
import consulo.project.ui.notification.Notification;
import consulo.project.ui.notification.NotificationsManager;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.popup.Balloon;
import consulo.util.lang.ref.SimpleReference;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import jakarta.inject.Singleton;

import javax.swing.*;
import java.awt.*;
import java.lang.reflect.Array;

/**
 * @author VISTALL
 * @since 2025-03-10
 */
@ServiceImpl(profiles = ComponentProfiles.UNIFIED)
@Singleton
public class UnifiedNotificationsManagerImpl extends NotificationsManager {
    @Override
    public void expire(@Nonnull Notification notification) {
    }

    @Nullable
    @Override
    @RequiredUIAccess
    public Window findWindowForBalloon(@Nullable Project project) {
        return null;
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T extends Notification> T[] getNotificationsOfType(Class<T> klass, @Nullable Project project) {
        return (T[]) Array.newInstance(klass, 0);
    }

    @Override
    public Balloon createBalloon(
        @Nullable JComponent windowComponent,
        @Nonnull Notification notification,
        boolean showCallout,
        boolean hideOnClickOutside,
        @Nonnull SimpleReference<Object> layoutDataRef,
        @Nonnull Disposable parentDisposable
    ) {
        return null;
    }
}
