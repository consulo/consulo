// Copyright 2000-2023 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.externalSystem.impl.internal.util.cache;

import org.jspecify.annotations.Nullable;

import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;

public final class BasicLocalCache<T> {

    private final AtomicReference<@Nullable CachedValue<T>> myAtomic = new AtomicReference<>();

    public @Nullable T getValue() {
        CachedValue<T> cachedValue = myAtomic.get();
        return cachedValue != null ? cachedValue.value() : null;
    }

    public T getOrCreateValue(long stamp, Supplier<T> createValue) {
        CachedValue<T> cachedValue = Objects.requireNonNull(myAtomic.updateAndGet(it -> {
            if (it == null || it.stamp() < stamp) {
                return new CachedValue<>(stamp, createValue.get());
            }
            else {
                return it;
            }
        }));
        return cachedValue.value();
    }

    private record CachedValue<T>(long stamp, T value) {
    }
}
