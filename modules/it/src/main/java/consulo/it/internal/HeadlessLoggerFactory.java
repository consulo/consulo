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

import consulo.it.LoggedError;
import consulo.logging.Logger;
import consulo.logging.internal.LoggerFactory;
import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Headless {@link LoggerFactory}: logs to stderr and records every {@code error(...)} so that the test it was
 * logged in can be failed once it returns.
 * <p>
 * The call itself does not throw. A {@code LOG.error} is a report, not a control flow branch - the caller keeps
 * running after it, and throwing instead would take a path production never takes, so the test would no longer be
 * exercising the code it is there to cover.
 *
 * @author VISTALL
 */
public class HeadlessLoggerFactory implements LoggerFactory {
    private static final List<LoggedError> ourLoggedErrors = new CopyOnWriteArrayList<>();

    private static volatile List<String> ourAllowedCategories = null;

    /**
     * Opt-out, rejected by default. A null list rejects every category, an empty one allows every category, and
     * otherwise a category is allowed when it starts with one of the entries.
     */
    public static void setAllowedErrorCategories(@Nullable List<String> categories) {
        ourAllowedCategories = categories;
    }

    public static List<LoggedError> takeLoggedErrors() {
        List<LoggedError> errors = List.copyOf(ourLoggedErrors);
        ourLoggedErrors.clear();
        return errors;
    }

    private static void record(String category, String message, @Nullable Throwable t) {
        List<String> allowed = ourAllowedCategories;
        if (allowed != null && (allowed.isEmpty() || allowed.stream().anyMatch(category::startsWith))) {
            return;
        }
        ourLoggedErrors.add(new LoggedError(category, message, t));
    }

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
            record(myCategory, message, t);

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
