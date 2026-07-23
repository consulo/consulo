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
package consulo.language.internal.psi.stub;

import consulo.index.io.data.DataExternalizer;
import consulo.language.psi.stub.IndexOption;
import consulo.localize.LocalizeValue;

/**
 * Internal impl of {@link IndexOption}. Sealed to a fixed set of record variants, accessible
 * only from modules that have {@code consulo.language.internal.psi.stub} on qualified exports
 * (the index implementation module).
 *
 * <p>Plugins never reference these records directly — use the factory methods on
 * {@link IndexOption}. The storage layer in the index-impl module pattern-matches on the
 * sealed hierarchy to pick the correct cache tier.</p>
 */
public sealed interface IndexOptionImpl extends IndexOption
    permits IndexOptionImpl.FullySharable,
            IndexOptionImpl.UniqueToModule,
            IndexOptionImpl.SharablePerOption {

    record FullySharable() implements IndexOptionImpl {
        public static final FullySharable INSTANCE = new FullySharable();
    }

    record UniqueToModule(LocalizeValue displayName) implements IndexOptionImpl {
    }

    record SharablePerOption<T extends Record>(T value,
                                               DataExternalizer<T> externalizer,
                                               LocalizeValue displayName) implements IndexOptionImpl {
    }
}
