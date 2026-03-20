/*
 * Copyright 2000-2012 JetBrains s.r.o.
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
package consulo.module;

import consulo.annotation.UsedInPlugin;
import consulo.annotation.access.RequiredReadAction;
import consulo.annotation.access.RequiredWriteAction;
import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ServiceAPI;
import consulo.component.util.graph.Graph;
import consulo.project.Project;
import consulo.ui.image.Image;

import org.jspecify.annotations.Nullable;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * Provides services for working with the modules of a project.
 */
@ServiceAPI(ComponentScope.PROJECT)
public abstract class ModuleManager {
  /**
   * Returns the module manager instance for the current project.
   *
   * @param project the project for which the module manager is requested.
   * @return the module manager instance.
   */
  
  public static ModuleManager getInstance(Project project) {
    return project.getComponent(ModuleManager.class);
  }

  /**
   * Creates a module of the specified type at the specified path and adds it to the project
   * to which the module manager is related.
   *
   *
   * @param name the module name
   * @param dirPath the path at which the module is created.
   * @return the module instance.
   */
  
  @RequiredWriteAction
  public abstract Module newModule(String name, String dirPath);

  /**
   * Disposes of the specified module and removes it from the project.
   *
   * @param module the module to remove.
   */
  @RequiredWriteAction
  public abstract void disposeModule(Module module);

  /**
   * Returns the list of all modules in the project.
   *
   * @return the array of modules.
   */
  
  public abstract Module[] getModules();

  /**
   * Returns the project module with the specified name.
   *
   * @param name the name of the module to find.
   * @return the module instance, or null if no module with such name exists.
   */
  @RequiredReadAction
  public abstract @Nullable Module findModuleByName(String name);

  /**
   * Returns the list of modules sorted by dependency (the modules which do not depend
   * on anything are in the beginning of the list, a module which depends on another module
   * follows it in the list).
   *
   * @return the sorted array of modules.
   */
  
  @RequiredReadAction
  public abstract Module[] getSortedModules();

  /**
   * Returns the module comparator which can be used for sorting modules by dependency
   * (the modules which do not depend on anything are in the beginning of the list,
   * a module which depends on another module follows it in the list).
   *
   * @return the module comparator instance.
   */
  
  @RequiredReadAction
  public abstract Comparator<Module> moduleDependencyComparator();

  /**
   * Returns the list of modules which directly depend on the specified module.
   *
   * @param module the module for which the list of dependent modules is requested.
   * @return list of <i>modules that depend on</i> given module.
   *
   * @see ModuleUtil#getAllDependentModules(Module)
   */
  
  @RequiredReadAction
  public abstract List<Module> getModuleDependentModules(Module module);

  /**
   * Checks if one of the specified modules directly depends on the other module.
   *
   * @param module   the module to check the dependency for.
   * @param onModule the module on which <code>module</code> may depend.
   * @return true if <code>module</code> directly depends on <code>onModule</code>, false otherwise.
   */
  @RequiredReadAction
  public abstract boolean isModuleDependent(Module module, Module onModule);

  /**
   * Returns the graph of dependencies between modules in the project.
   *
   * @return the module dependency graph.
   */
  
  @RequiredReadAction
  public abstract Graph<Module> moduleGraph();

  /**
   * Returns the graph of dependencies between modules in the project.
   *
   * @param includeTests whether test-only dependencies should be included
   * @return the module dependency graph.
   * @since 11.0
   */
  
  @RequiredReadAction
  public abstract Graph<Module> moduleGraph(boolean includeTests);

  /**
   * Returns the model for the list of modules in the project, which can be used to add,
   * remove or modify modules.
   *
   * @return the modifiable model instance.
   */
  
  @RequiredReadAction
  public abstract ModifiableModuleModel getModifiableModel();


  /**
   * Returns the path to the group to which the specified module belongs, as an
   * array of group names starting from the project root.
   *
   * @param module the module for which the path is requested.
   * @return the path to the group for the module, or null if the module does not belong to any group.
   */
  @RequiredReadAction
  public abstract @Nullable String[] getModuleGroupPath(Module module);

  public @Nullable UnloadedModuleDescription getUnloadedModuleDescription(String name) {
    // we not support module unloading
    return null;
  }

  
  public Collection<UnloadedModuleDescription> getUnloadedModuleDescriptions() {
    return Collections.emptyList();
  }

  
  @UsedInPlugin
  public abstract Image getModuleIcon(@Nullable Module module);
}
