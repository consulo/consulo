/*
 * Copyright 2013-2018 consulo.io
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
package consulo.application;

import consulo.annotation.ReviewAfterIssueFix;
import consulo.annotation.access.RequiredReadAction;
import consulo.annotation.access.RequiredWriteAction;
import consulo.logging.Logger;
import consulo.util.lang.function.ThrowableRunnable;
import consulo.util.lang.function.ThrowableSupplier;
import org.jspecify.annotations.Nullable;

import java.util.concurrent.CompletableFuture;

/**
 * @author VISTALL
 * @since 2018-04-24
 */
@Deprecated
public final class AccessRule {
    private static final Logger LOG = Logger.getInstance(AccessRule.class);

    public static <E extends Throwable> void read(@RequiredReadAction ThrowableRunnable<E> action) throws E {
        ReadAction.run(action);
    }

    public static @Nullable <T, E extends Throwable> T read(@RequiredReadAction ThrowableSupplier<T, E> action) throws E {
        return ReadAction.compute(action);
    }

    @ReviewAfterIssueFix(value = "github.com/uber/NullAway/issues/1504", todo = "Remove explicit casts")
    public static CompletableFuture<@Nullable Void> writeAsync(@RequiredWriteAction ThrowableRunnable<Throwable> action) {
        return writeAsync((ThrowableSupplier<@Nullable Void, Throwable>) () -> {
            action.run();
            return null;
        });
    }

    public static <T extends @Nullable Object> CompletableFuture<T> writeAsync(@RequiredWriteAction ThrowableSupplier<T, Throwable> action) {
        CompletableFuture<T> result = new CompletableFuture<>();
        AppUIExecutor.onWriteThread().later().execute(() -> {
            try {
                result.complete(action.get());
            }
            catch (Throwable throwable) {
                LOG.error(throwable);
                result.completeExceptionally(throwable);
            }
        });
        return result;
    }
}
