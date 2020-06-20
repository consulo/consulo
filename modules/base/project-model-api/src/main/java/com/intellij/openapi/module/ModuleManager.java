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
package com.intellij.openapi.module;

import com.intellij.openapi.project.Project;
import com.intellij.util.graph.Graph;
import consulo.annotation.access.RequiredReadAction;
import consulo.annotation.access.RequiredWriteAction;
import org.jetbrains.annotations.NonNls;
import javax.annotation.Nonnull;

import javax.annotation.Nullable;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * Provides services for working with the modules of a project.
 */
public abstract class ModuleManager {
  /**
   * Returns the module manager instance for the current project.
   *
   * @param project the project for which the module manager is requested.
   * @return the module manager instance.
   */
  @Nonnull
  public static ModuleManager getInstance(@Nonnull Project project) {
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
  @Nonnull
  @RequiredWriteAction
  public abstract Module newModule(@Nonnull String name, @Nonnull String dirPath);

  /**
   * Disposes of the specified module and removes it from the project.
   *
   * @param module the module to remove.
   */
  @RequiredWriteAction
  public abstract void disposeModule(@Nonnull Module module);

  /**
   * Returns the list of all modules in the project.
   *
   * @return the array of modules.
   */
  @Nonnull
  @RequiredReadAction
  public abstract Module[] getModules();

  /**
   * Returns the project module with the specified name.
   *
   * @param name the name of the module to find.
   * @return the module instance, or null if no module with such name exists.
   */
  @Nullable
  @RequiredReadAction
  public abstract Module findModuleByName(@Nonnull String name);

  /**
   * Returns the list of modules sorted by dependency (the modules which do not depend
   * on anything are in the beginning of the list, a module which depends on another module
   * follows it in the list).
   *
   * @return the sorted array of modules.
   */
  @Nonnull
  @RequiredReadAction
  public abstract Module[] getSortedModules();

  /**
   * Returns the module comparator which can be used for sorting modules by dependency
   * (the modules which do not depend on anything are in the beginning of the list,
   * a module which depends on another module follows it in the list).
   *
   * @return the module comparator instance.
   */
  @Nonnull
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
  @Nonnull
  @RequiredReadAction
  public abstract List<Module> getModuleDependentModules(@Nonnull Module module);

  /**
   * Checks if one of the specified modules directly depends on the other module.
   *
   * @param module   the module to check the dependency for.
   * @param onModule the module on which <code>module</code> may depend.
   * @return true if <code>module</code> directly depends on <code>onModule</code>, false otherwise.
   */
  @RequiredReadAction
  public abstract boolean isModuleDependent(@Nonnull Module module, @Nonnull Module onModule);

  /**
   * Returns the graph of dependencies between modules in the project.
   *
   * @return the module dependency graph.
   */
  @Nonnull
  @RequiredReadAction
  public abstract Graph<Module> moduleGraph();

  /**
   * Returns the graph of dependencies between modules in the project.
   *
   * @param includeTests whether test-only dependencies should be included
   * @return the module dependency graph.
   * @since 11.0
   */
  @Nonnull
  @RequiredReadAction
  public abstract Graph<Module> moduleGraph(boolean includeTests);

  /**
   * Returns the model for the list of modules in the project, which can be used to add,
   * remove or modify modules.
   *
   * @return the modifiable model instance.
   */
  @Nonnull
  @RequiredReadAction
  public abstract ModifiableModuleModel getModifiableModel();


  /**
   * Returns the path to the group to which the specified module belongs, as an
   * array of group names starting from the project root.
   *
   * @param module the module for which the path is requested.
   * @return the path to the group for the module, or null if the module does not belong to any group.
   */
  @Nullable
  @RequiredReadAction
  public abstract String[] getModuleGroupPath(@Nonnull Module module);

  @Nullable
  public UnloadedModuleDescription getUnloadedModuleDescription(@Nonnull String name) {
    // we not support module unloading
    return null;
  }

  @Nonnull
  public Collection<UnloadedModuleDescription> getUnloadedModuleDescriptions() {
    return Collections.emptyList();
  }
}
