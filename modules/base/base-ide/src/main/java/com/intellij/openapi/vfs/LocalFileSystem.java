// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.openapi.vfs;

import com.intellij.openapi.vfs.newvfs.NewVirtualFileSystem;
import org.jetbrains.annotations.SystemIndependent;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.File;
import java.nio.file.Path;
import java.util.Collection;
import java.util.Collections;
import java.util.Set;

import static java.util.Collections.singleton;

public abstract class LocalFileSystem extends NewVirtualFileSystem {
  public static final String PROTOCOL = StandardFileSystems.FILE_PROTOCOL;
  public static final String PROTOCOL_PREFIX = StandardFileSystems.FILE_PROTOCOL_PREFIX;

  private static class LocalFileSystemHolder {
    private static final LocalFileSystem ourInstance = (LocalFileSystem)VirtualFileManager.getInstance().getFileSystem(PROTOCOL);
  }

  public static LocalFileSystem getInstance() {
    return LocalFileSystemHolder.ourInstance;
  }

  @Nonnull
  public static LocalFileSystem get(@Nonnull VirtualFileManager manager) {
    return (LocalFileSystem)manager.getFileSystem(PROTOCOL);
  }

  @Nullable
  public VirtualFile findFileByIoFile(@Nonnull File file) {
    return findFileByPath(file.getAbsolutePath());
  }

  @Nullable
  public VirtualFile refreshAndFindFileByIoFile(@Nonnull File file) {
    return refreshAndFindFileByPath(file.getAbsolutePath());
  }

  @Nullable
  public VirtualFile findFileByNioFile(@Nonnull Path file) {
    return findFileByPath(file.toAbsolutePath().toString());
  }

  @Nullable
  public VirtualFile refreshAndFindFileByNioFile(@Nonnull Path file) {
    return refreshAndFindFileByPath(file.toAbsolutePath().toString());
  }

  /**
   * Performs a non-recursive synchronous refresh of specified files.
   *
   * @param files files to refresh.
   */
  public abstract void refreshIoFiles(@Nonnull Iterable<? extends File> files);

  public abstract void refreshIoFiles(@Nonnull Iterable<? extends File> files, boolean async, boolean recursive, @Nullable Runnable onFinish);

  /**
   * Performs a non-recursive synchronous refresh of specified files.
   *
   * @param files files to refresh.
   */
  public abstract void refreshFiles(@Nonnull Iterable<? extends VirtualFile> files);

  public abstract void refreshFiles(@Nonnull Iterable<? extends VirtualFile> files, boolean async, boolean recursive, @Nullable Runnable onFinish);

  public interface WatchRequest {
    @Nonnull
    @SystemIndependent String getRootPath();

    boolean isToWatchRecursively();
  }

  @Nullable
  public WatchRequest addRootToWatch(@Nonnull String rootPath, boolean watchRecursively) {
    Set<WatchRequest> result = addRootsToWatch(singleton(rootPath), watchRecursively);
    return result.size() == 1 ? result.iterator().next() : null;
  }

  @Nonnull
  public Set<WatchRequest> addRootsToWatch(@Nonnull Collection<String> rootPaths, boolean watchRecursively) {
    if (rootPaths.isEmpty()) {
      return Collections.emptySet();
    }
    else if (watchRecursively) {
      return replaceWatchedRoots(Collections.emptySet(), rootPaths, null);
    }
    else {
      return replaceWatchedRoots(Collections.emptySet(), null, rootPaths);
    }
  }

  public void removeWatchedRoot(@Nonnull WatchRequest watchRequest) {
    removeWatchedRoots(singleton(watchRequest));
  }

  public void removeWatchedRoots(@Nonnull Collection<WatchRequest> watchRequests) {
    if (!watchRequests.isEmpty()) {
      replaceWatchedRoots(watchRequests, null, null);
    }
  }

  @Nullable
  public WatchRequest replaceWatchedRoot(@Nullable WatchRequest watchRequest, @Nonnull String rootPath, boolean watchRecursively) {
    Set<WatchRequest> requests = watchRequest != null ? singleton(watchRequest) : Collections.emptySet();
    Set<String> roots = singleton(rootPath);
    Set<WatchRequest> result = watchRecursively ? replaceWatchedRoots(requests, roots, null) : replaceWatchedRoots(requests, null, roots);
    return result.size() == 1 ? result.iterator().next() : null;
  }

  /**
   * Stops watching given watch requests and starts watching new paths.
   * May do nothing and return the same set of requests when it contains exactly the same paths.
   */
  @Nonnull
  public abstract Set<WatchRequest> replaceWatchedRoots(@Nonnull Collection<WatchRequest> watchRequests, @Nullable Collection<String> recursiveRoots, @Nullable Collection<String> flatRoots);

  /**
   * Registers a handler that allows a version control system plugin to intercept file operations in the local file system
   * and to perform them through the VCS tool.
   *
   * @param handler the handler instance.
   */
  public abstract void registerAuxiliaryFileOperationsHandler(@Nonnull LocalFileOperationsHandler handler);

  /**
   * Unregisters a handler that allows a version control system plugin to intercept file operations in the local file system
   * and to perform them through the VCS tool.
   *
   * @param handler the handler instance.
   */
  public abstract void unregisterAuxiliaryFileOperationsHandler(@Nonnull LocalFileOperationsHandler handler);
}