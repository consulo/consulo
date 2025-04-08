/*
 * Copyright 2013-2016 consulo.io
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
package consulo.compiler;

import consulo.annotation.DeprecationInfo;
import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ExtensionAPI;
import consulo.component.extension.ExtensionPointName;
import consulo.module.Module;
import consulo.project.Project;
import jakarta.annotation.Nonnull;

/**
 * @author VISTALL
 * @since 20:22/12.06.13
 */
@Deprecated
@DeprecationInfo("Use consulo.compiler.ModuleAdditionalOutputDirectoriesProvider")
@ExtensionAPI(ComponentScope.APPLICATION)
public interface AdditionalOutputDirectoriesProvider {
    ExtensionPointName<AdditionalOutputDirectoriesProvider> EP_NAME = ExtensionPointName.create(AdditionalOutputDirectoriesProvider.class);

    @Nonnull
    String[] getOutputDirectories(@Nonnull Project project, @Nonnull Module modules);
}
