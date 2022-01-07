/*
 * Copyright 2013-2016 consulo.io
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
package consulo.packaging.elements;

import javax.annotation.Nonnull;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;

/**
 * @author VISTALL
 * @since 13.04.2016
 */
public interface ArchivePackageWriter<ArchiveObject> {
  @Nonnull
  ArchiveObject createArchiveObject(@Nonnull File tempFile) throws IOException;

  void addDirectory(@Nonnull ArchiveObject archiveObject, @Nonnull String relativePath) throws IOException;

  void addFile(@Nonnull ArchiveObject archiveObject, @Nonnull InputStream stream, @Nonnull String relativePath, long fileLength, long lastModified)
          throws IOException;

  void close(@Nonnull ArchiveObject archiveObject) throws IOException;
}
