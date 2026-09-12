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
package consulo.language.index.impl.internal.moduleAware;

import consulo.application.Application;
import consulo.component.extension.ExtensionPoint;
import consulo.component.extension.ExtensionPointCacheKey;
import consulo.language.psi.stub.ModuleAwareIndexOptionProvider;
import consulo.virtualFileSystem.fileType.FileType;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class ModuleAwareIndexOptionRegistry {
    private static final ExtensionPointCacheKey<ModuleAwareIndexOptionProvider, Map<String, ModuleAwareIndexOptionProvider>> BY_ID =
        ExtensionPointCacheKey.groupBy("ModuleAwareIndexOptionProvider.byId", ModuleAwareIndexOptionProvider::getId);

    private static final ExtensionPointCacheKey<ModuleAwareIndexOptionProvider, Map<FileType, List<ModuleAwareIndexOptionProvider>>> BY_FILE_TYPE =
        ExtensionPointCacheKey.create("ModuleAwareIndexOptionProvider.byFileType", walker -> {
            Map<FileType, List<ModuleAwareIndexOptionProvider>> byFileType = new HashMap<>();
            walker.walk(provider -> {
                for (FileType fileType : provider.getInputFileTypes()) {
                    byFileType.computeIfAbsent(fileType, ignored -> new ArrayList<>()).add(provider);
                }
            });
            byFileType.replaceAll((fileType, providers) -> List.copyOf(providers));
            return byFileType;
        });

    private ModuleAwareIndexOptionRegistry() {
    }

    public static List<ModuleAwareIndexOptionProvider> getApplicableProviders(FileType fileType) {
        return point().getOrBuildCache(BY_FILE_TYPE).getOrDefault(fileType, List.of());
    }

    public static @Nullable ModuleAwareIndexOptionProvider findById(String id) {
        return point().getOrBuildCache(BY_ID).get(id);
    }

    private static ExtensionPoint<ModuleAwareIndexOptionProvider> point() {
        return Application.get().getExtensionPoint(ModuleAwareIndexOptionProvider.class);
    }
}
