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
import consulo.component.extension.ExtensionPoint;
import consulo.dataContext.DataSink;
import consulo.dataContext.DataSnapshot;
import consulo.dataContext.UiDataProvider;
import consulo.dataContext.UiDataRule;
import consulo.util.dataholder.Key;
import consulo.util.lang.ref.SimpleReference;
import org.jspecify.annotations.Nullable;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
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
    // Cycle guard is per-thread: the same sink is shared by the async context and resolved
    // concurrently by many background action-update threads. A shared set would make one
    // thread's in-progress resolution look like a cycle to another thread, yielding spurious nulls.
    private final ThreadLocal<Set<Key>> myResolving = ThreadLocal.withInitial(HashSet::new);

    private boolean mySnapshotCollected;

    private final Application myApplication;

    public DataSinkImpl(Application application) {
        myApplication = application;
    }

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

    /**
     * Collects data from the provider and applies all registered {@link UiDataRule}s.
     */
    public void collectFromProvider(UiDataProvider provider, Iterable<UiDataRule> rules) {
        if (!mySnapshotCollected) {
            provider.uiDataSnapshot(this);
            rules.forEach(rule -> rule.uiDataSnapshot(this, this));
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
     * A {@link UiDataRule} computing a derived value (e.g. {@code VIRTUAL_FILE} from
     * {@code PSI_ELEMENT}) reads dependencies back through {@link #get}, which must therefore
     * resolve lazy data too — otherwise lazily-provided keys would always read as {@code null}.
     */
    @SuppressWarnings("unchecked")
    public <T> @Nullable T resolve(Key<T> key) {
        // 1. Immediate data
        Object immediate = myImmediateData.get(key);
        if (immediate != null) {
            return (T) immediate;
        }

        // Guard against cyclic lazy dependencies (e.g. a rule for A reading B whose rule reads A)
        Set<Key> resolving = myResolving.get();
        if (!resolving.add(key)) {
            return null;
        }
        try {
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
        finally {
            resolving.remove(key);
        }
    }

    @Override
    public @Nullable <T> T get(Key<T> key) {
        return resolve(key);
    }

    private @Nullable <T> T resolveUnderReadAction(Supplier<T> computation) {
        if (myApplication.isReadAccessAllowed()) {
            return computation.get();
        }

        SimpleReference<T> result = SimpleReference.create();
        myApplication.tryRunReadAction(() -> result.set(computation.get()));
        return result.get();
    }
}
