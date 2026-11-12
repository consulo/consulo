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
package consulo.virtualFileSystem.impl.internal;

import consulo.virtualFileSystem.VirtualFile;
import consulo.virtualFileSystem.VirtualFileSystemWithMacroSupport;
import consulo.virtualFileSystem.archive.ArchiveFile;
import consulo.virtualFileSystem.archive.ArchiveFileSystemProvider;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;

/**
 * @author VISTALL
 * @since 10-Aug-22
 */
@SuppressWarnings("ExtensionImplIsNotAnnotated")
public class ArchiveFileSystemByProvider extends ArchiveFileSystemViaArchiveFile implements VirtualFileSystemWithMacroSupport {
  private final ArchiveFileSystemProvider myProvider;

  public ArchiveFileSystemByProvider(ArchiveFileSystemProvider provider) {
    this(provider, false);
  }

  private ArchiveFileSystemByProvider(ArchiveFileSystemProvider provider, boolean platformHosted) {
    super(provider.getProtocol(), platformHosted);
    myProvider = provider;
  }

  @Override
  protected ArchiveFileSystemViaArchiveFile newPlatformFileSystem() {
    return new ArchiveFileSystemByProvider(myProvider, true);
  }

  
  @Override
  public ArchiveFile createArchiveFile(Path path) throws IOException {
    return myProvider.createArchiveFile(path);
  }

  @Override
  protected boolean isCorrectFileType(VirtualFile host) {
    if (host.isDirectory()) {
      Path path = host.getFileSystem().getNioPath(host);
      return path != null && myProvider.isArchiveDirectory(path);
    }
    return super.isCorrectFileType(host);
  }

  @Override
  public boolean isMakeCopyOfJar(File originalJar) {
    // an archive which is a directory is read where it is
    return !originalJar.isDirectory() && super.isMakeCopyOfJar(originalJar);
  }
}
