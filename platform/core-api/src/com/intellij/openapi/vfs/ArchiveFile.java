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

import com.intellij.util.containers.EmptyIterator;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.io.InputStream;
import java.util.Iterator;

/**
 * Encapsulates operations with archive file
 */
public interface ArchiveFile {
  ArchiveFile EMPTY = new ArchiveFile() {
    @Nullable
    @Override
    public ArchiveEntry getEntry(String name) {
      return null;
    }

    @Nullable
    @Override
    public InputStream getInputStream(ArchiveEntry entry) throws IOException {
      return null;
    }

    @NotNull
    @Override
    public Iterator<? extends ArchiveEntry> entries() {
      return EmptyIterator.getInstance();
    }

    @Override
    public int getSize() {
      return 0;
    }
  };

  @Nullable
  ArchiveEntry getEntry(String name);

  @Nullable
  InputStream getInputStream(ArchiveEntry entry) throws IOException;

  @NotNull
  Iterator<? extends ArchiveEntry> entries();

  int getSize();

  default void close() {
  }
}
