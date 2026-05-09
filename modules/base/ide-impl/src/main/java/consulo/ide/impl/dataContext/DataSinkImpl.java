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
import org.jspecify.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * Internal implementation of {@link DataSink} and {@link DataSnapshot}.
 */
public class DataSinkImpl implements DataSink, DataSnapshot {
    private final Map<Key, Object> myImmediateData = new HashMap<>();
    private final Map<Key, Supplier<?>> myLazyData = new HashMap<>();
    private final Map<Key, Function<DataSnapshot, ?>> myLazyValueData = new HashMap<>();

    private boolean mySnapshotCollected;

    @Override
    public <T> void set(Key<T> key, @Nullable T data) {
        if (data != null) {
            myImmediateData.putIfAbsent(key, data);
        }
    }

    @Override
    public <T> void lazy(Key<T> key, Supplier<T> dataSupplier) {
        if (!myImmediateData.containsKey(key) && !myLazyData.containsKey(key)) {
            myLazyData.put(key, dataSupplier);
        }
    }

    @Override
    public <T> void lazyValue(Key<T> key, Function<DataSnapshot, T> dataFunction) {
        if (!myImmediateData.containsKey(key) && !myLazyValueData.containsKey(key)) {
            myLazyValueData.put(key, dataFunction);
        }
    }

    @Override
    public void uiDataSnapshot(UiDataProvider provider) {
        provider.uiDataSnapshot(this);
    }

    public void collectFromProvider(UiDataProvider provider, Iterable<UiDataRule> rules) {
        if (!mySnapshotCollected) {
            provider.uiDataSnapshot(this);
            rules.forEach(rule -> rule.uiDataSnapshot(this, this));
            mySnapshotCollected = true;
        }
    }

    @SuppressWarnings("unchecked")
    public <T> @Nullable T resolve(Key<T> key) {
        Object immediate = myImmediateData.get(key);
        if (immediate != null) {
            return (T) immediate;
        }

        Supplier<?> supplier = myLazyData.get(key);
        if (supplier != null) {
            T result = resolveUnderReadAction(() -> (T) supplier.get());
            if (result != null) {
                return result;
            }
        }

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
    public @Nullable <T> T get(Key<T> key) {
        @SuppressWarnings("unchecked")
        T result = (T) myImmediateData.get(key);
        return result;
    }

    private @Nullable <T> T resolveUnderReadAction(Supplier<T> computation) {
        SimpleReference<T> result = SimpleReference.create();
        Application.get().tryRunReadAction(() -> result.set(computation.get()));
        return result.get();
    }
}
