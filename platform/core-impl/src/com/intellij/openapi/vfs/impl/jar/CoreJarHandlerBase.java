/*
 * Copyright 2013 must-be.org
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
package com.intellij.openapi.vfs.impl.jar;

import com.intellij.openapi.vfs.ArchiveFile;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.impl.archive.CoreArchiveHandler;
import com.intellij.openapi.vfs.impl.zip.ZipArchiveFile;
import consulo.lombok.annotations.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.IOException;
import java.util.zip.ZipFile;

/**
 * @author VISTALL
 * @since 18:38/13.07.13
 */
@Logger
public class CoreJarHandlerBase extends CoreArchiveHandler {
  public CoreJarHandlerBase(@NotNull String path) {
    super(path);
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
      CoreJarHandlerBase.LOGGER.warn(e.getMessage() + ": " + originalFile.getPath(), e);
      return null;
    }
  }

  @Override
  public VirtualFile markDirty() {
    return null;
  }

  @Override
  public void refreshLocalFileForJar() {
  }
}
