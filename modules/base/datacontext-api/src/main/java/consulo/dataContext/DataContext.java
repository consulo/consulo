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

import org.jspecify.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;

/**
 * Allows an action to retrieve information about the context in which it was invoked.
 *
 * @see Key
 * @see DataManager
 * @see DataProvider
 */
public interface DataContext {
    DataContext EMPTY_CONTEXT = new DataContext() {
        @Override
        public <T> @Nullable T getData(Key<T> key) {
            return null;
        }
    };

    /**
     * Returns the value corresponding to the specified data key.
     *
     * @param key the data key for which the value is requested.
     * @return the value, or null if no value is available in the current context for this identifier.
     */
    <T> @Nullable T getData(Key<T> key);

    /**
     * Returns not null value corresponding to the specified data key.
     *
     * @param key the data key for which the value is requested.
     * @return not null value, or throws {@link IllegalArgumentException}.
     */
    default <T> T getRequiredData(Key<T> key) {
        T data = getData(key);
        if (data == null) {
            throw new IllegalArgumentException("There no data for key: " + key);
        }
        return data;
    }

    /**
     * Checks if the data exists.
     *
     * @param key the data key for which the value is requested.
     * @return {@code true} if not null data exists, {@code false} otherwise.
     */
    default <T> boolean hasData(Key<T> key) {
        return getData(key) != null;
    }

    
    public static Builder builder() {
        return new Builder(null);
    }

    public final static class Builder {
        private @Nullable DataContext myParent = null;
        private @Nullable Map<Key, Object> myMap = null;

        Builder(@Nullable DataContext parent) {
            myParent = parent;
        }

        public Builder parent(@Nullable DataContext parent) {
            myParent = parent;
            return this;
        }

        public <T> Builder add(Key<? super T> dataKey, @Nullable T value) {
            if (value != null) {
                if (myMap == null) {
                    myMap = new HashMap<>();
                }
                myMap.put(dataKey, value);
            }
            return this;
        }

        public Builder addAll(DataContext dataContext, Key<?>... keys) {
            for (Key<?> key : keys) {
                //noinspection unchecked
                add((Key<Object>) key, dataContext.getData(key));
            }
            return this;
        }

        public DataContext build() {
            if (myMap == null && myParent == null) {
                return EMPTY_CONTEXT;
            }
            return new BuilderDataContext(myMap != null ? myMap : Map.of(), myParent);
        }
    }
}
