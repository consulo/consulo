/*
 * Copyright 2013-2023 consulo.io
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
package consulo.compiler.internal;

import consulo.annotation.component.ExtensionImpl;
import consulo.compiler.AdditionalOutputDirectoriesProvider;
import consulo.compiler.ModuleAdditionalOutputDirectoriesProvider;
import consulo.compiler.ModuleAdditionalOutputDirectory;
import consulo.module.Module;
import jakarta.inject.Inject;

import jakarta.annotation.Nonnull;

import java.util.ArrayList;
import java.util.List;

/**
 * @author VISTALL
 * @since 2023-05-07
 */
@ExtensionImpl
public class OldModuleAdditionalOutputDirectoriesProvider implements ModuleAdditionalOutputDirectoriesProvider {
    private final Module myModule;

    @Inject
    public OldModuleAdditionalOutputDirectoriesProvider(Module module) {
        myModule = module;
    }

    @Nonnull
    @Override
    public List<ModuleAdditionalOutputDirectory> getOutputDirectories() {
        List<ModuleAdditionalOutputDirectory> result = new ArrayList<>();

        AdditionalOutputDirectoriesProvider.EP_NAME.forEachExtensionSafe(provider -> {
            String[] outputDirectories = provider.getOutputDirectories(myModule.getProject(), myModule);
            for (String outputDirectory : outputDirectories) {
                result.add(new ModuleAdditionalOutputDirectory(outputDirectory, false));
            }
        });
        return result;
    }
}
