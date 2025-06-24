/*
 * Copyright 2013-2020 consulo.io
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
package consulo.desktop.util.windows.defender;

import consulo.project.ui.internal.NotificationFullContent;
import consulo.project.ui.notification.Notification;
import jakarta.annotation.Nonnull;

import java.nio.file.Path;
import java.util.Collection;

/**
 * from kotlin
 */
public class WindowsDefenderNotification extends Notification implements NotificationFullContent {
    private final Collection<Path> myPaths;

    public WindowsDefenderNotification(@Nonnull Notification.Builder notificationBuilder, Collection<Path> paths) {
        super(notificationBuilder);
        myPaths = paths;
    }

    @Nonnull
    public Collection<Path> getPaths() {
        return myPaths;
    }
}
