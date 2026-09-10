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
import consulo.dataContext.DataProvider;
import consulo.dataContext.DataSink;
import consulo.dataContext.DataSnapshot;
import consulo.dataContext.UiDataProvider;
import consulo.dataContext.UiDataRule;
import consulo.util.dataholder.Key;
import consulo.util.lang.ObjectUtil;
import consulo.util.lang.ref.SimpleReference;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
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
 * <p>
 * One sink carries a whole component hierarchy: {@link #collectFrom} is called for the providers of the
 * hierarchy from the focused component upwards, so the nearest component to answer for a key wins, and
 * {@link #applyRules} runs once over what they answered together. A {@link UiDataRule} therefore reads the
 * hierarchy as one snapshot rather than one component at a time - which is what lets a component
 * {@link #setNull hide} a key from the components above it: everything derived from that key is derived
 * after the hiding, and sees nothing.
 */
public class DataSinkImpl implements DataSink {
    private static final Logger LOG = Logger.getInstance(DataSinkImpl.class);

    /**
     * A component answered for the key by standing for it itself - nothing above it in the hierarchy answers.
     */
    public static final Object EXPLICIT_NULL = ObjectUtil.sentinel("DataSinkImpl.EXPLICIT_NULL");

    private final Map<Key, Object> myImmediateData = new HashMap<>();
    private final Map<Key, Object> myComputedData = new HashMap<>();
    private final Map<Key, Supplier<?>> myLazyData = new HashMap<>();
    private final Map<Key, Function<DataSnapshot, ?>> myLazyValueData = new HashMap<>();
    // Cycle guard is per-thread: the same sink is shared by the async context and resolved
    // concurrently by many background action-update threads. A shared set would make one
    // thread's in-progress resolution look like a cycle to another thread, yielding spurious nulls.
    private final ThreadLocal<Set<Key>> myResolving = ThreadLocal.withInitial(HashSet::new);

    /**
     * Providers of the hierarchy which are not {@link UiDataProvider}s, in the order they were met. They cannot
     * be asked for what they hold without a key, so they are kept and asked one by one, after the snapshot.
     */
    private final List<DataProvider> myLegacyProviders = new ArrayList<>();

    private boolean myRulesApplied;
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
            Object data = myImmediateData.get(key);
            return data == EXPLICIT_NULL ? null : (T) data;
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
    public <T> void setNull(Key<T> key) {
        myImmediateData.putIfAbsent(key, EXPLICIT_NULL);
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
     * Collects what one component of the hierarchy answers. Called from the focused component upwards, so a
     * key already answered for is kept and the nearest component wins.
     */
    public void collectFrom(DataProvider provider) {
        if (provider instanceof UiDataProviderAdapter adapter) {
            adapter.getProvider().uiDataSnapshot(this);
        }
        else if (provider instanceof UiDataProvider uiProvider) {
            uiProvider.uiDataSnapshot(this);
        }
        else {
            myLegacyProviders.add(provider);
        }
    }

    /**
     * Runs the rules once over everything the hierarchy answered. Collecting more afterwards is pointless -
     * the rules would not see it - so it is only done once per sink.
     */
    public void applyRules(Iterable<UiDataRule> rules) {
        if (myRulesApplied) {
            return;
        }
        myRulesApplied = true;

        myCollectingRules = true;
        try {
            rules.forEach(rule -> rule.uiDataSnapshot(this, myImmediateSnapshot));
        }
        finally {
            myCollectingRules = false;
        }
    }

    /**
     * Collects data from the provider and applies all registered {@link UiDataRule}s.
     */
    public void collectFromProvider(UiDataProvider provider, Iterable<UiDataRule> rules) {
        provider.uiDataSnapshot(this);
        applyRules(rules);
    }

    /**
     * Resolves data for the given key.
     * <ol>
     *   <li>Check immediate data — return if found, and answer nothing at all if a component stood for the key</li>
     *   <li>Ask the legacy providers of the hierarchy, nearest first — return if one answers</li>
     *   <li>Check lazy supplier — execute under {@code tryRunReadAction} — return if found</li>
     *   <li>Check lazyValue function — execute under {@code tryRunReadAction} with the resolving snapshot — return if found</li>
     *   <li>Return null</li>
     * </ol>
     * A lazy supplier may perform slow PSI or VFS work, so on the UI thread the lazy steps are skipped
     * while an action updates itself there, or when {@code actionSystem.update.actions.suppress.dataRules.on.edt}
     * is on. Immediate data stays available in both cases.
     */
    @SuppressWarnings("unchecked")
    public <T> @Nullable T resolve(Key<T> key) {
        // 1. Immediate data — the UI snapshot wins over what rules computed from it
        Object immediate = myImmediateData.get(key);
        if (immediate == EXPLICIT_NULL) {
            return null;
        }
        if (immediate == null) {
            immediate = myComputedData.get(key);
        }
        if (immediate != null) {
            return (T) immediate;
        }

        // 2. Whatever of the hierarchy has not been migrated off the old provider
        for (DataProvider provider : myLegacyProviders) {
            T data = provider.getDataUnchecked(key);
            if (data != null) {
                return data;
            }
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
