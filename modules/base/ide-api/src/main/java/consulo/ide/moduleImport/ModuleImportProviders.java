/*
 * Copyright 2013-2017 consulo.io
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
package consulo.ide.moduleImport;

import consulo.application.Application;
import jakarta.annotation.Nonnull;

import java.util.ArrayList;
import java.util.List;

/**
 * @author VISTALL
 * @since 2017-01-30
 */
public class ModuleImportProviders {
    @Nonnull
    public static List<ModuleImportProvider> getExtensions(boolean forImportAction) {
        List<ModuleImportProvider> list = new ArrayList<>();
        Application.get().getExtensionPoint(ModuleImportProvider.class).forEachExtensionSafe(provider -> {
            if (forImportAction) {
                if (!provider.isOnlyForNewImport()) {
                    list.add(provider);
                }
            }
            else {
                list.add(provider);
            }
        });
        return list;
    }
}
