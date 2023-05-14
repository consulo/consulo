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
package consulo.virtualFileSystem.archive;

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ExtensionAPI;

import jakarta.annotation.Nonnull;
import java.io.IOException;

/**
 * Provider for creating which used for creating {@link ArchiveFileSystem}
 *
 * @author VISTALL
 * @since 10-Aug-22
 */
@ExtensionAPI(ComponentScope.APPLICATION)
public interface ArchiveFileSystemProvider {
  @Nonnull
  String getProtocol();

  @Nonnull
  ArchiveFile createArchiveFile(@Nonnull String path) throws IOException;
}
