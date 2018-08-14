/*
 * Copyright 2000-2017 JetBrains s.r.o.
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

import com.intellij.diagnostic.VMOptions.MemoryKind;
import com.intellij.notification.Notification;
import com.intellij.notification.NotificationType;
import com.intellij.notification.Notifications;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.diagnostic.IdeaLoggingEvent;
import com.intellij.openapi.util.SystemInfo;
import com.intellij.util.io.MappingFailedException;

import javax.annotation.Nullable;
import javax.swing.*;
import java.lang.reflect.InvocationTargetException;

/**
 * @author kir
 */
public class DefaultIdeaErrorLogger {
  public static DefaultIdeaErrorLogger INSTANCE = new DefaultIdeaErrorLogger();

  private static boolean ourOomOccurred = false;
  private static boolean ourLoggerBroken = false;
  private static boolean ourMappingFailedNotificationPosted = false;

  public boolean canHandle(IdeaLoggingEvent event) {
    return !ourLoggerBroken;
  }

  public void handle(IdeaLoggingEvent event) {
    if (ourLoggerBroken) return;

    try {
      Throwable throwable = event.getThrowable();
      final MemoryKind kind = getOOMErrorKind(throwable);
      if (kind != null) {
        ourOomOccurred = true;
        SwingUtilities.invokeAndWait(() -> new OutOfMemoryDialog(kind).show());
      }
      else if (throwable instanceof MappingFailedException) {
        processMappingFailed(event);
      }
      else if (!ourOomOccurred) {
        MessagePool messagePool = MessagePool.getInstance();
        messagePool.addIdeFatalMessage(event);
      }
    }
    catch (Throwable e) {
      String message = e.getMessage();
      //noinspection InstanceofCatchParameter
      if (message != null && message.contains("Could not initialize class com.intellij.diagnostic.MessagePool") || e instanceof NullPointerException && ApplicationManager.getApplication() == null) {
        ourLoggerBroken = true;
      }
    }
  }

  @Nullable
  private static MemoryKind getOOMErrorKind(Throwable t) {
    String message = t.getMessage();

    if (t instanceof OutOfMemoryError) {
      if (message != null && message.contains("unable to create new native thread")) return null;
      if (message != null && message.contains("Metaspace")) return MemoryKind.METASPACE;
      return MemoryKind.HEAP;
    }

    if (t instanceof VirtualMachineError && message != null && message.contains("CodeCache")) {
      return MemoryKind.CODE_CACHE;
    }

    return null;
  }

  private static void processMappingFailed(IdeaLoggingEvent event) throws InterruptedException, InvocationTargetException {
    if (!ourMappingFailedNotificationPosted && SystemInfo.isWindows && SystemInfo.is32Bit) {
      ourMappingFailedNotificationPosted = true;
      @SuppressWarnings("ThrowableResultOfMethodCallIgnored") String exceptionMessage = event.getThrowable().getMessage();
      String text = exceptionMessage + "<br>Possible cause: unable to allocate continuous memory chunk of necessary size.<br>" + "Reducing JVM maximum heap size (-Xmx) may help.";
      Notifications.Bus.notify(new Notification("Memory", "Memory Mapping Failed", text, NotificationType.WARNING), null);
    }
  }
}