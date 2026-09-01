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

import consulo.application.AccessToken;
import consulo.application.Application;
import consulo.application.internal.SlowOperations;
import consulo.dataContext.AsyncDataContext;
import consulo.dataContext.DataProvider;
import consulo.dataContext.DataSink;
import consulo.dataContext.UiDataProvider;
import consulo.dataContext.UiDataRule;
import consulo.language.editor.PlatformDataKeys;
import consulo.logging.Logger;
import consulo.ui.Component;
import consulo.util.dataholder.Key;
import consulo.util.dataholder.UserDataHolder;
import org.jspecify.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;

/**
 * A pre-cached async data context that captures the answers of a component hierarchy on EDT and allows safe
 * data resolution from background threads.
 * <p>
 * The hierarchy is captured into one snapshot rather than kept as a chain to walk per key: every component is
 * collected from the focused one upwards, so the nearest one to answer for a key wins, and the
 * {@link UiDataRule}s run once over the result. A rule therefore reads the hierarchy the way an action does,
 * and a component which {@link DataSink#setNull stands for a key itself} hides it from the
 * components above it together with everything the rules would have derived from it.
 */
public class PreCachedDataContext implements AsyncDataContext, UserDataHolder {
    private static final Logger LOG = Logger.getInstance(PreCachedDataContext.class);

    private static boolean ourIsCapturingSnapshot;

    private final DataSinkImpl mySnapshot;
    private final @Nullable Component myComponent;
    private Map<Key, Object> myUserData;

    public PreCachedDataContext(DataSinkImpl snapshot) {
        this(snapshot, null);
    }

    public PreCachedDataContext(
        DataSinkImpl snapshot,
        @Nullable Component component
    ) {
        mySnapshot = snapshot;
        myComponent = component;
    }

    /**
     * Captures a component hierarchy to answer from right away. Every walk of the platform builds its snapshot
     * through this, so the order of the components - and what one of them hides from the ones above - is the
     * only thing a frontend has to describe.
     */
    public static Capture capture(Application application) {
        return new Capture(application, false);
    }

    /**
     * Captures a component hierarchy to be read later from a background thread. The providers are held to the
     * rule that they answer without slow work, since the answers are taken here and not when they are read.
     */
    public static Capture captureForAsync(Application application) {
        return new Capture(application, true);
    }

    public static class Capture {
        private final Application myApplication;
        private final DataSinkImpl mySink;
        private final boolean myForAsync;

        private Capture(Application application, boolean forAsync) {
            myApplication = application;
            mySink = new DataSinkImpl(application);
            myForAsync = forAsync;
        }

        /**
         * This component of the hierarchy stands for the key itself - nothing above it answers for it.
         */
        public Capture hide(Key<?> key) {
            mySink.setNull(key);
            return this;
        }

        public Capture collect(@Nullable DataProvider provider) {
            if (provider == null) {
                return this;
            }

            if (!myForAsync) {
                mySink.collectFrom(logSlow(provider));
                return this;
            }

            ourIsCapturingSnapshot = true;
            try (AccessToken ignore = SlowOperations.startSection(SlowOperations.FORCE_ASSERT)) {
                mySink.collectFrom(logSlow(provider));
            }
            finally {
                ourIsCapturingSnapshot = false;
            }
            return this;
        }

        /**
         * Closes the snapshot by running the rules over everything the hierarchy answered.
         */
        public DataSinkImpl snapshot() {
            mySink.applyRules(myApplication.getExtensionPoint(UiDataRule.class));
            return mySink;
        }

        /**
         * Closes the snapshot and answers one key from it, which is what a context built per key needs.
         */
        public <T> @Nullable T resolve(Key<T> key) {
            DataSinkImpl snapshot = snapshot();
            T data = snapshot.resolve(key);
            return data == null ? null : BaseDataManager.validated(data, key, snapshot);
        }

        public PreCachedDataContext build(BaseDataManager dataManager) {
            return build(dataManager, null);
        }

        public PreCachedDataContext build(BaseDataManager dataManager, @Nullable Component component) {
            return new PreCachedDataContext(snapshot(), component);
        }
    }

    @Override
    public <T> @Nullable T getData(Key<T> dataId) {
        // the flag is only ever set on the UI thread, so a background reader must not consult it
        if (ourIsCapturingSnapshot && Application.get().isDispatchThread()) {
            LOG.error("DataContext must not be queried during another DataContext creation");
        }

        // the component the context was built from is the last word on what it is, so a provider which names a
        // component of its own - the tree inside the panel the provider hangs off - is answered first
        if (myComponent != null && (Component.KEY == dataId || PlatformDataKeys.CONTEXT_UI_COMPONENT == dataId)) {
            //noinspection unchecked
            return (T) myComponent;
        }

        T data = mySnapshot.resolve(dataId);
        return data == null ? null : BaseDataManager.validated(data, dataId, mySnapshot);
    }

    @Override
    public <T> T getUserData(Key<T> key) {
        Map<Key, Object> map = myUserData;
        if (map == null) {
            return null;
        }
        //noinspection unchecked
        return (T) map.get(key);
    }

    @Override
    public <T> void putUserData(Key<T> key, @Nullable T value) {
        if (myUserData == null) {
            myUserData = new HashMap<>();
        }
        myUserData.put(key, value);
    }

    /**
     * A provider which has not been migrated to {@link UiDataProvider} is asked per key
     * rather than for a snapshot, and is the one which can be slow. Saying so is all this does.
     */
    private static DataProvider logSlow(DataProvider provider) {
        if (provider instanceof UiDataProviderAdapter) {
            return provider;
        }

        return dataKey -> {
            long start = System.currentTimeMillis();
            try {
                return provider.getData(dataKey);
            }
            finally {
                long elapsed = System.currentTimeMillis() - start;
                if (elapsed > 100) {
                    LOG.warn("Slow data provider " + provider + " took " + elapsed + "ms on " + dataKey +
                        ". Consider implementing UiDataProvider.");
                }
            }
        };
    }
}
