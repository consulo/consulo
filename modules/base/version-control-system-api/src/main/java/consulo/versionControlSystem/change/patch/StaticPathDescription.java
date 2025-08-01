/*
 * Copyright 2000-2010 JetBrains s.r.o.
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
package consulo.versionControlSystem.change.patch;

import jakarta.annotation.Nonnull;

class StaticPathDescription implements PathDescription {
  private final String myPath;
  private final boolean myIsDirectory;
  private final long myLastModified;

  StaticPathDescription(boolean isDirectory, long lastModified, String path) {
    myIsDirectory = isDirectory;
    myLastModified = lastModified;
    myPath = path;
  }

  @Override
  @Nonnull
  public String getPath() {
    return myPath;
  }

  @Override
  public boolean isDirectory() {
    return myIsDirectory;
  }

  @Override
  public long lastModified() {
    return myLastModified;
  }
}
