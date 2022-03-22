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
package consulo.component.store.impl.internal;

import consulo.application.Application;
import consulo.component.ComponentManager;
import consulo.ui.NotificationType;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * @author VISTALL
 * @since 22-Mar-22
 */
public interface StorageNotificationService {
  static StorageNotificationService getInstance() {
    return Application.get().getInstance(StorageNotificationService.class);
  }

  void notify(@Nonnull NotificationType notificationType, @Nonnull String title, @Nonnull String text, @Nullable ComponentManager project);

  void notifyUnknownMacros(@Nonnull TrackingPathMacroSubstitutor substitutor, @Nonnull final ComponentManager project, @Nullable final String componentName);
}
