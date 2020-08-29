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
package consulo.roots;

import com.intellij.openapi.module.Module;
import com.intellij.openapi.roots.*;
import com.intellij.openapi.roots.libraries.Library;
import com.intellij.openapi.roots.libraries.LibraryTable;
import com.intellij.openapi.vfs.VirtualFile;
import consulo.module.extension.ModuleExtension;
import consulo.module.extension.ModuleExtensionWithSdk;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * @author VISTALL
 * @since 29.07.14
 */
public interface ModifiableModuleRootLayer extends ModuleRootLayer {

  /**
   * Adds the specified directory as a content root.
   *
   * @param root root of a content
   * @return new content entry
   */
  @Nonnull
  ContentEntry addContentEntry(@Nonnull VirtualFile root);

  /**
   * Adds the specified directory as a content root.
   *
   * @param url root of a content
   * @return new content entry
   */
  @Nonnull
  ContentEntry addContentEntry(@Nonnull String url);

  /**
   * Adds file as single content entry. Can used only when module don't have base dir
   *
   * @param file target file
   * @return new content entry
   */
  @Nonnull
  default ContentEntry addSingleContentEntry(@Nonnull VirtualFile file) {
    return addContentEntry(file);
  }

  /**
   * Adds file as single content entry. Can used only when module don't have base dir   *
   *
   * @param url url of file
   * @return new content entry
   */
  @Nonnull
  default ContentEntry addSingleContentEntry(@Nonnull String url) {
    return addContentEntry(url);
  }

  /**
   * Remove the specified content root.
   *
   * @param entry the content root to remove.
   */
  void removeContentEntry(@Nonnull ContentEntry entry);

  /**
   * Appends an order entry to the classpath.
   *
   * @param orderEntry the order entry to add.
   */
  void addOrderEntry(@Nonnull OrderEntry orderEntry);

  /**
   * Creates an entry for a given library and adds it to order
   *
   * @param library the library for which the entry is created.
   * @return newly created order entry for the library
   */
  @Nonnull
  LibraryOrderEntry addLibraryEntry(@Nonnull Library library);

  @Nonnull
  ModuleExtensionWithSdkOrderEntry addModuleExtensionSdkEntry(@Nonnull ModuleExtensionWithSdk<?> moduleExtension);

  /**
   * Adds an entry for invalid library.
   *
   * @param name
   * @param level
   * @return
   */
  @Nonnull
  LibraryOrderEntry addInvalidLibrary(@Nonnull String name, @Nonnull String level);

  @Nonnull
  ModuleOrderEntry addModuleOrderEntry(@Nonnull Module module);

  @Nonnull
  ModuleOrderEntry addInvalidModuleEntry(@Nonnull String name);

  @Nullable
  LibraryOrderEntry findLibraryOrderEntry(@Nonnull Library library);

  @Nullable
  ModuleExtensionWithSdkOrderEntry findModuleExtensionSdkEntry(@Nonnull ModuleExtension extension);

  /**
   * Removes order entry from an order.
   *
   * @param orderEntry
   */
  void removeOrderEntry(@Nonnull OrderEntry orderEntry);

  /**
   * @param newOrder
   */
  void rearrangeOrderEntries(@Nonnull OrderEntry[] newOrder);

  <T extends OrderEntry> void replaceEntryOfType(Class<T> entryClass, T entry);

  /**
   * Returns library table with module libraries.<br>
   * <b>Note:</b> returned library table does not support listeners.
   *
   * @return library table to be modified
   */
  @Nonnull
  LibraryTable getModuleLibraryTable();
}
