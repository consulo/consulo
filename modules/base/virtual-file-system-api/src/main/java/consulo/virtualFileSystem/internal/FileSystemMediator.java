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

import org.jspecify.annotations.Nullable;
import java.io.IOException;

public interface FileSystemMediator {
  @Nullable
  FileAttributes getAttributes(String path) throws IOException;

  @Nullable
  String resolveSymLink(String path) throws IOException;

  boolean clonePermissions(String source, String target, boolean execOnly) throws IOException;
}
