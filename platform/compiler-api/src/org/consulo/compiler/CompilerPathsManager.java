/*
 * Copyright 2013 Consulo.org
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
package org.consulo.compiler;

import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.ContentFolderType;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.pointers.VirtualFilePointer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * @author VISTALL
 * @since 17:03/26.05.13
 */
public abstract class CompilerPathsManager {
  @NotNull
  public static CompilerPathsManager getInstance(@NotNull final Project project) {
    return ServiceManager.getService(project, CompilerPathsManager.class);
  }

  @Nullable
  public abstract VirtualFile getCompilerOutput();

  @Nullable
  public abstract String getCompilerOutputUrl();

  public abstract VirtualFilePointer getCompilerOutputPointer();

  public abstract void setCompilerOutputPointer(VirtualFilePointer pointer);

  public abstract void setCompilerOutputUrl(String compilerOutputUrl);

  public abstract String getCompilerOutputUrl(@NotNull Module module, @NotNull ContentFolderType contentFolderType);

  @Nullable
  public abstract VirtualFile getCompilerOutput(@NotNull Module module, @NotNull ContentFolderType contentFolderType);
}
