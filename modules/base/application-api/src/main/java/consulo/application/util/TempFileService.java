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
package consulo.application.util;

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ServiceAPI;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.IOException;
import java.nio.file.Path;

/**
 * @author VISTALL
 * @since 09/11/2022
 */
@ServiceAPI(ComponentScope.APPLICATION)
public interface TempFileService {
  @Nonnull
  default Path createTempDirectory(@Nonnull String prefix, @Nullable String suffix) throws IOException {
    return createTempDirectory(prefix, suffix, true);
  }

  @Nonnull
  default Path createTempDirectory(@Nonnull String prefix, @Nullable String suffix, boolean deleteOnExit) throws IOException {
    return createTempDirectory(getTempDirectory(), prefix, suffix, deleteOnExit);
  }

  @Nonnull
  default Path createTempDirectory(@Nonnull Path dir, @Nonnull String prefix, @Nullable String suffix) throws IOException {
    return createTempDirectory(dir, prefix, suffix, true);
  }

  @Nonnull
  Path createTempDirectory(@Nonnull Path dir, @Nonnull String prefix, @Nullable String suffix, boolean deleteOnExit) throws IOException;

  @Nonnull
  default Path createTempFile(@Nonnull String prefix, @Nullable String suffix) throws IOException {
    return createTempFile(prefix, suffix, false);
  }

  @Nonnull
  default Path createTempFile(@Nonnull String prefix, @Nullable String suffix, boolean deleteOnExit) throws IOException {
    return createTempFile(getTempDirectory(), prefix, suffix, true, deleteOnExit);
  }

  @Nonnull
  default Path createTempFile(Path dir, @Nonnull String prefix, @Nullable String suffix) throws IOException {
    return createTempFile(dir, prefix, suffix, true, true);
  }

  @Nonnull
  default Path createTempFile(Path dir, @Nonnull String prefix, @Nullable String suffix, boolean create) throws IOException {
    return createTempFile(dir, prefix, suffix, create, true);
  }

  @Nonnull
  Path createTempFile(Path dir, @Nonnull String prefix, @Nullable String suffix, boolean create, boolean deleteOnExit) throws IOException;

  @Nonnull
  Path getTempDirectory();
}
