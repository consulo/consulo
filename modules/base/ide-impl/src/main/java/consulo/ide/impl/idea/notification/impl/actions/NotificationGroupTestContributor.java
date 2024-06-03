/*
 * Copyright 2013-2024 consulo.io
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
package consulo.ide.impl.idea.notification.impl.actions;

import consulo.annotation.component.ExtensionImpl;
import consulo.application.ApplicationProperties;
import consulo.project.ui.notification.NotificationGroup;
import consulo.project.ui.notification.NotificationGroupContributor;
import jakarta.annotation.Nonnull;

import java.util.function.Consumer;

/**
 * @author VISTALL
 * @since 03/06/2024
 */
@ExtensionImpl
public class NotificationGroupTestContributor implements NotificationGroupContributor {
  @Override
  public void contribute(@Nonnull Consumer<NotificationGroup> registrator) {
      if (!ApplicationProperties.isInSandbox()) {
        return;
      }

      registrator.accept(NotificationTestAction.TEST_GROUP);
      registrator.accept(NotificationTestAction.TEST_STICKY_GROUP);
      registrator.accept(NotificationTestAction.TEST_TOOLWINDOW_GROUP);
  }
}
