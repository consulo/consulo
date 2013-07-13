/*
 * Copyright 2013 Consulo.org
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
package com.intellij.openapi.vfs.impl.archive;

import org.jetbrains.annotations.NotNull;

public class ArchiveHandlerEntry {
  private final boolean isDirectory;
  private final String shortName;
  private final ArchiveHandlerEntry parent;

  public ArchiveHandlerEntry(@NotNull String shortName, final ArchiveHandlerEntry parent, final boolean directory) {
    this.shortName = shortName;
    this.parent = parent;
    isDirectory = directory;
  }

 public ArchiveHandlerEntry getParent() {
   return parent;
 }

  public boolean isDirectory() {
    return isDirectory;
  }

  public String getShortName() {
    return shortName;
  }
}
