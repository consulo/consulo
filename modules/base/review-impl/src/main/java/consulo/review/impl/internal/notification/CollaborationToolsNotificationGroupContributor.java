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
package consulo.review.impl.internal.notification;

import consulo.annotation.component.ExtensionImpl;
import consulo.project.ui.notification.NotificationGroup;
import consulo.project.ui.notification.NotificationGroupContributor;
import consulo.review.notification.CollaborationToolsNotificationGroups;

import java.util.function.Consumer;

/**
 * @author VISTALL
 * @since 2026-10-25
 */
@ExtensionImpl
public class CollaborationToolsNotificationGroupContributor implements NotificationGroupContributor {
    @Override
    public void contribute(Consumer<NotificationGroup> registrator) {
        registrator.accept(CollaborationToolsNotificationGroups.VCS_HOSTING_INTEGRATIONS);
    }
}
