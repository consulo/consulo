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
package com.intellij.openapi.diagnostic;

import com.intellij.util.ArrayUtil;
import consulo.annotation.DeprecationInfo;
import consulo.logging.attachment.Attachment;
import org.jetbrains.annotations.NonNls;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

@Deprecated
@DeprecationInfo("Use consulo.logging.Logger")
public final class Logger {

  private final consulo.logging.Logger myDelegate;

  public static Logger getInstance(@NonNls String category) {
    return new Logger(consulo.logging.Logger.getInstance(category));
  }

  public static Logger getInstance(Class cl) {
    return getInstance(cl.getName());
  }

  private Logger(@Nonnull consulo.logging.Logger delegate) {
    myDelegate = delegate;
  }

  public boolean isTraceEnabled() {
    return isDebugEnabled();
  }

  /**
   * Log a message with 'trace' level which finer-grained than 'debug' level. Use this method instead of {@link #debug(String)} for internal
   * events of a subsystem to avoid overwhelming the log if 'debug' level is enabled.
   */
  public void trace(String message) {
    debug(message);
  }

  public void trace(Throwable message) {
    debug(message);
  }

  public boolean isDebugEnabled() {
    return myDelegate.isDebugEnabled();
  }

  public void debug(@NonNls String message) {
    myDelegate.debug(message);
  }

  public void debug(@Nullable Throwable t) {
    myDelegate.debug(t);
  }

  public void debug(@NonNls String message, @Nullable Throwable t) {
    myDelegate.debug(message, t);
  }

  public void debug(@Nonnull String message, Object... details) {
    if (isDebugEnabled()) {
      StringBuilder sb = new StringBuilder();
      sb.append(message);
      for (Object detail : details) {
        sb.append(String.valueOf(detail));
      }
      debug(sb.toString());
    }
  }

  public void info(@Nonnull Throwable t) {
    info(t.getMessage(), t);
  }

  public void info(@NonNls String message) {
    myDelegate.info(message);
  }

  public void info(@NonNls String message, @Nullable Throwable t) {
    myDelegate.info(message, t);
  }

  public void warn(@NonNls String message) {
    warn(message, null);
  }

  public void warn(@Nonnull Throwable t) {
    warn(t.getMessage(), t);
  }

  public void warn(@NonNls String message, @Nullable Throwable t) {
    myDelegate.warn(message, t);
  }

  public void error(@NonNls String message) {
    error(message, new Throwable(), ArrayUtil.EMPTY_STRING_ARRAY);
  }

  public void error(Object message) {
    error(String.valueOf(message));
  }

  public void error(@NonNls String message, Attachment... attachments) {
    error(message);
  }
  
  public void error(@NonNls String message, Throwable throwable, Attachment... attachments) {
    error(message);
  }

  public void error(@NonNls String message, @NonNls String... details) {
    error(message, new Throwable(), details);
  }

  public void error(@NonNls String message, @Nullable Throwable e) {
    error(message, e, ArrayUtil.EMPTY_STRING_ARRAY);
  }

  public void error(@Nonnull Throwable t) {
    error(t.getMessage(), t, ArrayUtil.EMPTY_STRING_ARRAY);
  }

  public void error(@NonNls String message, @Nullable Throwable t, @NonNls @Nonnull String... details) {
    myDelegate.error(message, t, details);
  }

  public boolean assertTrue(boolean value, @Nullable @NonNls Object message) {
    if (!value) {
      @NonNls String resultMessage = "Assertion failed";
      if (message != null) resultMessage += ": " + message;
      error(resultMessage, new Throwable());
    }

    return value;
  }

  public boolean assertTrue(boolean value) {
    return value || assertTrue(false, null);
  }
}
