/*
 * Copyright 2013-2020 consulo.io
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
package com.intellij.diagnostic;

import com.intellij.notification.Notification;
import com.intellij.notification.NotificationType;
import com.intellij.notification.impl.NotificationFullContent;

import javax.annotation.Nonnull;
import java.nio.file.Path;
import java.util.Collection;

/**
 * from kotlin
 */
public class WindowsDefenderNotification extends Notification implements NotificationFullContent {
  private final Collection<Path> myPaths;

  public WindowsDefenderNotification(String text, Collection<Path> paths) {
     super("System Health", "", text, NotificationType.WARNING);
    myPaths = paths;
  }

  @Nonnull
  public Collection<Path> getPaths() {
    return myPaths;
  }
}
