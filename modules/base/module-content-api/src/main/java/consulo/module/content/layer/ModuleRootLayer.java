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
package consulo.module.content.layer;

import consulo.annotation.DeprecationInfo;
import consulo.content.ContentFolderTypeProvider;
import consulo.module.Module;
import consulo.module.content.layer.orderEntry.OrderEntry;
import consulo.module.content.layer.orderEntry.RootPolicy;
import consulo.module.extension.ModuleExtension;
import consulo.project.Project;
import consulo.virtualFileSystem.VirtualFile;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import java.util.List;
import java.util.function.Predicate;

/**
 * @author VISTALL
 * @since 29.07.14
 */
public interface ModuleRootLayer {
  @Nonnull
  Project getProject();

  /**
   * Returns the module to which the model belongs.
   *
   * @return the module instance.
   */
  @Nonnull
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
   * Iterate content entries without creating array
   * @param processor for iterate
   * @return true if iteration finished normally
   */
  boolean iterateContentEntries(@Nonnull Predicate<ContentEntry> processor);

  /**
   * Use this method to obtain order of roots of a module. Order of entries is important.
   *
   * @return list of order entries for this module
   */
  @Nonnull
  OrderEntry[] getOrderEntries();

  /**
   * Returns an array of content roots from all content entries. A helper method.
   *
   * @return the array of content roots.
   * @see #getContentEntries()
   */
  @Nonnull
  VirtualFile[] getContentRoots();

  /**
   * Returns an array of content root urls from all content entries. A helper method.
   *
   * @return the array of content root URLs.
   * @see #getContentEntries()
   */
  @Nonnull
  String[] getContentRootUrls();

  @Nonnull
  String[] getContentFolderUrls(@Nonnull Predicate<ContentFolderTypeProvider> predicate);

  @Nonnull
  VirtualFile[] getContentFolderFiles(@Nonnull Predicate<ContentFolderTypeProvider> predicate);

  @Nonnull
  ContentFolder[] getContentFolders(@Nonnull Predicate<ContentFolderTypeProvider> predicate);

  /**
   * Returns an array of exclude roots from all content entries. A helper method.
   *
   * @return the array of excluded roots.
   * @see #getContentEntries()
   */
  @Nonnull
  @Deprecated
  @DeprecationInfo(value = "Use #getContentFolderFiles(ContentFolderScopes.excluded())")
  VirtualFile[] getExcludeRoots();

  /**
   * Returns an array of exclude root urls from all content entries. A helper method.
   *
   * @return the array of excluded root URLs.
   * @see #getContentEntries()
   */
  @Deprecated
  @Nonnull
  @DeprecationInfo(value = "Use #getContentFolderUrls(ContentFolderScopes.excluded())")
  String[] getExcludeRootUrls();

  /**
   * Returns an array of source roots from all content entries. A helper method.
   *
   * @return the array of source roots.
   * @see #getContentEntries()
   * @see #getSourceRoots(boolean)
   */
  @Nonnull
  @Deprecated
  @DeprecationInfo(value = "Use #getContentFolderFiles(ContentFolderScopes.productionAndTest())")
  VirtualFile[] getSourceRoots();

  /**
   * Returns an array of source roots from all content entries. A helper method.
   *
   * @param includingTests determines whether test source roots should be included in the result
   * @return the array of source roots.
   * @see #getContentEntries()
   * @since 10.0
   */
  @Nonnull
  @Deprecated
  @DeprecationInfo(value = "Use #getContentFolderFiles(ContentFolderScopes.production()) or #getContentFolderFiles(ContentFolderScopes.productionAndTest()")
  VirtualFile[] getSourceRoots(boolean includingTests);

  /**
   * Returns an array of source root urls from all content entries. A helper method.
   *
   * @return the array of source root URLs.
   * @see #getContentEntries()
   * @see #getSourceRootUrls(boolean)
   */
  @Deprecated
  @Nonnull
  @DeprecationInfo(value = "Use #getContentFolderUrls(ContentFolderScopes.productionAndTest())")
  String[] getSourceRootUrls();

  /**
   * Returns an array of source root urls from all content entries. A helper method.
   *
   * @param includingTests determines whether test source root urls should be included in the result
   * @return the array of source root URLs.
   * @see #getContentEntries()
   * @since 10.0
   */
  @Nonnull
  @Deprecated
  @DeprecationInfo(value = "Use #getContentFolderUrls(ContentFolderScopes.production()) or #getContentFolderFiles(ContentFolderScopes.productionAndTest()")
  String[] getSourceRootUrls(boolean includingTests);

  /**
   * Passes all order entries in the module to the specified visitor.
   *
   * @param policy       the visitor to accept.
   * @param initialValue the default value to be returned by the visit process.
   * @return the value returned by the visitor.
   * @see OrderEntry#accept(consulo.ide.impl.idea.openapi.roots.RootPolicy, Object)
   */
  <R> R processOrder(RootPolicy<R> policy, R initialValue);

  /**
   * Returns {@link consulo.ide.impl.idea.openapi.roots.OrderEnumerator} instance which can be used to process order entries of the module (with or without dependencies) and
   * collect classes or source roots
   *
   * @return {@link consulo.ide.impl.idea.openapi.roots.OrderEnumerator} instance
   * @since 10.0
   */
  @Nonnull
  OrderEnumerator orderEntries();

  /**
   * Returns list of module names <i>this module</i> depends on.
   *
   * @return the list of module names this module depends on.
   */
  @Nonnull
  String[] getDependencyModuleNames();

  @Nullable
  <T extends ModuleExtension> T getExtension(Class<T> clazz);

  @Nullable
  <T extends ModuleExtension> T getExtension(@Nonnull String key);

  @Nullable
  <T extends ModuleExtension> T getExtensionWithoutCheck(Class<T> clazz);

  @Nullable
  <T extends ModuleExtension> T getExtensionWithoutCheck(@Nonnull String key);

  @Nonnull
  List<ModuleExtension> getExtensions();

  @Nonnull
  Module[] getModuleDependencies();

  @Nonnull
  Module[] getModuleDependencies(boolean includeTests);
}
