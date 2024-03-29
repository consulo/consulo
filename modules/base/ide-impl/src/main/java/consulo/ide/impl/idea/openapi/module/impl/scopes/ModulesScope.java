/*
 * Copyright 2000-2014 JetBrains s.r.o.
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
package consulo.ide.impl.idea.openapi.module.impl.scopes;

import consulo.module.Module;
import consulo.project.Project;
import consulo.module.content.ProjectFileIndex;
import consulo.module.content.ProjectRootManager;
import consulo.virtualFileSystem.VirtualFile;
import consulo.language.psi.scope.GlobalSearchScope;
import org.jetbrains.annotations.NonNls;
import jakarta.annotation.Nonnull;

import java.util.Arrays;
import java.util.Set;

public class ModulesScope extends GlobalSearchScope {

  private final ProjectFileIndex myProjectFileIndex;
  private final Set<Module> myModules;

  public ModulesScope(@Nonnull Set<Module> modules, Project project) {
    super(project);
    myProjectFileIndex = ProjectRootManager.getInstance(project).getFileIndex();
    myModules = modules;
  }

  @Override
  public boolean contains(@Nonnull VirtualFile file) {
    Module moduleOfFile = myProjectFileIndex.getModuleForFile(file);
    return moduleOfFile != null && myModules.contains(moduleOfFile);
  }

  @Override
  public int compare(@Nonnull VirtualFile file1, @Nonnull VirtualFile file2) {
    return 0;
  }

  @Override
  public boolean isSearchInModuleContent(@Nonnull Module aModule) {
    return myModules.contains(aModule);
  }

  @Override
  public boolean isSearchInLibraries() {
    return false;
  }

  @NonNls
  public String toString() {
    return "Modules:" + Arrays.toString(myModules.toArray(new Module[myModules.size()]));
  }
}
