/*
 * Copyright 2013-2022 consulo.io
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
package consulo.virtualFileSystem.fileType;

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.Extension;
import consulo.component.extension.ExtensionPointName;
import consulo.util.io.ByteSequence;
import consulo.virtualFileSystem.VirtualFile;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Collection;

/**
 * Pluggable file type detector by content
 */
@Extension(ComponentScope.APPLICATION)
public interface FileTypeDetector {
  ExtensionPointName<FileTypeDetector> EP_NAME = ExtensionPointName.create(FileTypeDetector.class);

  /**
   * Detects file type by its content
   *
   * @param file             to analyze
   * @param firstBytes       of the file for identifying its file type
   * @param firstCharsIfText - characters, converted from first bytes parameter if the file content was determined to be text, or null otherwise
   * @return detected file type, or null if was unable to detect
   */
  @Nullable
  FileType detect(@Nonnull VirtualFile file, @Nonnull ByteSequence firstBytes, @Nullable CharSequence firstCharsIfText);

  /**
   * Returns the file type that this detector is capable of detecting, or null if it can detect
   * multiple file types.
   */
  @Nullable
  default Collection<? extends FileType> getDetectedFileTypes() {
    return null;
  }

  /**
   * Defines how much content is required for this detector to detect file type reliably. At least such amount of bytes
   * will be passed to {@link #detect(VirtualFile, ByteSequence, CharSequence)} if present.
   *
   * @return number of first bytes to be given
   */
  default int getDesiredContentPrefixLength() {
    return 1024;
  }

  int getVersion();
}