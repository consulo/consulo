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
package consulo.desktop.awt.startup;

import consulo.application.ApplicationManager;
import consulo.application.internal.MessagePool;
import consulo.ide.impl.idea.diagnostic.OutOfMemoryDialog;
import consulo.ide.impl.idea.diagnostic.VMOptions.MemoryKind;
import consulo.logging.internal.FatalErrorReporter;
import consulo.logging.internal.IdeaLoggingEvent;
import jakarta.annotation.Nullable;

import javax.swing.*;

/**
 * @author kir
 */
public class DefaultIdeaErrorLogger extends FatalErrorReporter {
  public static DefaultIdeaErrorLogger INSTANCE = new DefaultIdeaErrorLogger();

  private static boolean ourOomOccurred = false;
  private static boolean ourLoggerBroken = false;

  @Override
  public boolean canHandle(IdeaLoggingEvent event) {
    return !ourLoggerBroken;
  }

  @Override
  public void handle(IdeaLoggingEvent event) {
    if (ourLoggerBroken) return;

    try {
      Throwable throwable = event.getThrowable();
      final MemoryKind kind = getOOMErrorKind(throwable);
      if (kind != null) {
        ourOomOccurred = true;
        SwingUtilities.invokeAndWait(() -> new OutOfMemoryDialog(kind).showAsync());
      }
      else if (!ourOomOccurred) {
        MessagePool messagePool = MessagePool.getInstance();
        messagePool.addIdeFatalMessage(event);
      }
    }
    catch (Throwable e) {
      String message = e.getMessage();
      //noinspection InstanceofCatchParameter
      if (message != null && message.contains("Could not initialize class MessagePool") || e instanceof NullPointerException && ApplicationManager.getApplication() == null) {
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
}