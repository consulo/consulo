/*
 * Copyright 2000-2013 JetBrains s.r.o.
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
package consulo.externalSystem.service.notification;

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ExtensionAPI;
import consulo.externalSystem.model.ProjectSystemId;
import consulo.project.Project;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

/**
 * Allows to customize {@link ExternalSystemNotificationManager external system notifications} shown to end-user by the ide.
 *
 * @author Denis Zhdanov
 * @since 8/5/13 8:52 AM
 */
@ExtensionAPI(ComponentScope.APPLICATION)
public interface ExternalSystemNotificationExtension {

  @Nonnull
  ProjectSystemId getTargetExternalSystemId();

  /**
   * Allows to customize external system processing notification.
   *
   * @param notificationData notification data
   * @param project          target ide project
   * @param error            error occurred during external system processing
   */
  void customize(@Nonnull NotificationData notificationData, @Nonnull Project project, @Nullable Throwable error);
}
