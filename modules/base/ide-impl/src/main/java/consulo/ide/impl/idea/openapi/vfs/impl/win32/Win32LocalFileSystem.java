/*
 * Copyright 2000-2016 JetBrains s.r.o.
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
package consulo.ide.impl.idea.openapi.vfs.impl.win32;

import consulo.util.io.FileAttributes;
import consulo.virtualFileSystem.impl.internal.windows.WindowsFileSystemHelper;
import consulo.virtualFileSystem.VirtualFile;
import consulo.ide.impl.idea.openapi.vfs.impl.local.LocalFileSystemBase;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.util.Collection;
import java.util.Set;

/**
 * @author Dmitry Avdeev
 */
public class Win32LocalFileSystem extends LocalFileSystemBase {
  public static boolean isAvailable() {
    return WindowsFileSystemHelper.isAvailable();
  }

  private static final ThreadLocal<Win32LocalFileSystem> THREAD_LOCAL = new ThreadLocal<Win32LocalFileSystem>() {
    @Override
    protected Win32LocalFileSystem initialValue() {
      return new Win32LocalFileSystem();
    }
  };

  public static Win32LocalFileSystem getWin32Instance() {
    if (!isAvailable()) throw new RuntimeException("Native filesystem for Windows is not loaded");
    Win32LocalFileSystem fileSystem = THREAD_LOCAL.get();
    fileSystem.myFsCache.clearCache();
    return fileSystem;
  }

  private final Win32FsCache myFsCache = new Win32FsCache();

  private Win32LocalFileSystem() { }

  @Nonnull
  @Override
  public String[] list(@Nonnull VirtualFile file) {
    return myFsCache.list(file);
  }

  @Override
  public FileAttributes getAttributes(@Nonnull VirtualFile file) {
    return myFsCache.getAttributes(file);
  }

  @Nonnull
  @Override
  public Set<WatchRequest> replaceWatchedRoots(@Nonnull Collection<WatchRequest> watchRequests,
                                               @Nullable Collection<String> recursiveRoots,
                                               @Nullable Collection<String> flatRoots) {
    throw new UnsupportedOperationException();
  }
}
