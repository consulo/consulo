/*
 * Copyright 2000-2016 JetBrains s.r.o.
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

import com.intellij.openapi.vfs.StandardFileSystems;
import consulo.vfs.impl.archive.ArchiveFile;
import consulo.vfs.impl.archive.ArchiveFileSystemBase;
import consulo.vfs.impl.zip.ZipArchiveFile;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;

public class JarFileSystemImpl extends ArchiveFileSystemBase {
  public JarFileSystemImpl() {
    super(StandardFileSystems.JAR_PROTOCOL);
  }

  @NotNull
  @Override
  public ArchiveFile createArchiveFile(@NotNull String filePath) throws IOException{
    return new ZipArchiveFile(filePath);
  }
}