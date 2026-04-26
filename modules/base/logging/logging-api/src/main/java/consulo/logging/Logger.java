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

import consulo.logging.attachment.Attachment;
import consulo.logging.internal.LoggerFactoryInitializer;
import consulo.util.lang.StringUtil;
import org.jspecify.annotations.Nullable;

/**
 * @author VISTALL
 * @since 2019-08-10
 */
public interface Logger {
    static final String[] EMPTY_STRING_ARRAY = new String[0];

    public static Logger getInstance(String category) {
        return LoggerFactoryInitializer.getFactory().getLoggerInstance(category);
    }

    public static Logger getInstance(Class clazz) {
        return LoggerFactoryInitializer.getFactory().getLoggerInstance(clazz);
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

    public abstract void debug(String message);

    public abstract void debug(Throwable t);

    public abstract void debug(String message, @Nullable Throwable t);

    default void debug(String message, Object... details) {
        if (isDebugEnabled()) {
            StringBuilder sb = new StringBuilder();
            sb.append(message);
            for (Object detail : details) {
                sb.append(String.valueOf(detail));
            }
            debug(sb.toString());
        }
    }

    default void info(Throwable t) {
        info(StringUtil.notNullize(t.getMessage()), t);
    }

    public abstract void info(String message);

    public abstract void info(String message, @Nullable Throwable t);

    default void warn(String message) {
        warn(message, null);
    }

    default void warn(Throwable t) {
        warn(StringUtil.notNullize(t.getMessage()), t);
    }

    public abstract void warn(String message, @Nullable Throwable t);

    default void error(String message) {
        error(message, new Throwable(), EMPTY_STRING_ARRAY);
    }

    default void error(Object message) {
        error(String.valueOf(message));
    }

    default void error(String message, String details, Attachment... attachments) {
        error(message);
    }

    default void error(String message, Attachment... attachments) {
        error(message);
    }

    default void error(String message, @Nullable Throwable throwable, Attachment... attachments) {
        error(message);
    }

    default void error(String message, String... details) {
        error(message, new Throwable(), details);
    }

    default void error(String message, @Nullable Throwable e) {
        error(message, e, EMPTY_STRING_ARRAY);
    }

    default void error(Throwable t) {
        error(StringUtil.notNullize(t.getMessage()), t, EMPTY_STRING_ARRAY);
    }

    public abstract void error(String message, @Nullable Throwable t, String... details);

    default boolean assertTrue(boolean value, @Nullable Object message) {
        if (!value) {
            String resultMessage = "Assertion failed";
            if (message != null) {
                resultMessage += ": " + message;
            }
            error(resultMessage, new Throwable());
        }

        return value;
    }

    default boolean assertTrue(boolean value) {
        return value || assertTrue(false, null);
    }

    /**
     * Set level to logger. Not guaranteed action - some loggers can ignore method invoke
     * <p>
     * WARNING: Can be called only from platform - or will throw error
     */
    default void setLevel(LoggerLevel level) throws IllegalAccessException {
        // nothing by default
    }
}
