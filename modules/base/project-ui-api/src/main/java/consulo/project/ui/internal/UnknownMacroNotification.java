/*
 * Copyright 2000-2009 JetBrains s.r.o.
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

import consulo.project.ui.notification.Notification;
import consulo.project.ui.notification.NotificationGroup;
import consulo.project.ui.notification.NotificationType;
import consulo.project.ui.notification.event.NotificationListener;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import java.util.Collection;

/**
 * @author spleaner
 */
public class UnknownMacroNotification extends Notification {
  private final Collection<String> myMacros;

  public UnknownMacroNotification(@Nonnull NotificationGroup group,
                                  @Nonnull String title,
                                  @Nonnull String content,
                                  @Nonnull NotificationType type,
                                  @Nullable NotificationListener listener,
                                  @Nonnull Collection<String> macros) {
    super(group, title, content, type, listener);

    myMacros = macros;
  }

  public Collection<String> getMacros() {
    return myMacros;
  }
}
