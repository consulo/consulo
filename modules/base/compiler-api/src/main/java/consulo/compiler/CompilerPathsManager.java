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

import com.intellij.openapi.module.Module;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.pointers.VirtualFilePointer;
import consulo.annotation.DeprecationInfo;
import consulo.roots.ContentFolderTypeProvider;
import jakarta.inject.Inject;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import jakarta.inject.Singleton;

/**
 * @author VISTALL
 * @since 17:03/26.05.13
 */
@Deprecated
@DeprecationInfo(value = "Use CompilerConfiguration for projects, and ModuleCompilerPathsManager for modules", until = "2.0")
@Singleton
public class CompilerPathsManager {
  @Nonnull
  public static CompilerPathsManager getInstance(@Nonnull final Project project) {
    return project.getComponent(CompilerPathsManager.class);
  }

  private final Project myProject;

  @Inject
  public CompilerPathsManager(Project project) {
    myProject = project;
  }

  @Nullable
  public VirtualFile getCompilerOutput() {
    return CompilerConfiguration.getInstance(myProject).getCompilerOutput();
  }

  @Nullable
  public String getCompilerOutputUrl() {
    return CompilerConfiguration.getInstance(myProject).getCompilerOutputUrl();
  }

  public VirtualFilePointer getCompilerOutputPointer() {
    return CompilerConfiguration.getInstance(myProject).getCompilerOutputPointer();
  }

  public void setCompilerOutputUrl(@Nullable String compilerOutputUrl) {
    CompilerConfiguration.getInstance(myProject).setCompilerOutputUrl(compilerOutputUrl);
  }

  public boolean isInheritedCompilerOutput(@Nonnull Module module) {
    return ModuleCompilerPathsManager.getInstance(module).isInheritedCompilerOutput();
  }

  public void setInheritedCompilerOutput(@Nonnull Module module, boolean val) {
    ModuleCompilerPathsManager.getInstance(module).setInheritedCompilerOutput(val);
  }

  public boolean isExcludeOutput(@Nonnull Module module) {
    return ModuleCompilerPathsManager.getInstance(module).isExcludeOutput();
  }

  public void setExcludeOutput(@Nonnull Module module, boolean val) {
    ModuleCompilerPathsManager.getInstance(module).setExcludeOutput(val);
  }

  public void setCompilerOutputUrl(@Nonnull Module module, @Nonnull ContentFolderTypeProvider contentFolderType, @Nullable String compilerOutputUrl) {
    ModuleCompilerPathsManager.getInstance(module).setCompilerOutputUrl(contentFolderType, compilerOutputUrl);
  }

  public String getCompilerOutputUrl(@Nonnull Module module, @Nonnull ContentFolderTypeProvider contentFolderType) {
    return ModuleCompilerPathsManager.getInstance(module).getCompilerOutputUrl(contentFolderType);
  }

  @Nullable
  public VirtualFile getCompilerOutput(@Nonnull Module module, @Nonnull ContentFolderTypeProvider contentFolderType) {
    return ModuleCompilerPathsManager.getInstance(module).getCompilerOutput(contentFolderType);
  }

  @Nonnull
  public VirtualFilePointer getCompilerOutputPointer(@Nonnull Module module, @Nonnull ContentFolderTypeProvider contentFolderType) {
    return ModuleCompilerPathsManager.getInstance(module).getCompilerOutputPointer(contentFolderType);
  }
}
