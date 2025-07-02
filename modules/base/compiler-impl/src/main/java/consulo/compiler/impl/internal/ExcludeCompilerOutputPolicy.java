/*
 * Copyright 2000-2009 JetBrains s.r.o.
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

import consulo.annotation.component.ExtensionImpl;
import consulo.module.content.layer.DirectoryIndexExcludePolicy;
import consulo.project.Project;
import consulo.module.content.layer.ContentEntry;
import consulo.virtualFileSystem.VirtualFile;
import consulo.virtualFileSystem.pointer.VirtualFilePointer;
import consulo.compiler.CompilerConfiguration;
import consulo.compiler.ModuleCompilerPathsManager;
import consulo.language.content.LanguageContentFolderScopes;
import consulo.content.ContentFolderTypeProvider;
import consulo.module.content.layer.ModuleRootLayer;
import jakarta.inject.Inject;

import jakarta.annotation.Nonnull;

import java.util.ArrayList;
import java.util.List;

/**
 * @author yole
 */
@ExtensionImpl
public class ExcludeCompilerOutputPolicy implements DirectoryIndexExcludePolicy {
    private final Project myProject;

    @Inject
    public ExcludeCompilerOutputPolicy(final Project project) {
        myProject = project;
    }

    @Nonnull
    @Override
    public VirtualFile[] getExcludeRootsForProject() {
        VirtualFile outputPath = CompilerConfiguration.getInstance(myProject).getCompilerOutput();
        if (outputPath != null) {
            return new VirtualFile[]{outputPath};
        }
        return VirtualFile.EMPTY_ARRAY;
    }

    @Nonnull
    @Override
    public VirtualFilePointer[] getExcludeRootsForModule(@Nonnull final ModuleRootLayer moduleRootLayer) {
        ModuleCompilerPathsManager manager = ModuleCompilerPathsManager.getInstance(moduleRootLayer.getModule());
        List<VirtualFilePointer> result = new ArrayList<VirtualFilePointer>(3);

        if (manager.isInheritedCompilerOutput()) {
            final VirtualFilePointer compilerOutputPointer = CompilerConfiguration.getInstance(myProject).getCompilerOutputPointer();
            for (ContentEntry contentEntry : moduleRootLayer.getContentEntries()) {
                if (compilerOutputPointer.getUrl().contains(contentEntry.getUrl())) {
                    result.add(compilerOutputPointer);
                }
            }
        }
        else {
            if (!manager.isExcludeOutput()) {
                return VirtualFilePointer.EMPTY_ARRAY;
            }

            for (ContentFolderTypeProvider contentFolderType
                : ContentFolderTypeProvider.filter(LanguageContentFolderScopes.productionAndTest())) {
                result.add(manager.getCompilerOutputPointer(contentFolderType));
            }
        }
        return result.isEmpty() ? VirtualFilePointer.EMPTY_ARRAY : result.toArray(new VirtualFilePointer[result.size()]);
    }
}
