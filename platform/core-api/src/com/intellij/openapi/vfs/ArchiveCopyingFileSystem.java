/*
 * Copyright 2000-2012 JetBrains s.r.o.
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
package com.intellij.openapi.vfs;

import org.jetbrains.annotations.NotNull;
import consulo.annotations.DeprecationInfo;

import java.io.File;

/**
 * @author yole
 */
public interface ArchiveCopyingFileSystem {
  @Deprecated
  @DeprecationInfo(value = "Use #addNoCopyArchiveForPath(String)", until = "2.0")
  void setNoCopyJarForPath(String pathInJar);

  default void addNoCopyArchiveForPath(@NotNull String path) {
    setNoCopyJarForPath(path);
  }

  @Deprecated
  @DeprecationInfo(value = "Use #isMakeCopyForArchive(File)", until = "2.0")
  boolean isMakeCopyOfJar(File originalFile);

  default boolean isMakeCopyForArchive(@NotNull File originalFile) {
    return isMakeCopyOfJar(originalFile);
  }
}
