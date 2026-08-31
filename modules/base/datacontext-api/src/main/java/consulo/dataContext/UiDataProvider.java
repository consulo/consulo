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
package consulo.dataContext;

import consulo.util.dataholder.Key;

/**
 * A data provider that populates a {@link DataSink} with data.
 * <p>
 * Unlike {@link DataProvider} which returns data synchronously for a given key,
 * {@code UiDataProvider} populates a sink with all available data at once,
 * supporting both immediate values ({@link DataSink#set}) and deferred values
 * ({@link DataSink#lazy}) that are computed later with read access.
 * <p>
 * This pattern allows PSI-requiring data to be safely deferred from EDT,
 * since lazy suppliers are resolved under {@code tryRunReadAction}.
 * <p>
 * Components can implement this interface instead of (or in addition to) {@link DataProvider}.
 * When both are implemented, {@code UiDataProvider} takes priority.
 */
public interface UiDataProvider {
    Key<UiDataProvider> KEY = Key.of(UiDataProvider.class);

    /**
     * Called on EDT to populate the data sink.
     * <p>
     * Use {@link DataSink#set} for immediate UI data (editors, components, selections).
     * Use {@link DataSink#lazy} for data requiring read access (PSI files, PSI elements).
     *
     * @param sink the sink to populate with data
     */
    void uiDataSnapshot(DataSink sink);
}
