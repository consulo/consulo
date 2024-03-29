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
package consulo.virtualFileSystem.internal;

import consulo.util.io.FileAttributes;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import java.io.IOException;

public interface FileSystemMediator {
  @Nullable
  FileAttributes getAttributes(@Nonnull String path) throws IOException;

  @Nullable
  String resolveSymLink(@Nonnull String path) throws IOException;

  boolean clonePermissions(@Nonnull String source, @Nonnull String target, boolean execOnly) throws IOException;
}
