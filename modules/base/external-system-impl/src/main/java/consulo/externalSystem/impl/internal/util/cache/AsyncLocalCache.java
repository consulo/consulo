// Copyright 2000-2023 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.externalSystem.impl.internal.util.cache;

import consulo.util.lang.ExceptionUtil;
import org.jspecify.annotations.Nullable;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.function.Supplier;

public final class AsyncLocalCache<T> {

    private final BasicLocalCache<CompletableFuture<T>> myValueDeferredCache = new BasicLocalCache<>();

    private final BasicLocalCache<T> myValueCache = new BasicLocalCache<>();

    public @Nullable T getValue() {
        return myValueCache.getValue();
    }

    public T getOrCreateValueBlocking(long stamp, Supplier<T> createValue) {
        CompletableFuture<T> deferred = new CompletableFuture<>();
        CompletableFuture<T> valueDeferred = myValueDeferredCache.getOrCreateValue(stamp, () -> deferred);
        if (valueDeferred == deferred) {
            try {
                deferred.complete(createValue.get());
            }
            catch (Throwable e) {
                deferred.completeExceptionally(e);
            }
        }
        T value = awaitValue(valueDeferred);
        return myValueCache.getOrCreateValue(stamp, () -> value);
    }

    private static <T> T awaitValue(CompletableFuture<T> valueDeferred) {
        try {
            return valueDeferred.join();
        }
        catch (CompletionException e) {
            ExceptionUtil.rethrowUnchecked(e.getCause());
            throw e;
        }
    }
}
