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
import consulo.dataContext.AsyncDataContext;
import consulo.dataContext.DataProvider;
import consulo.dataContext.UiDataProvider;
import consulo.dataContext.UiDataRule;
import consulo.logging.Logger;
import consulo.util.dataholder.Key;
import consulo.util.dataholder.UserDataHolder;
import org.jspecify.annotations.Nullable;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * A pre-cached async data context that captures data providers on EDT
 * and allows safe data resolution from background threads.
 * <p>
 * For {@link UiDataProvider} components, the snapshot is pre-collected on EDT
 * via {@link DataSinkImpl}, and lazy values are resolved under {@code tryRunReadAction}.
 * <p>
 * For legacy {@link DataProvider} components,
 * providers are captured on EDT and wrapped for background-safe access.
 */
public class PreCachedDataContext implements AsyncDataContext, UserDataHolder {
    private static final Logger LOG = Logger.getInstance(PreCachedDataContext.class);

    private final BaseDataManager myDataManager;
    private final List<DataProvider> myProviders;
    private Map<Key, Object> myUserData;

    /**
     * Creates a pre-cached async data context from a list of providers
     * already resolved on EDT.
     *
     * @param dataManager the data manager for rule resolution
     * @param providers   providers in hierarchy order (child-first)
     */
    public PreCachedDataContext(BaseDataManager dataManager, List<DataProvider> providers) {
        myDataManager = dataManager;
        myProviders = providers;
    }

    @Override
    public <T> @Nullable T getData(Key<T> dataId) {
        for (DataProvider provider : myProviders) {
            T data = myDataManager.getDataFromProvider(provider, dataId, null);
            if (data != null) {
                return data;
            }
        }
        return null;
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
     * Initializes a data provider for async (background) use.
     * <p>
     * For {@link UiDataProvider}: pre-collects snapshot via {@link DataSinkImpl}
     * on EDT, returns a provider that resolves from the pre-collected sink.
     * <p>
     * For other providers: wraps with slow-access logging.
     *
     * @param provider the original provider from the component
     * @return a background-safe data provider
     */
    public static DataProvider initProviderForAsync(DataProvider provider) {
        // UiDataProviderAdapter already handles UiDataProvider via DataSinkImpl,
        // but for async context we want to pre-collect the sink on EDT
        if (provider instanceof UiDataProviderAdapter uiAdapter) {
            DataSinkImpl sink = new DataSinkImpl();
            List<UiDataRule> rules = Application.get().getExtensionPoint(UiDataRule.class).getExtensionList();
            sink.collectFromProvider(uiAdapter.getProvider(), rules);
            return sink::resolve;
        }

        return new DataProvider() {
            @Override
            public @Nullable Object getData(Key<?> dataKey) {
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
            }
        };
    }
}
