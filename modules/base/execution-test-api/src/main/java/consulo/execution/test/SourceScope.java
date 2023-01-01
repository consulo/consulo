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
package consulo.execution.test;

import consulo.component.util.graph.Graph;
import consulo.language.psi.scope.GlobalSearchScope;
import consulo.module.Module;
import consulo.module.ModuleManager;
import consulo.project.Project;
import consulo.virtualFileSystem.VirtualFile;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.*;

/**
 * @author dyoma
 */
public abstract class SourceScope {
  public abstract GlobalSearchScope getGlobalSearchScope();

  public abstract Project getProject();

  public abstract GlobalSearchScope getLibrariesScope();

  public static Map<Module, Collection<Module>> buildAllDependencies(final Project project) {
    Graph<Module> graph = ModuleManager.getInstance(project).moduleGraph();
    Map<Module, Collection<Module>> result = new HashMap<>();
    for (final Module module : graph.getNodes()) {
      buildDependenciesForModule(module, graph, result);
    }
    return result;
  }

  private static void buildDependenciesForModule(final Module module, final Graph<Module> graph, Map<Module, Collection<Module>> map) {
    final Set<Module> deps = new HashSet<>();
    map.put(module, deps);
    traverseModule(deps, graph, module);
  }

  private static void traverseModule(Set<Module> deps, Graph<Module> graph, Module m) {
    for (Iterator<Module> iterator = graph.getIn(m); iterator.hasNext(); ) {
      final Module dep = iterator.next();
      if (!deps.contains(dep)) {
        deps.add(dep);
        traverseModule(deps, graph, dep);
      }
    }
  }

  private abstract static class ModuleSourceScope extends SourceScope {
    private final Project myProject;

    protected ModuleSourceScope(final Project project) {
      myProject = project;
    }

    @Override
    public Project getProject() {
      return myProject;
    }
  }

  public static SourceScope wholeProject(final Project project) {
    return new SourceScope() {
      @Override
      public GlobalSearchScope getGlobalSearchScope() {
        return GlobalSearchScope.allScope(project);
      }

      @Override
      public Project getProject() {
        return project;
      }

      @Override
      public Module[] getModulesToCompile() {
        return ModuleManager.getInstance(project).getModules();
      }

      @Override
      public GlobalSearchScope getLibrariesScope() {
        return getGlobalSearchScope();
      }
    };
  }

  public static SourceScope modulesWithDependencies(final Module[] modules) {
    if (modules == null || modules.length == 0) return null;
    return new ModuleSourceScope(modules[0].getProject()) {
      @Override
      public GlobalSearchScope getGlobalSearchScope() {
        return evaluateScopesAndUnite(modules, new ScopeForModuleEvaluator() {
          @Override
          public GlobalSearchScope evaluate(final Module module) {
            return GlobalSearchScope.moduleWithDependenciesScope(module);
          }
        });
      }

      @Override
      public GlobalSearchScope getLibrariesScope() {
        return evaluateScopesAndUnite(modules, new ScopeForModuleEvaluator() {
          @Override
          public GlobalSearchScope evaluate(final Module module) {
            return new ModuleWithDependenciesAndLibsDependencies(module);
          }
        });
      }

      @Override
      public Module[] getModulesToCompile() {
        return modules;
      }
    };
  }

  private interface ScopeForModuleEvaluator {
    GlobalSearchScope evaluate(Module module);
  }

  private static GlobalSearchScope evaluateScopesAndUnite(final Module[] modules, final ScopeForModuleEvaluator evaluator) {
    GlobalSearchScope scope = evaluator.evaluate(modules[0]);
    for (int i = 1; i < modules.length; i++) {
      final Module module = modules[i];
      final GlobalSearchScope otherscope = evaluator.evaluate(module);
      scope = scope.uniteWith(otherscope);
    }
    return scope;
  }

  public static SourceScope modules(final Module[] modules) {
    if (modules == null || modules.length == 0) return null;
    return new ModuleSourceScope(modules[0].getProject()) {
      @Override
      public GlobalSearchScope getGlobalSearchScope() {
        return evaluateScopesAndUnite(modules, new ScopeForModuleEvaluator() {
          @Override
          public GlobalSearchScope evaluate(final Module module) {
            return GlobalSearchScope.moduleScope(module);
          }
        });
      }

      @Override
      public GlobalSearchScope getLibrariesScope() {
        return evaluateScopesAndUnite(modules, new ScopeForModuleEvaluator() {
          @Override
          public GlobalSearchScope evaluate(final Module module) {
            return GlobalSearchScope.moduleWithLibrariesScope(module);
          }
        });
      }

      @Override
      public Module[] getModulesToCompile() {
        return modules;
      }
    };
  }

  public abstract Module[] getModulesToCompile();

  private static class ModuleWithDependenciesAndLibsDependencies extends GlobalSearchScope {
    private final GlobalSearchScope myMainScope;
    private final List<GlobalSearchScope> myScopes = new ArrayList<>();

    public ModuleWithDependenciesAndLibsDependencies(final Module module) {
      super(module.getProject());
      myMainScope = GlobalSearchScope.moduleWithDependenciesAndLibrariesScope(module);
      final Map<Module, Collection<Module>> map = buildAllDependencies(module.getProject());
      if (map == null) return;
      final Collection<Module> modules = map.get(module);
      for (final Module dependency : modules) {
        myScopes.add(GlobalSearchScope.moduleWithLibrariesScope(dependency));
      }
    }

    @Override
    public boolean contains(@Nonnull final VirtualFile file) {
      return findScopeFor(file) != null;
    }

    @Override
    public int compare(@Nonnull final VirtualFile file1, @Nonnull final VirtualFile file2) {
      final GlobalSearchScope scope = findScopeFor(file1);
      assert scope != null;
      if (scope.contains(file2)) return scope.compare(file1, file2);
      return 0;
    }

    @Override
    public boolean isSearchInModuleContent(@Nonnull final Module aModule) {
      return myMainScope.isSearchInModuleContent(aModule);
    }

    @Override
    public boolean isSearchInLibraries() {
      return true;
    }

    @Nullable
    private GlobalSearchScope findScopeFor(final VirtualFile file) {
      if (myMainScope.contains(file)) return myMainScope;
      //noinspection ForLoopReplaceableByForEach
      for (int i = 0, size = myScopes.size(); i < size; i++) {
        GlobalSearchScope scope = myScopes.get(i);
        if (scope.contains(file)) return scope;
      }
      return null;
    }
  }
}
