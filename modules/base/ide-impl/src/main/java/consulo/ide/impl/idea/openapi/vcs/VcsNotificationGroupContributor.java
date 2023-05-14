/*
 * Copyright 2013-2022 consulo.io
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
package consulo.ide.impl.idea.openapi.vcs;

import consulo.annotation.component.ExtensionImpl;
import consulo.versionControlSystem.ui.VcsBalloonProblemNotifier;
import consulo.project.ui.notification.NotificationGroup;
import consulo.project.ui.notification.NotificationGroupContributor;
import consulo.versionControlSystem.VcsNotifier;

import jakarta.annotation.Nonnull;
import java.util.function.Consumer;

/**
 * @author VISTALL
 * @since 08-Aug-22
 */
@ExtensionImpl
public class VcsNotificationGroupContributor implements NotificationGroupContributor {
  @Override
  public void contribute(@Nonnull Consumer<NotificationGroup> registrator) {
     registrator.accept(VcsNotifier.IMPORTANT_ERROR_NOTIFICATION);
     registrator.accept(VcsNotifier.NOTIFICATION_GROUP_ID);
     registrator.accept(VcsNotifier.SILENT_NOTIFICATION);
     registrator.accept(VcsNotifier.STANDARD_NOTIFICATION);
     registrator.accept(VcsBalloonProblemNotifier.NOTIFICATION_GROUP);
  }
}
