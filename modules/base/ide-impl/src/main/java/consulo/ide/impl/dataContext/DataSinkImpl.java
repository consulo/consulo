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
import consulo.application.util.registry.Registry;
import consulo.component.extension.ExtensionPoint;
import consulo.logging.Logger;
import consulo.ui.ex.internal.ActionUpdateInvoker;
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
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * Internal implementation of {@link DataSink}.
 * <p>
 * Collects immediate data via {@link #set}, lazy suppliers via {@link #lazy},
 * and lazy functions via {@link #lazyValue}. Resolves lazy data under
 * {@code tryRunReadAction} when {@link #resolve} is called.
 */
public class DataSinkImpl implements DataSink {
    private static final Logger LOG = Logger.getInstance(DataSinkImpl.class);

    private final Map<Key, Object> myImmediateData = new HashMap<>();
    private final Map<Key, Object> myComputedData = new HashMap<>();
    private final Map<Key, Supplier<?>> myLazyData = new HashMap<>();
    private final Map<Key, Function<DataSnapshot, ?>> myLazyValueData = new HashMap<>();
    // Cycle guard is per-thread: the same sink is shared by the async context and resolved
    // concurrently by many background action-update threads. A shared set would make one
    // thread's in-progress resolution look like a cycle to another thread, yielding spurious nulls.
    private final ThreadLocal<Set<Key>> myResolving = ThreadLocal.withInitial(HashSet::new);

    private boolean mySnapshotCollected;
    private boolean myCollectingRules;

    private final Application myApplication;

    /**
     * Snapshot view handed to {@link UiDataRule}s while the snapshot is being collected on EDT.
     * It exposes immediate data only and never runs a lazy supplier, so rules cannot pull
     * slow data (PSI, VFS) onto EDT. Rules are therefore non-recursive: a value contributed
     * lazily by another rule reads as {@code null} here and is computed later in background
     * through {@link #resolve}.
     */
    private final DataSnapshot myImmediateSnapshot = new DataSnapshot() {
        @SuppressWarnings("unchecked")
        @Override
        public <T> @Nullable T get(Key<T> key) {
            return (T) myImmediateData.get(key);
        }
    };

    /**
     * Snapshot view handed to {@link #lazyValue} functions. Those run in background, so reading
     * a dependency may resolve other lazy data.
     */
    private final DataSnapshot myResolvingSnapshot = new DataSnapshot() {
        @Override
        public <T> @Nullable T get(Key<T> key) {
            return resolve(key);
        }
    };

    public DataSinkImpl(Application application) {
        myApplication = application;
    }

    @Override
    public <T> void set(Key<T> key, @Nullable T data) {
        if (data == null) {
            return;
        }
        if (myCollectingRules) {
            // rules must not override other rules or snapshot values
            if (myImmediateData.containsKey(key) || myComputedData.containsKey(key)) {
                return;
            }
            myComputedData.put(key, data);
            return;
        }
        myImmediateData.putIfAbsent(key, data);
    }

    @Override
    public <T> void lazy(Key<T> key, Supplier<T> dataSupplier) {
        if (!isKnown(key) && !myLazyData.containsKey(key)) {
            myLazyData.put(key, dataSupplier);
        }
    }

    @Override
    public <T> void lazyValue(Key<T> key, Function<DataSnapshot, T> dataFunction) {
        if (!isKnown(key) && !myLazyValueData.containsKey(key)) {
            myLazyValueData.put(key, dataFunction);
        }
    }

    private boolean isKnown(Key<?> key) {
        return myImmediateData.containsKey(key) || myComputedData.containsKey(key);
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
            myCollectingRules = true;
            try {
                rules.forEach(rule -> rule.uiDataSnapshot(this, myImmediateSnapshot));
            }
            finally {
                myCollectingRules = false;
            }
            mySnapshotCollected = true;
        }
    }

    /**
     * Resolves data for the given key.
     * <ol>
     *   <li>Check immediate data — return if found</li>
     *   <li>Check lazy supplier — execute under {@code tryRunReadAction} — return if found</li>
     *   <li>Check lazyValue function — execute under {@code tryRunReadAction} with the resolving snapshot — return if found</li>
     *   <li>Return null</li>
     * </ol>
     * A lazy supplier may perform slow PSI or VFS work, so on the UI thread steps 2 and 3 are skipped
     * while an action updates itself there, or when {@code actionSystem.update.actions.suppress.dataRules.on.edt}
     * is on. Immediate data stays available in both cases.
     */
    @SuppressWarnings("unchecked")
    public <T> @Nullable T resolve(Key<T> key) {
        // 1. Immediate data — the UI snapshot wins over what rules computed from it
        Object immediate = myImmediateData.get(key);
        if (immediate == null) {
            immediate = myComputedData.get(key);
        }
        if (immediate != null) {
            return (T) immediate;
        }

        if (myApplication.isDispatchThread() && !areLazyValuesAllowedOnUiThread()) {
            reportLazyValueRequestedOnUiThread(key);
            return null;
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
                T result = resolveUnderReadAction(() -> (T) function.apply(myResolvingSnapshot));
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

    private static boolean areLazyValuesAllowedOnUiThread() {
        return !ActionUpdateInvoker.isNoRulesInUiThreadSection()
            && !Registry.is("actionSystem.update.actions.suppress.dataRules.on.edt", false);
    }

    private static final Set<String> ourReportedUiThreadKeys = ConcurrentHashMap.newKeySet();

    /**
     * Reported once per key per session, the JB {@code PreCachedDataContext} shape — the skip itself
     * is the sanctioned behavior of the no-rules update section, so repeating the nudge on every
     * update pass is pure noise.
     */
    private void reportLazyValueRequestedOnUiThread(Key<?> key) {
        if (!Registry.is("actionSystem.update.actions.warn.dataRules.on.edt", true)) {
            return;
        }
        if (!myLazyData.containsKey(key) && !myLazyValueData.containsKey(key)) {
            return;
        }
        if (!ourReportedUiThreadKeys.add(key.toString())) {
            return;
        }
        Throwable throwable = new Throwable();
        myApplication.executeOnPooledThread(() -> LOG.warn(
            key + " is not available on UI thread. Code that depends on data rules and slow data providers "
                + "must be run in background. For example, an action must use ActionUpdateThread.BGT.", throwable));
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
