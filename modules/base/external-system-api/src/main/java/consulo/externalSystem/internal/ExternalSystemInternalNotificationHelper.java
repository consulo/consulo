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
package consulo.externalSystem.internal;

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ServiceAPI;
import consulo.externalSystem.model.ProjectSystemId;
import consulo.externalSystem.service.notification.NotificationData;
import consulo.externalSystem.service.notification.NotificationSource;
import consulo.project.ui.notification.Notification;
import consulo.util.dataholder.Key;
import consulo.util.lang.Pair;
import consulo.virtualFileSystem.VirtualFile;
import jakarta.annotation.Nonnull;

/**
 * @author VISTALL
 * @since 2025-04-10
 */
@ServiceAPI(ComponentScope.PROJECT)
public interface ExternalSystemInternalNotificationHelper {
    @Nonnull
    Key<Pair<NotificationSource, ProjectSystemId>> CONTENT_ID_KEY = Key.create("CONTENT_ID");

    void addMessage(
        VirtualFile virtualFile,
        String groupName,
        @Nonnull Notification notification,
        @Nonnull ProjectSystemId externalSystemId,
        @Nonnull NotificationData notificationData
    );
}
