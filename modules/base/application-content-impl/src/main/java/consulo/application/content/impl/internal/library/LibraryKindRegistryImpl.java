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
package consulo.application.content.impl.internal.library;

import consulo.annotation.component.ServiceImpl;
import consulo.application.Application;
import consulo.component.extension.ExtensionPoint;
import consulo.component.extension.ExtensionPointCacheKey;
import consulo.content.internal.LibraryKindRegistry;
import consulo.content.library.LibraryKind;
import consulo.content.library.LibraryPresentation;
import consulo.content.library.LibraryPresentationProvider;
import consulo.content.library.LibraryType;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;

import java.util.Map;

/**
 * @author VISTALL
 * @since 2025-04-13
 */
@Singleton
@ServiceImpl
public class LibraryKindRegistryImpl implements LibraryKindRegistry {
    private static final ExtensionPointCacheKey<LibraryType, Map<String, LibraryType>> KINDS_FROM_LIBRARY_TYPE =
        ExtensionPointCacheKey.groupBy("KINDS_FROM_LIBRARY_TYPE", t -> t.getKind().getKindId());

    private static final ExtensionPointCacheKey<LibraryPresentationProvider, Map<String, LibraryPresentationProvider>>
        KINDS_FROM_LIBRARY_PRESENTATION_PROVIDER = ExtensionPointCacheKey.groupBy("KINDS_FROM_LIBRARY_TYPE", t -> t.getKind().getKindId());

    private final Application myApplication;

    @Inject
    public LibraryKindRegistryImpl(Application application) {
        myApplication = application;
    }

    @Nullable
    @Override
    public LibraryKind findKindById(@Nullable String libraryKindId) {
        if (libraryKindId == null) {
            return null;
        }

        LibraryType libraryType = find(libraryKindId, LibraryType.class, KINDS_FROM_LIBRARY_TYPE);
        if (libraryType != null) {
            return libraryType.getKind();
        }

        LibraryPresentationProvider provider =
            find(libraryKindId, LibraryPresentationProvider.class, KINDS_FROM_LIBRARY_PRESENTATION_PROVIDER);
        if (provider != null) {
            return provider.getKind();
        }

        return null;
    }

    @Nullable
    @Override
    public LibraryType<?> findLibraryTypeByKindId(@Nullable String libraryKindId) {
        return libraryKindId == null ? null : find(libraryKindId, LibraryType.class, KINDS_FROM_LIBRARY_TYPE);
    }

    private <P extends LibraryPresentation<?>> P find(
        @Nonnull String libraryKindId,
        @Nonnull Class<P> pClass,
        @Nonnull ExtensionPointCacheKey<P, Map<String, P>> cacheKey
    ) {
        ExtensionPoint<P> libraryTypes = myApplication.getExtensionPoint(pClass);

        Map<String, P> map = libraryTypes.getOrBuildCache(cacheKey);

        return map.get(libraryKindId);
    }
}
