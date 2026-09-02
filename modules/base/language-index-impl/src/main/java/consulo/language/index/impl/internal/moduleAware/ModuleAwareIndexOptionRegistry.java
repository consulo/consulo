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

import consulo.language.psi.stub.ModuleAwareIndexOptionProvider;
import consulo.virtualFileSystem.fileType.FileType;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * Dispatch helper for {@link ModuleAwareIndexOptionProvider} — returns providers claiming
 * a given file type, or looks up a provider by stable id (used during revalidation when
 * the stored meta names providers that may or may not still be registered).
 *
 * <p>Reads the extension point on every call. Caching can be bolted on later with an
 * extension-point listener for invalidation; v1 stays dumb.</p>
 */
public final class ModuleAwareIndexOptionRegistry {
    private ModuleAwareIndexOptionRegistry() {
    }

    public static List<ModuleAwareIndexOptionProvider> getApplicableProviders(FileType fileType) {
        List<ModuleAwareIndexOptionProvider> all = ModuleAwareIndexOptionProvider.EP_NAME.getExtensionList();
        List<ModuleAwareIndexOptionProvider> result = new ArrayList<>(all.size());
        for (ModuleAwareIndexOptionProvider provider : all) {
            if (provider.getInputFileTypes().contains(fileType)) {
                result.add(provider);
            }
        }
        return result;
    }

    public static @Nullable ModuleAwareIndexOptionProvider findById(String id) {
        for (ModuleAwareIndexOptionProvider provider : ModuleAwareIndexOptionProvider.EP_NAME.getExtensionList()) {
            if (provider.getId().equals(id)) {
                return provider;
            }
        }
        return null;
    }
}
