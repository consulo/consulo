/*
 * Copyright 2013 Consulo.org
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
package com.intellij.openapi.vfs.impl.zip;

import com.intellij.ide.highlighter.ZipArchiveFileType;
import com.intellij.openapi.components.ApplicationComponent;
import com.intellij.openapi.vfs.ArchiveFileSystem;
import com.intellij.openapi.vfs.impl.archive.ArchiveHandler;
import com.intellij.util.messages.MessageBus;
import org.consulo.vfs.ArchiveFileSystemBase;
import org.jetbrains.annotations.NotNull;

/**
 * @author VISTALL
 * @since 16:43/14.07.13
 */
public class ZipFileSystemImpl extends ArchiveFileSystemBase implements ApplicationComponent {
  public ZipFileSystemImpl(MessageBus bus) {
    super(bus);
  }

  @Override
  public ArchiveHandler createHandler(ArchiveFileSystem fileSystem, String path) {
    return new ZipHandler(fileSystem, path);
  }

  @Override
  public void setNoCopyJarForPath(String pathInJar) {
  }

  @NotNull
  @Override
  public String getProtocol() {
    return ZipArchiveFileType.PROTOCOL;
  }

  @Override
  public void initComponent() {
  }

  @Override
  public void disposeComponent() {
  }

  @NotNull
  @Override
  public String getComponentName() {
    return getClass().getSimpleName();
  }
}
