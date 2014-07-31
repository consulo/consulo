/*
 * Copyright 2013-2014 must-be.org
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
package com.intellij.openapi.roots;

import com.google.common.base.Predicate;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.vfs.VirtualFile;
import org.consulo.module.extension.ModuleExtension;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.mustbe.consulo.DeprecationInfo;
import org.mustbe.consulo.roots.ContentFolderTypeProvider;

/**
 * @author VISTALL
 * @since 29.07.14
 */
public interface ModuleRootLayer {
  /**
   * Returns the module to which the model belongs.
   *
   * @return the module instance.
   */
  @NotNull
  Module getModule();

  /**
   * Use this method to obtain all content entries of a module. Entries are given in
   * lexicographical order of their paths.
   *
   * @return list of content entries for this module
   * @see ContentEntry
   */
  ContentEntry[] getContentEntries();

  /**
   * Use this method to obtain order of roots of a module. Order of entries is important.
   *
   * @return list of order entries for this module
   */
  @NotNull
  OrderEntry[] getOrderEntries();

  /**
   * Returns an array of content roots from all content entries. A helper method.
   *
   * @return the array of content roots.
   * @see #getContentEntries()
   */
  @NotNull
  VirtualFile[] getContentRoots();

  /**
   * Returns an array of content root urls from all content entries. A helper method.
   *
   * @return the array of content root URLs.
   * @see #getContentEntries()
   */
  @NotNull
  String[] getContentRootUrls();

  @NotNull
  String[] getContentFolderUrls(@NotNull Predicate<ContentFolderTypeProvider> predicate);

  @NotNull
  VirtualFile[] getContentFolderFiles(@NotNull Predicate<ContentFolderTypeProvider> predicate);

  @NotNull
  ContentFolder[] getContentFolders(@NotNull Predicate<ContentFolderTypeProvider> predicate);

  /**
   * Returns an array of exclude roots from all content entries. A helper method.
   *
   * @return the array of excluded roots.
   * @see #getContentEntries()
   */
  @NotNull
  @Deprecated
  @DeprecationInfo(value = "Use #getContentFolderFiles(ContentFolderScopes.excluded())", until = "2.0")
  VirtualFile[] getExcludeRoots();

  /**
   * Returns an array of exclude root urls from all content entries. A helper method.
   *
   * @return the array of excluded root URLs.
   * @see #getContentEntries()
   */
  @Deprecated
  @NotNull
  @DeprecationInfo(value = "Use #getContentFolderUrls(ContentFolderScopes.excluded())", until = "2.0")
  String[] getExcludeRootUrls();

  /**
   * Returns an array of source roots from all content entries. A helper method.
   *
   * @return the array of source roots.
   * @see #getContentEntries()
   * @see #getSourceRoots(boolean)
   */
  @NotNull
  @Deprecated
  @DeprecationInfo(value = "Use #getContentFolderFiles(ContentFolderScopes.productionAndTest())", until = "2.0")
  VirtualFile[] getSourceRoots();

  /**
   * Returns an array of source roots from all content entries. A helper method.
   *
   * @param includingTests determines whether test source roots should be included in the result
   * @return the array of source roots.
   * @see #getContentEntries()
   * @since 10.0
   */
  @NotNull
  @Deprecated
  @DeprecationInfo(value = "Use #getContentFolderFiles(ContentFolderScopes.production()) or #getContentFolderFiles(ContentFolderScopes.productionAndTest()",
                   until = "2.0")
  VirtualFile[] getSourceRoots(boolean includingTests);

  /**
   * Returns an array of source root urls from all content entries. A helper method.
   *
   * @return the array of source root URLs.
   * @see #getContentEntries()
   * @see #getSourceRootUrls(boolean)
   */
  @Deprecated
  @NotNull
  @DeprecationInfo(value = "Use #getContentFolderUrls(ContentFolderScopes.productionAndTest())", until = "2.0")
  String[] getSourceRootUrls();

  /**
   * Returns an array of source root urls from all content entries. A helper method.
   *
   * @param includingTests determines whether test source root urls should be included in the result
   * @return the array of source root URLs.
   * @see #getContentEntries()
   * @since 10.0
   */
  @NotNull
  @Deprecated
  @DeprecationInfo(value = "Use #getContentFolderUrls(ContentFolderScopes.production()) or #getContentFolderFiles(ContentFolderScopes.productionAndTest()",
                   until = "2.0")
  String[] getSourceRootUrls(boolean includingTests);

  /**
   * Passes all order entries in the module to the specified visitor.
   *
   * @param policy       the visitor to accept.
   * @param initialValue the default value to be returned by the visit process.
   * @return the value returned by the visitor.
   * @see OrderEntry#accept(RootPolicy, Object)
   */
  <R> R processOrder(RootPolicy<R> policy, R initialValue);

  /**
   * Returns {@link OrderEnumerator} instance which can be used to process order entries of the module (with or without dependencies) and
   * collect classes or source roots
   *
   * @return {@link OrderEnumerator} instance
   * @since 10.0
   */
  @NotNull
  OrderEnumerator orderEntries();

  /**
   * Returns list of module names <i>this module</i> depends on.
   *
   * @return the list of module names this module depends on.
   */
  @NotNull
  String[] getDependencyModuleNames();

  @Nullable
  <T extends ModuleExtension> T getExtension(Class<T> clazz);

  @Nullable
  <T extends ModuleExtension> T getExtension(@NotNull String key);

  @Nullable
  <T extends ModuleExtension> T getExtensionWithoutCheck(Class<T> clazz);

  @Nullable
  <T extends ModuleExtension> T getExtensionWithoutCheck(@NotNull String key);

  @NotNull
  ModuleExtension[] getExtensions();

  @NotNull
  Module[] getModuleDependencies();

  @NotNull
  Module[] getModuleDependencies(boolean includeTests);
}
