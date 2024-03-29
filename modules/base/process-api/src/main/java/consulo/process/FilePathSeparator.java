/*
 * Copyright 2000-2013 JetBrains s.r.o.
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
package consulo.process;

import consulo.platform.Platform;

import jakarta.annotation.Nonnull;

public enum FilePathSeparator {
  WINDOWS('\\', ';'), UNIX('/', ':');

  public final char fileSeparator;
  public final char pathSeparator;

  FilePathSeparator(char fileSeparator, char pathSeparator) {
    this.fileSeparator = fileSeparator;
    this.pathSeparator = pathSeparator;
  }

  @Nonnull
  public static FilePathSeparator current() {
    return Platform.current().os().isWindows() ? WINDOWS : UNIX;
  }
}
