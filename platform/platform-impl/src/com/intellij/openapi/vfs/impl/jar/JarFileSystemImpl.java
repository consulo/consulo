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
package com.intellij.openapi.vfs.impl.jar;

import com.intellij.openapi.components.ApplicationComponent;
import com.intellij.openapi.vfs.ArchiveFileSystem;
import com.intellij.openapi.vfs.JarFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.impl.archive.ArchiveHandler;
import com.intellij.openapi.vfs.impl.zip.ZipHandler;
import com.intellij.util.messages.MessageBus;
import org.consulo.vfs.ArchiveFileSystemBase;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;

public class JarFileSystemImpl extends ArchiveFileSystemBase implements JarFileSystem, ApplicationComponent {
  public JarFileSystemImpl(MessageBus bus) {
    super(bus);
  }

  @Override
  public ArchiveHandler createHandler(ArchiveFileSystem fileSystem, String path) {
    return new ZipHandler(fileSystem, path);
  }

  @Override
  @NotNull
  public String getComponentName() {
    return "JarFileSystem";
  }

  @Override
  public void initComponent() {
  }

  @Override
  public void disposeComponent() {
  }

  @Nullable
  public File getMirroredFile(@NotNull VirtualFile vFile) {
    VirtualFile jar = findByPathWithSeparator(vFile);
    final ArchiveHandler handler = jar != null ? getHandler(jar) : null;
    return handler != null ? handler.getMirrorFile(new File(vFile.getPath())) : null;
  }

  @Override
  @NotNull
  public String getProtocol() {
    return PROTOCOL;
  }
}
