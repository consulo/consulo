/*
 * Copyright 2000-2009 JetBrains s.r.o.
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

import consulo.util.dataholder.Key;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.util.function.Function;
import java.util.function.Supplier;

/**
 * A sink for collecting data from {@link UiDataProvider} implementations.
 * <p>
 * Supports three modes of data provision:
 * <ul>
 *   <li>{@link #set} — immediate data, available on EDT (editors, components, selections)</li>
 *   <li>{@link #lazy} — deferred data, resolved later under read access (PSI files, PSI elements)</li>
 *   <li>{@link #lazyValue} — deferred data that needs access to already-collected snapshot data</li>
 * </ul>
 *
 * @author yole
 * @since 2006-10-23
 */
public interface DataSink {
    /**
     * Sets an immediate (non-lazy) data value.
     * Use for data that is readily available on EDT without read access.
     */
    <T> void set(@Nonnull Key<T> key, @Nullable T data);

    /**
     * Registers a lazy data supplier that will be resolved later under read access.
     * Use for data requiring PSI or other read-action-protected resources.
     *
     * @param key          the data key
     * @param dataSupplier supplier invoked later under {@code tryRunReadAction}
     */
    <T> void lazy(@Nonnull Key<T> key, @Nonnull Supplier<T> dataSupplier);

    /**
     * Registers a lazy data function that receives a {@link DataSnapshot}
     * of already-collected immediate data.
     * Use when the deferred computation needs access to other collected data.
     *
     * @param key          the data key
     * @param dataFunction function invoked later under {@code tryRunReadAction},
     *                     receiving a snapshot of immediate data
     */
    <T> void lazyValue(@Nonnull Key<T> key, @Nonnull Function<DataSnapshot, T> dataFunction);

    /**
     * Delegates to another {@link UiDataProvider} to populate this sink.
     * Useful for composing data from child components or delegates.
     *
     * @param provider the provider to collect data from
     */
    void uiDataSnapshot(@Nonnull UiDataProvider provider);
}
