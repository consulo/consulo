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
package com.intellij.openapi.fileTypes;

import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.util.ArrayFactory;
import consulo.annotations.DeprecationInfo;
import consulo.util.pointers.Named;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;

public interface FileType extends Named {
  public static final FileType[] EMPTY_ARRAY = new FileType[0];

  public static ArrayFactory<FileType> ARRAY_FACTORY = new ArrayFactory<FileType>() {
    @NotNull
    @Override
    public FileType[] create(int count) {
      return count == 0 ? EMPTY_ARRAY : new FileType[count];
    }
  };
  /**
   * Returns the id of the file type. The name must be unique among all file types registered in the system.
   *
   * @return The file type id.
   */
  @NotNull
  @NonNls
  default String getId() {
    return getName();
  }

  /**
   * Returns the name of the file type. The name must be unique among all file types registered in the system.
   *
   * @return The file type name.
   */
  @Override
  @NotNull
  @NonNls
  @Deprecated
  @DeprecationInfo(value = "Use #getId(), and implement #getId()")
  default String getName() {
    return getId();
  }

  /**
   * Returns the user-readable description of the file type.
   *
   * @return The file type description.
   */

  @NotNull
  String getDescription();

  /**
   * Returns the default extension for files of the type.
   *
   * @return The extension, not including the leading '.'.
   */

  @NotNull
  @NonNls
  String getDefaultExtension();

  /**
   * Returns the icon used for showing files of the type.
   *
   * @return The icon instance, or null if no icon should be shown.
   */

  @Nullable
  Icon getIcon();

  /**
   * Returns true if files of the specified type contain binary data. Used for source control, to-do items scanning and other purposes.
   *
   * @return true if the file is binary, false if the file is plain text.
   */
  boolean isBinary();

  /**
   * Returns true if the specified file type is read-only. Read-only file types are not shown in the "File Types" settings dialog,
   * and users cannot change the extensions associated with the file type.
   *
   * @return true if the file type is read-only, false otherwise.
   */
  default boolean isReadOnly() {
    return false;
  }

  /**
   * Returns the character set for the specified file.
   *
   * @param file    The file for which the character set is requested.
   * @param content file content as byte array
   * @return The character set name, in the format supported by {@link java.nio.charset.Charset} class.
   */
  @Nullable
  @NonNls
  default String getCharset(@NotNull VirtualFile file, final byte[] content) {
    return null;
  }
}
