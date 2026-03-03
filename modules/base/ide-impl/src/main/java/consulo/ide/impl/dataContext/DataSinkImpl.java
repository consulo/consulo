/*
 * Copyright 2013-2025 consulo.io
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
package consulo.ide.impl.dataContext;

import consulo.application.Application;
import consulo.dataContext.DataSink;
import consulo.dataContext.DataSnapshot;
import consulo.dataContext.UiDataProvider;
import consulo.dataContext.UiDataRule;
import consulo.util.dataholder.Key;
import consulo.util.lang.ref.SimpleReference;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * Internal implementation of {@link DataSink} and {@link DataSnapshot}.
 * <p>
 * Collects immediate data via {@link #set}, lazy suppliers via {@link #lazy},
 * and lazy functions via {@link #lazyValue}. Resolves lazy data under
 * {@code tryRunReadAction} when {@link #resolve} is called.
 */
public class DataSinkImpl implements DataSink, DataSnapshot {
    private final Map<Key, Object> myImmediateData = new HashMap<>();
    private final Map<Key, Supplier<?>> myLazyData = new HashMap<>();
    private final Map<Key, Function<DataSnapshot, ?>> myLazyValueData = new HashMap<>();

    private boolean mySnapshotCollected;

    @Override
    public <T> void set(@Nonnull Key<T> key, @Nullable T data) {
        if (data != null) {
            myImmediateData.putIfAbsent(key, data);
        }
    }

    @Override
    public <T> void lazy(@Nonnull Key<T> key, @Nonnull Supplier<T> dataSupplier) {
        if (!myImmediateData.containsKey(key) && !myLazyData.containsKey(key)) {
            myLazyData.put(key, dataSupplier);
        }
    }

    @Override
    public <T> void lazyValue(@Nonnull Key<T> key, @Nonnull Function<DataSnapshot, T> dataFunction) {
        if (!myImmediateData.containsKey(key) && !myLazyValueData.containsKey(key)) {
            myLazyValueData.put(key, dataFunction);
        }
    }

    @Override
    public void uiDataSnapshot(@Nonnull UiDataProvider provider) {
        provider.uiDataSnapshot(this);
    }

    /**
     * Collects data from the provider and applies all registered {@link UiDataRule}s.
     */
    public void collectFromProvider(@Nonnull UiDataProvider provider, @Nonnull List<UiDataRule> rules) {
        if (!mySnapshotCollected) {
            provider.uiDataSnapshot(this);
            for (UiDataRule rule : rules) {
                rule.uiDataSnapshot(this, this);
            }
            mySnapshotCollected = true;
        }
    }

    /**
     * Resolves data for the given key.
     * <ol>
     *   <li>Check immediate data — return if found</li>
     *   <li>Check lazy supplier — execute under {@code tryRunReadAction} — return if found</li>
     *   <li>Check lazyValue function — execute under {@code tryRunReadAction} with this as snapshot — return if found</li>
     *   <li>Return null</li>
     * </ol>
     */
    @Nullable
    @SuppressWarnings("unchecked")
    public <T> T resolve(@Nonnull Key<T> key) {
        // 1. Immediate data
        Object immediate = myImmediateData.get(key);
        if (immediate != null) {
            return (T) immediate;
        }

        // 2. Lazy supplier
        Supplier<?> supplier = myLazyData.get(key);
        if (supplier != null) {
            T result = resolveUnderReadAction(() -> (T) supplier.get());
            if (result != null) {
                return result;
            }
        }

        // 3. Lazy value function
        Function<DataSnapshot, ?> function = myLazyValueData.get(key);
        if (function != null) {
            T result = resolveUnderReadAction(() -> (T) function.apply(this));
            if (result != null) {
                return result;
            }
        }

        return null;
    }

    @Override
    @Nullable
    public <T> T get(@Nonnull Key<T> key) {
        // DataSnapshot only returns immediate (non-lazy) data
        @SuppressWarnings("unchecked")
        T result = (T) myImmediateData.get(key);
        return result;
    }

    @Nullable
    private <T> T resolveUnderReadAction(@Nonnull Supplier<T> computation) {
        SimpleReference<T> result = SimpleReference.create();
        Application.get().tryRunReadAction(() -> result.set(computation.get()));
        return result.get();
    }
}
