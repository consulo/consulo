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

import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.pointers.VirtualFilePointer;
import consulo.roots.ContentFolderTypeProvider;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * @author VISTALL
 * @since 18:27/20.10.13
 */
public abstract class ModuleCompilerPathsManager {
  @Nonnull
  public static ModuleCompilerPathsManager getInstance(@Nonnull Module module) {
    return ServiceManager.getService(module, ModuleCompilerPathsManager.class);
  }

  public abstract boolean isInheritedCompilerOutput();

  public abstract void setInheritedCompilerOutput(boolean val);

  public abstract boolean isExcludeOutput();

  public abstract void setExcludeOutput(boolean val);

  public abstract void setCompilerOutputUrl(@Nonnull ContentFolderTypeProvider contentFolderType, @Nullable String compilerOutputUrl);

  @Nullable
  public abstract String getCompilerOutputUrl(@Nonnull ContentFolderTypeProvider contentFolderType);

  @Nullable
  public abstract VirtualFile getCompilerOutput(@Nonnull ContentFolderTypeProvider contentFolderType);

  @Nonnull
  public abstract VirtualFilePointer getCompilerOutputPointer(@Nonnull ContentFolderTypeProvider contentFolderType);
}
