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
package com.intellij.openapi.module;

import com.intellij.openapi.components.ComponentManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.util.ArrayFactory;
import consulo.annotation.DeprecationInfo;
import consulo.disposer.Disposable;
import consulo.util.pointers.Named;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * Represents a module in an Consulo project.
 *
 * @see ModuleManager#getModules()
 */
public interface Module extends ComponentManager, Disposable, Named {
  public static final Module[] EMPTY_ARRAY = new Module[0];

  public static ArrayFactory<Module> ARRAY_FACTORY = count -> count == 0 ? EMPTY_ARRAY : new Module[count];

  /**
   * Returns the <code>VirtualFile</code> to the module dir
   *
   * @return the virtual file instance.
   */
  @Nullable
  VirtualFile getModuleDir();

  /**
   * Returns the path to the module dir
   *
   * @return the path to the module dir
   */
  @Nullable
  String getModuleDirPath();

  /**
   * Returns the path to the module url
   *
   * @return the path to the module url
   */
  @Nullable
  String getModuleDirUrl();

  /**
   * Returns the project to which this module belongs.
   *
   * @return the project instance.
   */
  @Nonnull
  Project getProject();

  /**
   * Returns the name of this module.
   *
   * @return the module name.
   */
  @Override
  @Nonnull
  String getName();

  /**
   * Checks if the module instance has been disposed and unloaded.
   *
   * @return true if the module has been disposed, false otherwise
   */
  @Override
  boolean isDisposed();

  /**
   * Sets a custom option for this module.
   *
   * @param optionName  the name of the custom option.
   * @param optionValue the value of the custom option.
   */
  @Deprecated
  @DeprecationInfo("Use ModuleExtension for store your variables")
  default void setOption(@Nonnull String optionName, @Nonnull String optionValue) {
    throw new UnsupportedOperationException();
  }

  /**
   * Removes a custom option from this module.
   *
   * @param optionName the name of the custom option.
   */
  @Deprecated
  @DeprecationInfo("Use ModuleExtension for store your variables")
  default void clearOption(@Nonnull String optionName) {
    throw new UnsupportedOperationException();
  }

  /**
   * Gets the value of a custom option for this module.
   *
   * @param optionName the name of the custom option.
   * @return the value of the custom option, or null if no value has been set.
   */
  @Nullable
  @Deprecated
  @DeprecationInfo("Use ModuleExtension for store your variables")
  default String getOptionValue(@Nonnull String optionName) {
    throw new UnsupportedOperationException();
  }

  /**
   * Returns module scope including sources and tests, excluding libraries and dependencies.
   *
   * @return scope including sources and tests, excluding libraries and dependencies.
   */
  @Nonnull
  GlobalSearchScope getModuleScope();

  @Nonnull
  GlobalSearchScope getModuleScope(boolean includeTests);

  /**
   * Returns module scope including sources, tests, and libraries, excluding dependencies.
   *
   * @return scope including sources, tests, and libraries, excluding dependencies.
   */
  @Nonnull
  GlobalSearchScope getModuleWithLibrariesScope();

  /**
   * Returns module scope including sources, tests, and dependencies, excluding libraries.
   *
   * @return scope including sources, tests, and dependencies, excluding libraries.
   */
  @Nonnull
  GlobalSearchScope getModuleWithDependenciesScope();

  @Nonnull
  GlobalSearchScope getModuleContentScope();

  @Nonnull
  GlobalSearchScope getModuleContentWithDependenciesScope();

  @Nonnull
  GlobalSearchScope getModuleWithDependenciesAndLibrariesScope(boolean includeTests);

  @Nonnull
  GlobalSearchScope getModuleWithDependentsScope();

  @Nonnull
  GlobalSearchScope getModuleTestsWithDependentsScope();

  @Nonnull
  GlobalSearchScope getModuleRuntimeScope(boolean includeTests);
}
