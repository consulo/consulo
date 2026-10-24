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
package consulo.review.util;

import org.jspecify.annotations.Nullable;

import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutionException;

public final class FutureUtil {
    private FutureUtil() {
    }

    public static boolean isCancellation(@Nullable Throwable error) {
        return error != null && unwrap(error) instanceof CancellationException;
    }

    public static Throwable unwrap(Throwable error) {
        Throwable result = error;
        while (result instanceof CompletionException || result instanceof ExecutionException) {
            Throwable cause = result.getCause();
            if (cause == null) {
                break;
            }
            result = cause;
        }
        return result;
    }

    public static Exception unwrapException(Throwable error) {
        Throwable cause = unwrap(error);
        return cause instanceof Exception exception ? exception : new CompletionException(cause);
    }
}
