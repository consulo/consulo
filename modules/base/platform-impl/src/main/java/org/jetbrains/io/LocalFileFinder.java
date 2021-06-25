// Copyright 2000-2021 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package org.jetbrains.io;

import com.intellij.openapi.util.SystemInfo;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.File;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

public final class LocalFileFinder {
  private static record Result(boolean value,long time) {
  }

  // if java.io.File.exists() takes more time than this timeout we assume that this is network drive and do not ping it any more
  private static final int FILE_EXISTS_MAX_TIMEOUT_MILLIS = 10;

  private static final Map<Character, Result> windowsDrivesMap = new ConcurrentHashMap<>();

  private LocalFileFinder() {
  }

  /**
   * Behavior is not predictable and result is not guaranteed:
   * 1) result of check cached, but cache entry invalidated on timeout (5 min after write), not on actual change of drive status.
   * 2) even if drive exists, it could be not used due to 10 ms threshold.
   * Method is not generic and is not suitable for all.
   */
  @Nullable
  public static VirtualFile findFile(@Nonnull String path) {
    if (windowsDriveExists(path)) {
      return LocalFileSystem.getInstance().findFileByPath(path);
    }
    return null;
  }

  public static boolean windowsDriveExists(@Nonnull String path) {
    if (!SystemInfo.isWindows) {
      return true;
    }

    if (!FileUtil.isWindowsAbsolutePath(path)) {
      return false;
    }

    final char driveLetter = Character.toUpperCase(path.charAt(0));
    Boolean driveExists = null;

    final long t0 = System.currentTimeMillis();

    Result result = windowsDrivesMap.get(driveLetter);
    if (result != null) {
      // 5 min expired
      if ((t0 - result.time()) > TimeUnit.MINUTES.toMillis(5)) {
        windowsDrivesMap.remove(driveLetter);
      }
      else {
        driveExists = result.value();
      }
    }

    if (driveExists != null) {
      return driveExists;
    }
    else {
      boolean exists = new File(driveLetter + ":" + File.separator).exists();
      if (System.currentTimeMillis() - t0 > FILE_EXISTS_MAX_TIMEOUT_MILLIS) {
        exists = false; // may be a slow network drive
      }

      windowsDrivesMap.putIfAbsent(driveLetter, new Result(exists, t0));
      return exists;
    }
  }
}