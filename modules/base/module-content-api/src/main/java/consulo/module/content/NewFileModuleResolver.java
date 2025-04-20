/*
 * Copyright 2013-2022 consulo.io
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
package consulo.module.content;

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ExtensionAPI;
import consulo.module.Module;
import consulo.project.Project;
import consulo.virtualFileSystem.VirtualFile;
import consulo.virtualFileSystem.fileType.FileType;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

/**
 * If module is not directory based, creating new files will have problem for selecting module (which module use as owner)
 * Use this extension for resolving it depending on your project system
 *
 * @author VISTALL
 * @since 2022-08-11
 */
@ExtensionAPI(ComponentScope.PROJECT)
public interface NewFileModuleResolver {
    @Nullable
    static Module resolveModule(@Nonnull Project project, @Nonnull VirtualFile parent, @Nonnull FileType newFileType) {
        return project.getExtensionPoint(NewFileModuleResolver.class)
            .computeSafeIfAny(it -> it.resolveModule(parent, newFileType));
    }

    @Nullable
    Module resolveModule(@Nonnull VirtualFile directory, @Nonnull FileType fileType);
}
