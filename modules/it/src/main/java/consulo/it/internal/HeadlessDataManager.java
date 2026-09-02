/*
 * Copyright 2013-2026 consulo.io
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
package consulo.it.internal;

import consulo.annotation.component.ComponentProfiles;
import consulo.annotation.component.ServiceImpl;
import consulo.dataContext.AsyncDataContext;
import consulo.dataContext.DataContext;
import consulo.dataContext.DataManager;
import consulo.util.concurrent.AsyncResult;
import consulo.util.concurrent.Promise;
import consulo.util.concurrent.Promises;
import consulo.util.dataholder.Key;
import consulo.util.dataholder.UserDataHolder;
import jakarta.inject.Singleton;
import org.jspecify.annotations.Nullable;

/**
 * No focused component exists headlessly, so every context resolves empty; save/load keep the
 * production {@link UserDataHolder} semantics so context-scoped state still round-trips.
 *
 * @author VISTALL
 */
@ServiceImpl(profiles = ComponentProfiles.INTEGRATION_TEST)
@Singleton
public class HeadlessDataManager implements DataManager {
    @Override
    public DataContext getDataContext() {
        return DataContext.EMPTY_CONTEXT;
    }

    @Override
    public AsyncDataContext createAsyncDataContext(DataContext dataContext) {
        return new AsyncDataContext() {
            @Override
            public <T> @Nullable T getData(Key<T> key) {
                return dataContext.getData(key);
            }
        };
    }

    @Override
    public Promise<DataContext> getDataContextFromFocusAsync() {
        return Promises.resolvedPromise(DataContext.EMPTY_CONTEXT);
    }

    @Override
    public AsyncResult<DataContext> getDataContextFromFocus() {
        return AsyncResult.resolved(DataContext.EMPTY_CONTEXT);
    }

    @Override
    public DataContext getDataContext(consulo.ui.@Nullable Component component) {
        return DataContext.EMPTY_CONTEXT;
    }

    @Override
    public DataContext getDataContext(java.awt.@Nullable Component component) {
        return DataContext.EMPTY_CONTEXT;
    }

    @Override
    public <T> void saveInDataContext(@Nullable DataContext dataContext, Key<T> dataKey, @Nullable T data) {
        if (dataContext instanceof UserDataHolder) {
            ((UserDataHolder) dataContext).putUserData(dataKey, data);
        }
    }

    @Override
    public <T> @Nullable T loadFromDataContext(DataContext dataContext, Key<T> dataKey) {
        return dataContext instanceof UserDataHolder ? ((UserDataHolder) dataContext).getUserData(dataKey) : null;
    }
}
