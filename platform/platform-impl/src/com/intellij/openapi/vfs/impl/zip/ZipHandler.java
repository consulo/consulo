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

/*
 * @author max
 */
package com.intellij.openapi.vfs.impl.zip;

import com.intellij.openapi.vfs.ArchiveFile;
import com.intellij.openapi.vfs.ArchiveFileSystem;
import com.intellij.openapi.vfs.impl.archive.ArchiveHandlerBase;
import consulo.lombok.annotations.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.IOException;
import java.util.zip.ZipFile;

@Logger
public class ZipHandler extends ArchiveHandlerBase {

  public ZipHandler(@NotNull ArchiveFileSystem fileSystem, @NotNull String path) {
    super(fileSystem, path);
  }

  @Override
  @Nullable
  protected ArchiveFile createArchiveFile() {
    final File originalFile = getOriginalFile();
    try {
      @SuppressWarnings("IOResourceOpenedButNotSafelyClosed")
      final ZipFile zipFile = new ZipFile(getMirrorFile(originalFile));

      return new ZipArchiveFile(zipFile);
    }
    catch (IOException e) {
      ZipHandler.LOGGER.warn(e.getMessage() + ": " + originalFile.getPath(), e);
      return null;
    }
  }
}
