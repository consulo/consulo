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

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ServiceAPI;
import consulo.content.ContentFolderTypeProvider;
import consulo.module.Module;
import consulo.virtualFileSystem.VirtualFile;
import consulo.virtualFileSystem.pointer.VirtualFilePointer;

import org.jspecify.annotations.Nullable;

import java.util.Locale;

/**
 * @author VISTALL
 * @since 2013-10-20
 */
@ServiceAPI(ComponentScope.MODULE)
public abstract class ModuleCompilerPathsManager {
    
    public static ModuleCompilerPathsManager getInstance(Module module) {
        return module.getInstance(ModuleCompilerPathsManager.class);
    }

    public abstract boolean isInheritedCompilerOutput();

    public abstract void setInheritedCompilerOutput(boolean val);

    public abstract boolean isExcludeOutput();

    public abstract void setExcludeOutput(boolean val);

    public abstract void setCompilerOutputUrl(ContentFolderTypeProvider contentFolderType, @Nullable String compilerOutputUrl);

    @Nullable
    public abstract String getCompilerOutputUrl(ContentFolderTypeProvider contentFolderType);

    @Nullable
    public abstract VirtualFile getCompilerOutput(ContentFolderTypeProvider contentFolderType);

    
    public abstract VirtualFilePointer getCompilerOutputPointer(ContentFolderTypeProvider contentFolderType);

    
    public static String getRelativePathForProvider(ContentFolderTypeProvider contentFolderType, Module module) {
        return getRelativePathForProvider(contentFolderType, module.getName());
    }

    
    public static String getRelativePathForProvider(ContentFolderTypeProvider contentFolderType, String moduleName) {
        return contentFolderType.getId().toLowerCase(Locale.ROOT) + "/" + moduleName;
    }
}
