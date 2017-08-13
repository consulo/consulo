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
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

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
  @NotNull
  ContentEntry addContentEntry(@NotNull VirtualFile root);

  /**
   * Adds the specified directory as a content root.
   *
   * @param url root of a content
   * @return new content entry
   */
  @NotNull
  ContentEntry addContentEntry(@NotNull String url);

  /**
   * Remove the specified content root.
   *
   * @param entry the content root to remove.
   */
  void removeContentEntry(@NotNull ContentEntry entry);

  /**
   * Appends an order entry to the classpath.
   *
   * @param orderEntry the order entry to add.
   */
  void addOrderEntry(@NotNull OrderEntry orderEntry);

  /**
   * Creates an entry for a given library and adds it to order
   *
   * @param library the library for which the entry is created.
   * @return newly created order entry for the library
   */
  @NotNull
  LibraryOrderEntry addLibraryEntry(@NotNull Library library);

  @NotNull
  ModuleExtensionWithSdkOrderEntry addModuleExtensionSdkEntry(@NotNull ModuleExtensionWithSdk<?> moduleExtension);

  /**
   * Adds an entry for invalid library.
   *
   * @param name
   * @param level
   * @return
   */
  @NotNull
  LibraryOrderEntry addInvalidLibrary(@NotNull @NonNls String name, @NotNull String level);

  @NotNull
  ModuleOrderEntry addModuleOrderEntry(@NotNull Module module);

  @NotNull
  ModuleOrderEntry addInvalidModuleEntry(@NotNull String name);

  @Nullable
  LibraryOrderEntry findLibraryOrderEntry(@NotNull Library library);

  @Nullable
  ModuleExtensionWithSdkOrderEntry findModuleExtensionSdkEntry(@NotNull ModuleExtension extension);

  /**
   * Removes order entry from an order.
   *
   * @param orderEntry
   */
  void removeOrderEntry(@NotNull OrderEntry orderEntry);

  /**
   * @param newOrder
   */
  void rearrangeOrderEntries(@NotNull OrderEntry[] newOrder);

  <T extends OrderEntry> void replaceEntryOfType(Class<T> entryClass, T entry);

  /**
   * Returns library table with module libraries.<br>
   * <b>Note:</b> returned library table does not support listeners.
   *
   * @return library table to be modified
   */
  @NotNull
  LibraryTable getModuleLibraryTable();
}
