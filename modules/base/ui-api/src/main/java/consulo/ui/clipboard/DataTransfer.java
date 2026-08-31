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
package consulo.ui.clipboard;

import org.jspecify.annotations.Nullable;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;

/**
 * An immutable clipboard payload holding every representation of the same content.
 *
 * @author VISTALL
 * @since 2026-08-07
 */
public final class DataTransfer {
    public static final DataTransfer EMPTY = new DataTransfer(Map.of());

    public static DataTransfer of(String text) {
        return builder().put(DataTransferType.TEXT, text).build();
    }

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {
        private final Map<DataTransferType<?>, Object> myValues = new LinkedHashMap<>();

        private Builder() {
        }

        public <T> Builder put(DataTransferType<T> type, @Nullable T value) {
            if (value != null) {
                myValues.put(type, value);
            }
            return this;
        }

        public Builder putAll(DataTransfer transfer) {
            myValues.putAll(transfer.myValues);
            return this;
        }

        public DataTransfer build() {
            return myValues.isEmpty() ? EMPTY : new DataTransfer(myValues);
        }
    }

    private final Map<DataTransferType<?>, Object> myValues;

    private DataTransfer(Map<DataTransferType<?>, Object> values) {
        myValues = Collections.unmodifiableMap(new LinkedHashMap<>(values));
    }

    public Set<DataTransferType<?>> getTypes() {
        return myValues.keySet();
    }

    public boolean contains(DataTransferType<?> type) {
        return myValues.containsKey(type);
    }

    @SuppressWarnings("unchecked")
    public <T> @Nullable T get(DataTransferType<T> type) {
        return (T)myValues.get(type);
    }

    public boolean isEmpty() {
        return myValues.isEmpty();
    }

    public DataTransfer filter(Predicate<DataTransferType<?>> filter) {
        Map<DataTransferType<?>, Object> filtered = new LinkedHashMap<>();
        for (Map.Entry<DataTransferType<?>, Object> entry : myValues.entrySet()) {
            if (filter.test(entry.getKey())) {
                filtered.put(entry.getKey(), entry.getValue());
            }
        }
        return filtered.isEmpty() ? EMPTY : new DataTransfer(filtered);
    }

    @Override
    public String toString() {
        return "DataTransfer" + myValues.keySet();
    }
}
