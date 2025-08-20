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
package consulo.compiler.impl.internal;

import consulo.annotation.component.ServiceImpl;
import consulo.compiler.ModuleCompilerPathsManager;
import consulo.content.ContentFolderTypeProvider;
import consulo.module.content.internal.RuntimeRootProvider;
import consulo.virtualFileSystem.VirtualFile;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;

/**
 * @author VISTALL
 * @since 2025-08-19
 */
@Singleton
@ServiceImpl
public class CompilerRuntimeRootProvider implements RuntimeRootProvider {
    private ModuleCompilerPathsManager myModuleCompilerPathsManager;

    @Inject
    public CompilerRuntimeRootProvider(ModuleCompilerPathsManager moduleCompilerPathsManager) {
        myModuleCompilerPathsManager = moduleCompilerPathsManager;
    }

    @Nullable
    @Override
    public String getCompilerOutputUrl(@Nonnull ContentFolderTypeProvider contentFolderType) {
        return myModuleCompilerPathsManager.getCompilerOutputUrl(contentFolderType);
    }

    @Nullable
    @Override
    public VirtualFile getCompilerOutput(@Nonnull ContentFolderTypeProvider contentFolderType) {
        return myModuleCompilerPathsManager.getCompilerOutput(contentFolderType);
    }
}
