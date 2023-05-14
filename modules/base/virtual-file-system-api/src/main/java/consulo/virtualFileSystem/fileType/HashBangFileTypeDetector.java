/*
 * Copyright 2013-2023 consulo.io
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

import consulo.localize.LocalizeValue;
import consulo.util.io.ByteSequence;
import consulo.virtualFileSystem.VirtualFile;
import consulo.virtualFileSystem.internal.HashBangChecker;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import java.util.Collection;
import java.util.List;

/**
 * Consider using <code>hashBangs</code> attribute of <code>fileType</code>
 */
public abstract class HashBangFileTypeDetector implements FileTypeDetector {
  private final FileType myFileType;
  private final String myMarker;
  private final LocalizeValue myDescription;

  public HashBangFileTypeDetector(@Nonnull FileType fileType, @Nonnull String marker, @Nonnull LocalizeValue description) {
    myFileType = fileType;
    myMarker = marker;
    myDescription = description;
  }

  @Nullable
  @Override
  public FileType detect(@Nonnull VirtualFile file, @Nonnull ByteSequence firstBytes, @Nullable CharSequence firstCharsIfText) {
    if (HashBangChecker.isHashBangLine(firstCharsIfText, myMarker)) {
      return myFileType;
    }
    return null;
  }

  @Nullable
  @Override
  public Collection<? extends FileType> getDetectedFileTypes() {
    return List.of(myFileType);
  }

  @Nonnull
  public final LocalizeValue getDescription() {
    return myDescription;
  }

  @Nonnull
  public final String getMarker() {
    return myMarker;
  }

  @Nonnull
  public final FileType getFileType() {
    return myFileType;
  }

  @Override
  public int getDesiredContentPrefixLength() {
    // Maximum length of shebang varies for different OSes (https://www.in-ulm.de/~mascheck/various/shebang/#results).
    // On macOS, its 512.
    // On vast majority of Linux systems, a restriction of 127 bytes of shebang length is compiled into kernel.
    // See "#define BINPRM_BUF_SIZE 128" in /usr/include/linux/binfmts.h (127 + '0' as the string terminator).

    // Let's limit its maximum length to 256 which allows file type detection for most cases.
    // In future, it can be reduced for performance sake.
    return 256;
  }

  @Override
  public int getVersion() {
    return 1;
  }
}
