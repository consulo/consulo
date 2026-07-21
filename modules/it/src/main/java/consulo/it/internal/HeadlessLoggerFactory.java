/*
 * Copyright 2013-2026 consulo.io
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
package consulo.it.internal;

import consulo.logging.Logger;
import consulo.logging.internal.LoggerFactory;
import org.jspecify.annotations.Nullable;

/**
 * Headless {@link LoggerFactory}: logs to stderr but, unlike {@code DefaultLogger}, does not throw
 * {@link AssertionError} on {@code error(...)}. This keeps recoverable {@code LOG.error} calls
 * (e.g. the native file watcher failing in a temp-dir project) non-fatal so they do not mask the
 * real flow under test.
 *
 * @author VISTALL
 */
public class HeadlessLoggerFactory implements LoggerFactory {
    @Override
    public Logger getLoggerInstance(String category) {
        return new HeadlessLogger(category);
    }

    private static class HeadlessLogger implements Logger {
        private final String myCategory;

        HeadlessLogger(String category) {
            myCategory = category;
        }

        @Override
        public boolean isDebugEnabled() {
            return false;
        }

        @Override
        public void debug(String message) {
        }

        @Override
        public void debug(Throwable t) {
        }

        @Override
        public void debug(String message, @Nullable Throwable t) {
        }

        @Override
        public void info(String message) {
        }

        @Override
        @SuppressWarnings({"UseOfSystemOutOrSystemErr", "CallToPrintStackTrace"})
        public void info(String message, @Nullable Throwable t) {
            if (t != null) {
                System.err.println("INFO [" + myCategory + "]: " + message);
                t.printStackTrace();
            }
        }

        @Override
        @SuppressWarnings({"UseOfSystemOutOrSystemErr", "CallToPrintStackTrace"})
        public void warn(String message, @Nullable Throwable t) {
            System.err.println("WARN [" + myCategory + "]: " + message);
            if (t != null) {
                t.printStackTrace();
            }
        }

        @Override
        @SuppressWarnings({"UseOfSystemOutOrSystemErr", "CallToPrintStackTrace"})
        public void error(String message, @Nullable Throwable t, String... details) {
            System.err.println("ERROR [" + myCategory + "]: " + message);
            if (t != null) {
                t.printStackTrace();
            }
            if (details != null && details.length > 0) {
                for (String detail : details) {
                    System.err.println("  " + detail);
                }
            }
        }
    }
}
