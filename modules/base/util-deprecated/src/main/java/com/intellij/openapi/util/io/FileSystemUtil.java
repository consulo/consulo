/*
 * Copyright 2000-2015 JetBrains s.r.o.
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
package com.intellij.openapi.util.io;

import com.intellij.openapi.util.SystemInfo;
import consulo.logging.Logger;
import org.jetbrains.annotations.TestOnly;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.File;
import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.DosFileAttributes;
import java.nio.file.attribute.PosixFileAttributes;
import java.nio.file.attribute.PosixFilePermission;
import java.util.EnumSet;
import java.util.Set;

/**
 * @version 11.1
 */
public final class FileSystemUtil {

  private static final Logger LOG = Logger.getInstance(FileSystemUtil.class);

  public static interface Mediator {
    @Nullable
    FileAttributes getAttributes(@Nonnull String path) throws IOException;

    @Nullable
    String resolveSymLink(@Nonnull String path) throws IOException;

    boolean clonePermissions(@Nonnull String source, @Nonnull String target, boolean execOnly) throws IOException;
  }

  @Nonnull
  private static Mediator ourMediator = Nio2MediatorImpl.ourInstance;

  private static boolean ourLocked;

  public static void setMediatorLock(@Nullable Mediator newMediator) {
    if (ourLocked) {
      throw new IllegalArgumentException("locked");
    }

    if (newMediator != null) {
      ourMediator = newMediator;
    }

    ourLocked = true;
    LOG.info("Using file system mediator: " + getMediatorName(ourMediator));
  }

  @Nullable
  public static FileAttributes getAttributes(@Nonnull String path) {
    try {
      return ourMediator.getAttributes(path);
    }
    catch (Exception e) {
      LOG.warn(e);
    }
    return null;
  }

  @Nullable
  public static FileAttributes getAttributes(@Nonnull File file) {
    return getAttributes(file.getPath());
  }

  public static long lastModified(@Nonnull File file) {
    FileAttributes attributes = getAttributes(file);
    return attributes != null ? attributes.lastModified : 0;
  }

  /**
   * Checks if a last element in the path is a symlink.
   */
  public static boolean isSymLink(@Nonnull String path) {
    if (SystemInfo.areSymLinksSupported) {
      final FileAttributes attributes = getAttributes(path);
      return attributes != null && attributes.isSymLink();
    }
    return false;
  }

  /**
   * Checks if a last element in the path is a symlink.
   */
  public static boolean isSymLink(@Nonnull File file) {
    return isSymLink(file.getAbsolutePath());
  }

  @Nullable
  public static String resolveSymLink(@Nonnull String path) {
    try {
      final String realPath = ourMediator.resolveSymLink(path);
      if (realPath != null && new File(realPath).exists()) {
        return realPath;
      }
    }
    catch (Exception e) {
      LOG.warn(e);
    }
    return null;
  }

  @Nullable
  public static String resolveSymLink(@Nonnull File file) {
    return resolveSymLink(file.getAbsolutePath());
  }

  /**
   * Gives the second file permissions of the first one if possible; returns true if succeed.
   * Will do nothing on Windows.
   */
  public static boolean clonePermissions(@Nonnull String source, @Nonnull String target) {
    try {
      return ourMediator.clonePermissions(source, target, false);
    }
    catch (Exception e) {
      LOG.warn(e);
      return false;
    }
  }

  /**
   * Gives the second file permissions to execute of the first one if possible; returns true if succeed.
   * Will do nothing on Windows.
   */
  public static boolean clonePermissionsToExecute(@Nonnull String source, @Nonnull String target) {
    try {
      return ourMediator.clonePermissions(source, target, true);
    }
    catch (Exception e) {
      LOG.warn(e);
      return false;
    }
  }


  private static class Nio2MediatorImpl implements Mediator {
    private static final Nio2MediatorImpl ourInstance = new Nio2MediatorImpl();

    private final LinkOption[] myNoFollowLinkOptions = {LinkOption.NOFOLLOW_LINKS};
    private final PosixFilePermission[] myExecPermissions = {PosixFilePermission.OWNER_EXECUTE, PosixFilePermission.GROUP_EXECUTE, PosixFilePermission.OTHERS_EXECUTE};

    @Override
    public FileAttributes getAttributes(@Nonnull String pathStr) {
      try {
        Path path = Paths.get(pathStr);

        Class<? extends BasicFileAttributes> schema = SystemInfo.isWindows ? DosFileAttributes.class : PosixFileAttributes.class;
        BasicFileAttributes attributes = Files.readAttributes(path, schema, myNoFollowLinkOptions);
        boolean isSymbolicLink = attributes.isSymbolicLink() || SystemInfo.isWindows && attributes.isOther() && attributes.isDirectory() && path.getParent() != null;
        if (isSymbolicLink) {
          try {
            attributes = Files.readAttributes(path, schema);
          }
          catch (NoSuchFileException e) {
            return FileAttributes.BROKEN_SYMLINK;
          }
        }

        boolean isDirectory = attributes.isDirectory();
        boolean isOther = attributes.isOther();
        long size = attributes.size();
        long lastModified = attributes.lastModifiedTime().toMillis();
        if (SystemInfo.isWindows) {
          boolean isHidden = path.getParent() != null && ((DosFileAttributes)attributes).isHidden();
          boolean isWritable = isDirectory || !((DosFileAttributes)attributes).isReadOnly();
          return new FileAttributes(isDirectory, isOther, isSymbolicLink, isHidden, size, lastModified, isWritable);
        }
        else {
          boolean isWritable = Files.isWritable(path);
          return new FileAttributes(isDirectory, isOther, isSymbolicLink, false, size, lastModified, isWritable);
        }
      }
      catch (IOException | InvalidPathException e) {
        LOG.debug(pathStr, e);
        return null;
      }
    }

    @Override
    public String resolveSymLink(@Nonnull String path) throws IOException {
      try {
        return Paths.get(path).toRealPath().toString();
      }
      catch (NoSuchFileException e) {
        return null;
      }
    }

    @Override
    public boolean clonePermissions(@Nonnull String source, @Nonnull String target, boolean execOnly) throws IOException {
      if (!SystemInfo.isUnix) return false;

      Path sourcePath = Paths.get(source), targetPath = Paths.get(target);
      Set<PosixFilePermission> sourcePermissions = Files.readAttributes(sourcePath, PosixFileAttributes.class).permissions();
      Set<PosixFilePermission> targetPermissions = Files.readAttributes(targetPath, PosixFileAttributes.class).permissions();
      Set<PosixFilePermission> newPermissions;
      if (execOnly) {
        newPermissions = EnumSet.copyOf(targetPermissions);
        for (PosixFilePermission permission : myExecPermissions) {
          if (sourcePermissions.contains(permission)) {
            newPermissions.add(permission);
          }
          else {
            newPermissions.remove(permission);
          }
        }
      }
      else {
        newPermissions = sourcePermissions;
      }
      Files.setAttribute(targetPath, "posix:permissions", newPermissions);
      return true;
    }
  }


  @TestOnly
  static void resetMediator() {
    ourMediator = Nio2MediatorImpl.ourInstance;
    ourLocked = false;
  }

  @Nonnull
  static String getMediatorName() {
    return getMediatorName(ourMediator);
  }

  @Nonnull
  private static String getMediatorName(Mediator mediator) {
    return mediator.getClass().getSimpleName().replace("MediatorImpl", "");
  }
}
