/*
 * Copyright 2000-2016 JetBrains s.r.o.
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
package consulo.dataContext;

import consulo.dataContext.internal.BuilderDataContext;
import consulo.util.dataholder.Key;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.util.HashMap;
import java.util.Map;

/**
 * Allows an action to retrieve information about the context in which it was invoked.
 *
 * @see AnActionEvent#getDataContext()
 * @see consulo.ide.impl.idea.openapi.actionSystem.PlatformDataKeys
 * @see Key
 * @see consulo.ide.impl.idea.ide.DataManager
 * @see DataProvider
 */
public interface DataContext {
    DataContext EMPTY_CONTEXT = new DataContext() {
        @Override
        public <T> T getData(@Nonnull Key<T> key) {
            return null;
        }
    };

    /**
     * Returns the value corresponding to the specified data key.
     *
     * @param key the data key for which the value is requested.
     * @return the value, or null if no value is available in the current context for this identifier.
     */
    @Nullable
    <T> T getData(@Nonnull Key<T> key);

    /**
     * Returns not null value corresponding to the specified data key.
     *
     * @param key the data key for which the value is requested.
     * @return not null value, or throws {@link AssertionError}.
     */
    @Nonnull
    default <T> T getRequiredData(@Nonnull Key<T> key) {
        T data = getData(key);
        assert data != null;
        return data;
    }

    /**
     * Checks if the data exists.
     *
     * @param key the data key for which the value is requested.
     * @return {@code true} if not null data exists, {@code false} otherwise.
     */
    default <T> boolean hasData(@Nonnull Key<T> key) {
        return getData(key) != null;
    }

    @Nonnull
    public static Builder builder() {
        return new Builder(null);
    }

    public final static class Builder {
        private DataContext myParent;
        private Map<Key, Object> myMap;

        Builder(DataContext parent) {
            myParent = parent;
        }

        public Builder parent(@Nullable DataContext parent) {
            myParent = parent;
            return this;
        }

        @Nonnull
        public <T> Builder add(@Nonnull Key<? super T> dataKey, @Nullable T value) {
            if (value != null) {
                if (myMap == null) {
                    myMap = new HashMap<>();
                }
                myMap.put(dataKey, value);
            }
            return this;
        }

        @Nonnull
        public Builder addAll(@Nonnull DataContext dataContext, @Nonnull Key<?>... keys) {
            for (Key<?> key : keys) {
                //noinspection unchecked
                add((Key<Object>) key, dataContext.getData(key));
            }
            return this;
        }

        @Nonnull
        public DataContext build() {
            if (myMap == null && myParent == null) {
                return EMPTY_CONTEXT;
            }
            return new BuilderDataContext(myMap != null ? myMap : Map.of(), myParent);
        }
    }
}
