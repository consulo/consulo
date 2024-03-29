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
package consulo.module.content.layer.orderEntry;

import consulo.content.OrderRootType;
import consulo.module.Module;
import consulo.module.content.layer.ModuleRootLayer;
import consulo.module.content.layer.Synthetic;
import consulo.virtualFileSystem.VirtualFile;
import consulo.virtualFileSystem.VirtualFileManager;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

/**
 * Represents an entry in the classpath of a module (as shown in the "Order/Export" page
 * of the module configuration dialog).
 *
 * @author dsl
 */
public interface OrderEntry extends Synthetic, Comparable<OrderEntry> {
  /**
   * The empty array of order entries which can be reused to avoid unnecessary allocations.
   */
  OrderEntry[] EMPTY_ARRAY = new OrderEntry[0];

  /**
   * Return provider of ths order entry
   * @return provider
   */
  @Nonnull
  OrderEntryType<?> getType();

  /**
   * Returns list of root {@link VirtualFile}s of given type for this entry.
   * Those files should be traversed in order they are returned in.
   * Note that actual OrderEntry (as seen in UI) may also contain invalid roots.
   * If you want to get list of all roots, use {@link #getUrls(OrderRootType)} method. <br>
   *
   * Note that list of roots is project dependent.
   *
   * @param type  required root type.
   * @return list of virtual files.
   * @see #getUrls(OrderRootType)
   */
  @Nonnull
  VirtualFile[] getFiles(OrderRootType type);

  /**
   * Returns list of roots of given type for this entry. To validate returned roots,
   * use <code>{@link VirtualFileManager#findFileByUrl(java.lang.String)}</code> <br>
   *
   * Note that list of roots is project-dependent.
   *
   * @param rootType the type of roots which should be returned.
   * @return the list of roots of the specified type.
   */
  @Nonnull
  String[] getUrls(OrderRootType rootType);

  /**
   * Returns the user-visible name of this OrderEntry.
   *
   * @return name of this OrderEntry to be shown to user.
   */
  @Nonnull
  String getPresentableName();

  /**
   * Checks whether this order entry is invalid for some reason. Note that entry being valid
   * does not necessarily mean that all its roots are valid.
   *
   * @return true if entry is valid, false otherwise.
   */
  boolean isValid();

  /**
   * Returns the module to which the entry belongs.
   *
   * @return the module instance.
   */
  @Nonnull
  Module getOwnerModule();

  /**
   * Return owner module root layer
   */
  @Nonnull
  ModuleRootLayer getModuleRootLayer();

  /**
   * Accepts the specified order entries visitor.
   *
   * @param policy       the visitor to accept.
   * @param initialValue the default value to be returned by the visit process.
   * @return the value returned by the visitor.
   */
  <R> R accept(RootPolicy<R> policy, @Nullable R initialValue);

  boolean isEquivalentTo(@Nonnull OrderEntry other);
}
