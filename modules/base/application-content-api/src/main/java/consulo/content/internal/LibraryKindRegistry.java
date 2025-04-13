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
package consulo.content.internal;

import consulo.annotation.DeprecationInfo;
import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ServiceAPI;
import consulo.application.Application;
import consulo.content.library.LibraryKind;
import consulo.content.library.LibraryType;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

/**
 * @author VISTALL
 * @since 2025-04-13
 */
@ServiceAPI(ComponentScope.APPLICATION)
public interface LibraryKindRegistry {
    @Deprecated
    @DeprecationInfo("Use injecting")
    static LibraryKindRegistry getInstance() {
        return Application.get().getInstance(LibraryKindRegistry.class);
    }

    @Nullable
    LibraryKind findKindById(@Nullable String libraryKindId);

    @Nullable
    LibraryType<?> findLibraryTypeByKindId(@Nullable String libraryKindId);

    @Nonnull
    default LibraryType<?> findLibraryTypeByKindIdSafe(@Nullable String libraryKindId) {
        LibraryType<?> type = findLibraryTypeByKindId(libraryKindId);
        if (type == null) {
            throw new IllegalArgumentException("Unregistered libraryKind: " + libraryKindId);
        }
        return type;
    }
}
