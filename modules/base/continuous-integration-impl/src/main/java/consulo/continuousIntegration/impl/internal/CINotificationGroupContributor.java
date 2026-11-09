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
package consulo.continuousIntegration.impl.internal;

import consulo.annotation.component.ExtensionImpl;
import consulo.continuousIntegration.impl.localize.ContinuousIntegrationImplLocalize;
import consulo.project.ui.notification.NotificationGroup;
import consulo.project.ui.notification.NotificationGroupContributor;

import java.util.function.Consumer;

/**
 * @since 2026-11-09
 */
@ExtensionImpl
public class CINotificationGroupContributor implements NotificationGroupContributor {
    public static final NotificationGroup BUILDS =
        NotificationGroup.balloonGroup("ContinuousIntegration.Builds", ContinuousIntegrationImplLocalize.notificationGroup());

    @Override
    public void contribute(Consumer<NotificationGroup> registrator) {
        registrator.accept(BUILDS);
    }
}
