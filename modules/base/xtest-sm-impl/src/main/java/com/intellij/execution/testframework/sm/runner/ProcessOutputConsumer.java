// Copyright 2000-2017 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.execution.testframework.sm.runner;

import consulo.disposer.Disposable;
import consulo.util.dataholder.Key;

/**
 * @author Roman Chernyatchik
 */
public interface ProcessOutputConsumer extends Disposable {
  void setProcessor(GeneralTestEventsProcessor processor);
  void process(String text, Key outputType);

  /**
   * @deprecated use {@link #flushBufferOnProcessTermination(int)}
   */
  default void flushBufferBeforeTerminating() {}

  default void flushBufferOnProcessTermination(int exitCode) {
    flushBufferBeforeTerminating();
  }
}