/*
 * Copyright 2000-2014 JetBrains s.r.o.
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
package consulo.project.ui.internal;

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ServiceAPI;
import consulo.application.Application;
import org.jspecify.annotations.Nullable;
import consulo.project.ui.notification.NotificationDisplayType;
import consulo.project.ui.notification.NotificationGroup;

@ServiceAPI(value = ComponentScope.APPLICATION)
public interface NotificationsConfiguration {
  void changeSettings(NotificationGroup group, NotificationDisplayType displayType, boolean shouldLog, boolean shouldReadAloud);

  /**
   * How notifications of the group are shown - the user's choice, else the default of the group.
   */
  NotificationDisplayType getDisplayType(String groupId);

  /**
   * Whether notifications of the group are kept in the Notifications tool window.
   */
  boolean isShouldLog(String groupId);

  /**
   * Whether notifications are shown as toasts at all.
   */
  boolean isShowBalloons();

  boolean isRegistered(String groupId);

  void changeDisplayType(String groupId, NotificationDisplayType displayType);

  @Nullable String getToolWindowId(String groupId);

  public static NotificationsConfiguration getNotificationsConfiguration() {
    return Application.get().getInstance(NotificationsConfiguration.class);
  }
}
