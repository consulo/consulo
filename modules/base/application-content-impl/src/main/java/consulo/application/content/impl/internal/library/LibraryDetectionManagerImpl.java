/*
 * Copyright 2000-2010 JetBrains s.r.o.
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
import consulo.content.library.*;
import consulo.util.collection.ContainerUtil;
import consulo.util.collection.SmartList;
import consulo.util.lang.Pair;
import consulo.virtualFileSystem.VirtualFile;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import jakarta.inject.Singleton;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author nik
 */
@Singleton
@ServiceImpl
public class LibraryDetectionManagerImpl extends LibraryDetectionManager {
    private final Map<List<VirtualFile>, List<Pair<LibraryKind, LibraryProperties>>> myCache = new HashMap<>();

    @Override
    public boolean processProperties(@Nonnull List<VirtualFile> files, @Nonnull LibraryPropertiesProcessor processor) {
        for (Pair<LibraryKind, LibraryProperties> pair : getOrComputeKinds(files)) {
            //noinspection unchecked
            if (!processor.processProperties(pair.getFirst(), pair.getSecond())) {
                return false;
            }
        }
        return true;
    }

    @Nullable
    @Override
    public Pair<LibraryType<?>, LibraryProperties<?>> detectType(@Nonnull List<VirtualFile> files) {
        Pair<LibraryType<?>, LibraryProperties<?>> result = null;
        for (LibraryType<?> type : LibraryType.EP_NAME.getExtensions()) {
            LibraryProperties<?> properties = type.detect(files);
            if (properties != null) {
                if (result != null) {
                    return null;
                }
                result = Pair.<LibraryType<?>, LibraryProperties<?>>create(type, properties);
            }
        }
        return result;
    }

    private List<Pair<LibraryKind, LibraryProperties>> getOrComputeKinds(List<VirtualFile> files) {
        List<Pair<LibraryKind, LibraryProperties>> result = myCache.get(files);
        if (result == null) {
            result = computeKinds(files);
            myCache.put(files, result);
        }
        return result;
    }

    private static List<Pair<LibraryKind, LibraryProperties>> computeKinds(List<VirtualFile> files) {
        SmartList<Pair<LibraryKind, LibraryProperties>> result = new SmartList<>();
        List<LibraryType> libraryTypes = LibraryType.EP_NAME.getExtensionList();
        List<LibraryPresentationProvider> presentationProviders = LibraryPresentationProvider.EP_NAME.getExtensionList();
        for (LibraryPresentation provider : ContainerUtil.concat(libraryTypes, presentationProviders)) {
            LibraryProperties properties = provider.detect(files);
            if (properties != null) {
                result.add(Pair.create(provider.getKind(), properties));
            }
        }
        return result;
    }
}
