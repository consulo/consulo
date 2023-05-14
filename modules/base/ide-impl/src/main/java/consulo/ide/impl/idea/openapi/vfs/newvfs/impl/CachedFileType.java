// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.ide.impl.idea.openapi.vfs.newvfs.impl;

import consulo.ide.impl.idea.util.containers.ContainerUtil;
import consulo.virtualFileSystem.fileType.FileType;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import java.util.concurrent.ConcurrentMap;

//@ApiStatus.Internal
public class CachedFileType {
  private static final ConcurrentMap<FileType, CachedFileType> ourInterner = ContainerUtil.newConcurrentMap();

  @Nullable
  private FileType fileType;

  private CachedFileType(@Nonnull FileType fileType) {
    this.fileType = fileType;
  }

  @Nullable
  FileType getUpToDateOrNull() {
    return fileType;
  }

  static CachedFileType forType(@Nonnull FileType fileType) {
    CachedFileType cached = ourInterner.get(fileType);
    return cached != null ? cached : computeSynchronized(fileType);
  }

  private static CachedFileType computeSynchronized(FileType fileType) {
    synchronized (ourInterner) {
      return ourInterner.computeIfAbsent(fileType, CachedFileType::new);
    }
  }

  public static void clearCache() {
    synchronized (ourInterner) {
      for (CachedFileType value : ourInterner.values()) {
        // clear references to file types to aid plugin unloading
        value.fileType = null;
      }
      ourInterner.clear();
    }
  }

}
