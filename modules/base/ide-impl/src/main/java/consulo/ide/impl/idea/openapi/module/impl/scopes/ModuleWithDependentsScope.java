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
import consulo.module.ModuleManager;
import consulo.module.content.ModuleRootManager;
import consulo.module.content.layer.orderEntry.ModuleOrderEntry;
import consulo.module.content.layer.orderEntry.OrderEntry;
import consulo.project.Project;
import consulo.module.content.ProjectFileIndex;
import consulo.module.content.ProjectRootManager;
import consulo.project.content.scope.ProjectAwareSearchScope;
import consulo.virtualFileSystem.VirtualFile;
import consulo.language.psi.scope.GlobalSearchScope;
import consulo.project.content.scope.ProjectScopes;
import consulo.application.util.CachedValueProvider;
import consulo.application.util.CachedValuesManager;
import consulo.util.collection.MultiMap;
import consulo.util.collection.Queue;
import org.jetbrains.annotations.NonNls;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import java.util.HashSet;
import java.util.Set;

/**
 * @author max
 */
class ModuleWithDependentsScope extends GlobalSearchScope {
  private final Module myModule;

  private final ProjectFileIndex myProjectFileIndex;
  private final Set<Module> myModules;
  private final ProjectAwareSearchScope myProjectScope;

  ModuleWithDependentsScope(@Nonnull Module module) {
    super(module.getProject());
    myModule = module;

    myProjectFileIndex = ProjectRootManager.getInstance(module.getProject()).getFileIndex();
    myProjectScope = ProjectScopes.getProjectScope(module.getProject());

    myModules = buildDependents(myModule);
  }

  private static Set<Module> buildDependents(Module module) {
    Set<Module> result = new HashSet<Module>();
    result.add(module);

    Set<Module> processedExporting = new HashSet<Module>();

    ModuleIndex index = getModuleIndex(module.getProject());

    Queue<Module> walkingQueue = new Queue<Module>(10);
    walkingQueue.addLast(module);

    while (!walkingQueue.isEmpty()) {
      Module current = walkingQueue.pullFirst();
      processedExporting.add(current);
      result.addAll(index.plainUsages.get(current));
      for (Module dependent : index.exportingUsages.get(current)) {
        result.add(dependent);
        if (processedExporting.add(dependent)) {
          walkingQueue.addLast(dependent);
        }
      }
    }
    return result;
  }

  private static class ModuleIndex {
    final MultiMap<Module, Module> plainUsages = MultiMap.create();
    final MultiMap<Module, Module> exportingUsages = MultiMap.create();
  }

  private static ModuleIndex getModuleIndex(final Project project) {
    return CachedValuesManager.getManager(project).getCachedValue(project, new CachedValueProvider<ModuleIndex>() {
      @Nullable
      @Override
      public Result<ModuleIndex> compute() {
        ModuleIndex index = new ModuleIndex();
        for (Module module : ModuleManager.getInstance(project).getModules()) {
          for (OrderEntry orderEntry : ModuleRootManager.getInstance(module).getOrderEntries()) {
            if (orderEntry instanceof ModuleOrderEntry) {
              Module referenced = ((ModuleOrderEntry)orderEntry).getModule();
              if (referenced != null) {
                MultiMap<Module, Module> map = ((ModuleOrderEntry)orderEntry).isExported() ? index.exportingUsages : index.plainUsages;
                map.putValue(referenced, module);
              }
            }
          }
        }
        return Result.create(index, ProjectRootManager.getInstance(project));
      }
    });
  }

  @Override
  public boolean contains(@Nonnull VirtualFile file) {
    return contains(file, false);
  }

  boolean contains(@Nonnull VirtualFile file, boolean myOnlyTests) {
    Module moduleOfFile = myProjectFileIndex.getModuleForFile(file);
    if (moduleOfFile == null || !myModules.contains(moduleOfFile)) return false;
    if (myOnlyTests && !myProjectFileIndex.isInTestSourceContent(file)) return false;
    return myProjectScope.contains(file);
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
    return "Module with dependents:" + myModule.getName();
  }

  public boolean equals(Object o) {
    if (this == o) return true;
    if (!(o instanceof ModuleWithDependentsScope)) return false;

    ModuleWithDependentsScope moduleWithDependentsScope = (ModuleWithDependentsScope)o;

    return myModule.equals(moduleWithDependentsScope.myModule);
  }

  public int hashCode() {
    return myModule.hashCode();
  }
}
