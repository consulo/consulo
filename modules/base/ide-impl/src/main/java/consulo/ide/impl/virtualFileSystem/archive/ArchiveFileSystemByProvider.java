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
package consulo.ide.impl.virtualFileSystem.archive;

import consulo.virtualFileSystem.VirtualFileSystemWithMacroSupport;
import consulo.virtualFileSystem.archive.ArchiveFile;
import consulo.virtualFileSystem.archive.ArchiveFileSystemProvider;

import jakarta.annotation.Nonnull;
import java.io.IOException;

/**
 * @author VISTALL
 * @since 10-Aug-22
 */
@SuppressWarnings("ExtensionImplIsNotAnnotatedInspection")
public class ArchiveFileSystemByProvider extends ArchiveFileSystemBase implements VirtualFileSystemWithMacroSupport {
  private final ArchiveFileSystemProvider myProvider;

  public ArchiveFileSystemByProvider(ArchiveFileSystemProvider provider) {
    super(provider.getProtocol());
    myProvider = provider;
  }

  @Nonnull
  @Override
  public ArchiveFile createArchiveFile(@Nonnull String filePath) throws IOException {
    return myProvider.createArchiveFile(filePath);
  }
}
