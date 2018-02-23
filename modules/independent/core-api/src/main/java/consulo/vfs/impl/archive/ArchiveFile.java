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
package consulo.vfs.impl.archive;

import com.intellij.util.containers.EmptyIterator;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import java.io.IOException;
import java.io.InputStream;
import java.util.Iterator;

public interface ArchiveFile {
  ArchiveFile EMPTY = new ArchiveFile() {
    @Nonnull
    @Override
    public String getName() {
      throw new UnsupportedOperationException();
    }

    @Nullable
    @Override
    public ArchiveEntry getEntry(String name) {
      return null;
    }

    @Nullable
    @Override
    public InputStream getInputStream(@Nonnull ArchiveEntry entry) throws IOException {
      return null;
    }

    @Nonnull
    @Override
    public Iterator<? extends ArchiveEntry> entries() {
      return EmptyIterator.getInstance();
    }

    @Override
    public int getSize() {
      return 0;
    }
  };

  @Nonnull
  String getName();

  @Nullable
  ArchiveEntry getEntry(String name);

  @Nullable
  InputStream getInputStream(@Nonnull ArchiveEntry entry) throws IOException;

  @Nonnull
  Iterator<? extends ArchiveEntry> entries();

  int getSize();

  default void close() {
  }
}
