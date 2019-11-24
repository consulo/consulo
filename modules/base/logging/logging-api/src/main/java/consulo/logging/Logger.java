/*
 * Copyright 2013-2019 consulo.io
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
package consulo.logging;

import consulo.util.nodep.ArrayUtilRt;
import consulo.logging.attachment.Attachment;
import consulo.logging.internal.LoggerFactoryInitializer;
import org.jetbrains.annotations.NonNls;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * @author VISTALL
 * @since 2019-08-10
 */
public interface Logger {
  @Nonnull
  public static Logger getInstance(@NonNls String category) {
    return LoggerFactoryInitializer.getFactory().getLoggerInstance(category);
  }

  @Nonnull
  public static Logger getInstance(@Nonnull Class cl) {
    return getInstance(cl.getName());
  }

  default boolean isTraceEnabled() {
    return isDebugEnabled();
  }

  /**
   * Log a message with 'trace' level which finer-grained than 'debug' level. Use this method instead of {@link #debug(String)} for internal
   * events of a subsystem to avoid overwhelming the log if 'debug' level is enabled.
   */
  default void trace(String message) {
    debug(message);
  }

  default void trace(Throwable message) {
    debug(message);
  }

  public abstract boolean isDebugEnabled();

  public abstract void debug(@NonNls String message);

  public abstract void debug(@Nullable Throwable t);

  public abstract void debug(@NonNls String message, @Nullable Throwable t);

  default void debug(@Nonnull String message, Object... details) {
    if (isDebugEnabled()) {
      StringBuilder sb = new StringBuilder();
      sb.append(message);
      for (Object detail : details) {
        sb.append(String.valueOf(detail));
      }
      debug(sb.toString());
    }
  }

  default void info(@Nonnull Throwable t) {
    info(t.getMessage(), t);
  }

  public abstract void info(@NonNls String message);

  public abstract void info(@NonNls String message, @Nullable Throwable t);

  default void warn(@NonNls String message) {
    warn(message, null);
  }

  default void warn(@Nonnull Throwable t) {
    warn(t.getMessage(), t);
  }

  public abstract void warn(@NonNls String message, @Nullable Throwable t);

  default void error(@NonNls String message) {
    error(message, new Throwable(), ArrayUtilRt.EMPTY_STRING_ARRAY);
  }

  default void error(Object message) {
    error(String.valueOf(message));
  }

  default void error(@NonNls String message, Attachment... attachments) {
    error(message);
  }

  default void error(@NonNls String message, @Nullable Throwable throwable,  Attachment... attachments) {
    error(message);
  }

  default void error(@NonNls String message, @NonNls String... details) {
    error(message, new Throwable(), details);
  }

  default void error(@NonNls String message, @Nullable Throwable e) {
    error(message, e, ArrayUtilRt.EMPTY_STRING_ARRAY);
  }

  default void error(@Nonnull Throwable t) {
    error(t.getMessage(), t, ArrayUtilRt.EMPTY_STRING_ARRAY);
  }

  public abstract void error(@NonNls String message, @Nullable Throwable t, @NonNls @Nonnull String... details);

  default boolean assertTrue(boolean value, @Nullable @NonNls Object message) {
    if (!value) {
      @NonNls String resultMessage = "Assertion failed";
      if (message != null) resultMessage += ": " + message;
      error(resultMessage, new Throwable());
    }

    return value;
  }

  default boolean assertTrue(boolean value) {
    return value || assertTrue(false, null);
  }
}
