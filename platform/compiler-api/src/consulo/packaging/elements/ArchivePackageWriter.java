/*
 * Copyright 2013-2016 must-be.org
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

import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;

/**
 * @author VISTALL
 * @since 13.04.2016
 */
public interface ArchivePackageWriter<ArchiveObject> {
  @NotNull
  ArchiveObject createArchiveObject(@NotNull File tempFile) throws IOException;

  void addDirectory(@NotNull ArchiveObject archiveObject, @NotNull String relativePath) throws IOException;

  void addFile(@NotNull ArchiveObject archiveObject, @NotNull InputStream stream, @NotNull String relativePath, long fileLength, long lastModified)
          throws IOException;

  void close(@NotNull ArchiveObject archiveObject) throws IOException;
}
