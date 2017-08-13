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
package com.intellij.diagnostic;

import com.intellij.notification.Notification;
import com.intellij.notification.NotificationType;
import com.intellij.notification.Notifications;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.diagnostic.ErrorLogger;
import com.intellij.openapi.diagnostic.IdeaLoggingEvent;
import com.intellij.openapi.util.SystemInfo;
import com.intellij.util.io.MappingFailedException;

import javax.swing.*;
import java.lang.reflect.InvocationTargetException;

/**
 * @author kir
 */
public class DefaultIdeaErrorLogger implements ErrorLogger {
  private static boolean ourOomOccurred = false;
  private static boolean ourLoggerBroken = false;
  private static boolean ourMappingFailedNotificationPosted = false;

  @Override
  public boolean canHandle(IdeaLoggingEvent event) {
    return !ourLoggerBroken;
  }

  @Override
  public void handle(IdeaLoggingEvent event) {
    if (ourLoggerBroken) return;

    try {
      Throwable throwable = event.getThrowable();
      if (isOOMError(throwable)) {
        processOOMError(throwable);
      }
      else if (throwable instanceof MappingFailedException) {
        processMappingFailed(event);
      }
      else if (!ourOomOccurred) {
        MessagePool messagePool = MessagePool.getInstance();
        LogMessage message = messagePool.addIdeFatalMessage(event);
        if (message != null && ApplicationManager.getApplication() != null) {
          ErrorNotifier.notifyUi(message, messagePool);
        }
      }
    }
    catch (Throwable e) {
      String message = e.getMessage();
      //noinspection InstanceofCatchParameter
      if (message != null && message.contains("Could not initialize class com.intellij.diagnostic.MessagePool") ||
          e instanceof NullPointerException && ApplicationManager.getApplication() == null) {
        //noinspection AssignmentToStaticFieldFromInstanceMethod
        ourLoggerBroken = true;
      }
    }
  }

  private static boolean isOOMError(Throwable throwable) {
    return throwable instanceof OutOfMemoryError || (throwable instanceof VirtualMachineError &&
                                                     throwable.getMessage() != null &&
                                                     throwable.getMessage().contains("CodeCache"));
  }

  private static void processOOMError(final Throwable throwable) throws InterruptedException, InvocationTargetException {
    ourOomOccurred = true;

    SwingUtilities.invokeAndWait(() -> {
      String message = throwable.getMessage();
      OutOfMemoryDialog.MemoryKind k =
              message != null && message.contains("CodeCache") ? OutOfMemoryDialog.MemoryKind.CODE_CACHE : OutOfMemoryDialog.MemoryKind.HEAP;
      new OutOfMemoryDialog(k).show();
    });
  }

  private static void processMappingFailed(final IdeaLoggingEvent event) throws InterruptedException, InvocationTargetException {
    if (!ourMappingFailedNotificationPosted && SystemInfo.isWindows && SystemInfo.is32Bit) {
      ourMappingFailedNotificationPosted = true;
      @SuppressWarnings("ThrowableResultOfMethodCallIgnored") String exceptionMessage = event.getThrowable().getMessage();
      String text = exceptionMessage +
                    "<br>Possible cause: unable to allocate continuous memory chunk of necessary size.<br>" +
                    "Reducing JVM maximum heap size (-Xmx) may help.";
      Notifications.Bus.notify(new Notification("Memory", "Memory Mapping Failed", text, NotificationType.WARNING), null);
    }
  }
}
