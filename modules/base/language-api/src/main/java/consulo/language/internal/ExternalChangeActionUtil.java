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
package consulo.language.internal;

/**
 * Utility for marking write actions that originate from external sources
 * (VFS refresh, file reload from disk, etc.) rather than from user edits.
 */
public final class ExternalChangeActionUtil {
    private static final ThreadLocal<Integer> ourExternalChangeDepth = ThreadLocal.withInitial(() -> 0);
    private static final ThreadLocal<Integer> ourExternalDocumentChangeDepth = ThreadLocal.withInitial(() -> 0);

    private ExternalChangeActionUtil() {
    }

    /**
     * Returns {@code true} if the current thread is executing inside an external change action.
     */
    public static boolean isExternalChangeInProgress() {
        return ourExternalChangeDepth.get() > 0;
    }

    /**
     * Returns {@code true} if the current thread is executing inside an external document change action
     * (i.e., document content is being reloaded from an external source).
     */
    public static boolean isExternalDocumentChangeInProgress() {
        return ourExternalDocumentChangeDepth.get() > 0;
    }

    /**
     * Wraps a {@link Runnable} so that {@link #isExternalChangeInProgress()} returns
     * {@code true} for the duration of its execution on the current thread.
     * Supports nesting.
     */
    public static Runnable externalChangeAction(Runnable runnable) {
        return () -> {
            ourExternalChangeDepth.set(ourExternalChangeDepth.get() + 1);
            try {
                runnable.run();
            }
            finally {
                ourExternalChangeDepth.set(ourExternalChangeDepth.get() - 1);
            }
        };
    }

    /**
     * Wraps a {@link Runnable} so that both {@link #isExternalChangeInProgress()} and
     * {@link #isExternalDocumentChangeInProgress()} return {@code true} for the duration
     * of its execution on the current thread. Supports nesting.
     */
    public static Runnable externalDocumentChangeAction(Runnable runnable) {
        return () -> {
            ourExternalChangeDepth.set(ourExternalChangeDepth.get() + 1);
            ourExternalDocumentChangeDepth.set(ourExternalDocumentChangeDepth.get() + 1);
            try {
                runnable.run();
            }
            finally {
                ourExternalChangeDepth.set(ourExternalChangeDepth.get() - 1);
                ourExternalDocumentChangeDepth.set(ourExternalDocumentChangeDepth.get() - 1);
            }
        };
    }
}
