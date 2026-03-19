// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.application.util.concurrent;

import org.jspecify.annotations.Nullable;

import java.lang.management.ThreadInfo;

/**
 * Represents thread dump of the IDE captured by its performance diagnostic tool.
 */
public class ThreadDump {
  private final String myRawDump;
  private final StackTraceElement[] myEdtStack;
  private final ThreadInfo[] myThreadInfos;

  ThreadDump(String rawDump, @Nullable StackTraceElement[] edtStack, ThreadInfo[] threadInfos) {
    myRawDump = rawDump;
    myEdtStack = edtStack;
    myThreadInfos = threadInfos;
  }

  /**
   * @return full thread dump as a string
   */
  
  public String getRawDump() {
    return myRawDump;
  }

  /**
   * @return state of the AWT thread from the dump
   */
  public @Nullable StackTraceElement[] getEDTStackTrace() {
    return myEdtStack;
  }

  
  ThreadInfo[] getThreadInfos() {
    return myThreadInfos;
  }
}
