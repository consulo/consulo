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
package consulo.component.store.internal;

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ServiceAPI;
import consulo.component.ComponentManager;
import consulo.component.internal.RootComponentHolder;
import consulo.ui.NotificationType;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

/**
 * @author VISTALL
 * @since 22-Mar-22
 */
@ServiceAPI(ComponentScope.APPLICATION)
public interface StorageNotificationService {
  static StorageNotificationService getInstance() {
    return RootComponentHolder.getRootComponent().getInstance(StorageNotificationService.class);
  }

  void notify(@Nonnull NotificationType notificationType, @Nonnull String title, @Nonnull String text, @Nullable ComponentManager project);

  void notifyUnknownMacros(@Nonnull TrackingPathMacroSubstitutor substitutor, @Nonnull final ComponentManager project, @Nullable final String componentName);
}
