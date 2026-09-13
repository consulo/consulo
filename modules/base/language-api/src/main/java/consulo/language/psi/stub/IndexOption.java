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
package consulo.language.psi.stub;

import consulo.index.io.data.DataExternalizer;
import consulo.language.internal.psi.stub.IndexOptionImpl;
import consulo.localize.LocalizeValue;

/**
 * Describes how a file's indexed output is keyed in the cache under module-aware indexing.
 * Returned by {@link ModuleAwareIndexOptionProvider#getOptions}.
 *
 * Three tiers:
 * <ul>
 *   <li>{@link #fullySharable()} — output depends only on file content; shared across all projects.</li>
 *   <li>{@link #sharablePerOption} — output depends on a serialisable payload; shared across any
 *       project/module with matching payload hash.</li>
 *   <li>{@link #uniqueToModule} — output tied to module identity; not shared across modules.</li>
 * </ul>
 *
 * See {@code MODULE_AWARE_INDEX.md} for the full design.
 */
public sealed interface IndexOption permits IndexOptionImpl {

    static IndexOption fullySharable() {
        return IndexOptionImpl.FullySharable.INSTANCE;
    }

    static IndexOption uniqueToModule(LocalizeValue displayName) {
        return new IndexOptionImpl.UniqueToModule(displayName);
    }

    static <T extends Record> IndexOption sharablePerOption(T value,
                                                            DataExternalizer<T> externalizer,
                                                            LocalizeValue displayName) {
        return new IndexOptionImpl.SharablePerOption<>(value, externalizer, displayName);
    }
}
