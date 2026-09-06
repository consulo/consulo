// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.virtualFileSystem.internal;

import consulo.virtualFileSystem.fileType.FileType;
import consulo.virtualFileSystem.fileType.UnknownFileType;
import org.jspecify.annotations.Nullable;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.function.Supplier;

public class CachedFileType {
  private static final ConcurrentMap<FileType, CachedFileType> ourInterner = new ConcurrentHashMap<>();

  private @Nullable FileType fileType;

  private CachedFileType(FileType fileType) {
    this.fileType = fileType;
  }

  public @Nullable FileType getUpToDateOrNull() {
    return fileType;
  }

  public static CachedFileType forType(FileType fileType) {
    CachedFileType cached = ourInterner.get(fileType);
    return cached != null ? cached : computeSynchronized(fileType);
  }

  private static CachedFileType computeSynchronized(FileType fileType) {
    synchronized (ourInterner) {
      return ourInterner.computeIfAbsent(fileType, CachedFileType::new);
    }
  }

  /**
   * @return result that returns true if no files changed their types since method invocation
   */
  public static Supplier<Boolean> getFileTypeChangeChecker() {
    CachedFileType type = forType(UnknownFileType.INSTANCE);
    return () -> type.getUpToDateOrNull() != null;
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
