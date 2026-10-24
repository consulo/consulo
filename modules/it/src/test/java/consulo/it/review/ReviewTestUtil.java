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
package consulo.it.review;

import consulo.util.concurrent.coroutine.ObservableValue;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Predicate;

final class ReviewTestUtil {
    private ReviewTestUtil() {
    }

    static <T> T await(CompletableFuture<T> future) throws Exception {
        return future.get(5, TimeUnit.SECONDS);
    }

    static <V> V first(ObservableValue<V> value, Predicate<V> predicate) throws Exception {
        CompletableFuture<V> result = new CompletableFuture<>();
        Runnable unsubscribe = value.addListener(newValue -> {
            if (predicate.test(newValue)) {
                result.complete(newValue);
            }
        });
        V current = value.get();
        if (predicate.test(current)) {
            result.complete(current);
        }
        try {
            return await(result);
        }
        finally {
            unsubscribe.run();
        }
    }
}
