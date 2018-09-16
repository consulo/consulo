package com.intellij.remoteServer.runtime.log;

import com.intellij.execution.process.ProcessHandler;
import javax.annotation.Nonnull;

/**
 * @author nik
 */
public interface LoggingHandler {
  void print(@Nonnull String s);

  void attachToProcess(@Nonnull ProcessHandler handler);
}
