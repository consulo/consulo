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
package consulo.test.light.impl;

import consulo.annotation.component.ComponentProfiles;
import consulo.annotation.component.ServiceImpl;
import consulo.disposer.Disposable;
import consulo.project.Project;
import consulo.project.ui.notification.Notification;
import consulo.project.ui.notification.NotificationsManager;
import consulo.ui.ex.popup.Balloon;
import consulo.util.lang.ref.SimpleReference;
import jakarta.inject.Singleton;
import org.jspecify.annotations.Nullable;

import javax.swing.JComponent;
import java.awt.Window;
import java.lang.reflect.Array;

@ServiceImpl(profiles = ComponentProfiles.LIGHT_TEST)
@Singleton
public class LightNotificationsManager extends NotificationsManager {
    @Override
    public void expire(Notification notification) {
    }

    @Override
    public @Nullable Window findWindowForBalloon(@Nullable Project project) {
        return null;
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T extends Notification> T[] getNotificationsOfType(Class<T> clazz, @Nullable Project project) {
        return (T[])Array.newInstance(clazz, 0);
    }

    @Override
    public Balloon createBalloon(
        @Nullable JComponent windowComponent,
        Notification notification,
        boolean showCallout,
        boolean hideOnClickOutside,
        SimpleReference<Object> layoutDataRef,
        Disposable parentDisposable
    ) {
        throw new UnsupportedOperationException();
    }
}
