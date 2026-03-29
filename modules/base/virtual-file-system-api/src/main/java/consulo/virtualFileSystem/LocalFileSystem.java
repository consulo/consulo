// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.virtualFileSystem;

import org.jetbrains.annotations.SystemIndependent;

import org.jspecify.annotations.Nullable;
import java.io.File;
import java.nio.file.Path;
import java.util.Collection;
import java.util.Collections;
import java.util.Set;

import static java.util.Collections.singleton;

public abstract class LocalFileSystem extends NewVirtualFileSystem implements VirtualFileSystemWithMacroSupport {
  public static final String PROTOCOL = StandardFileSystems.FILE_PROTOCOL;
  public static final String PROTOCOL_PREFIX = StandardFileSystems.FILE_PROTOCOL_PREFIX;

  private static class LocalFileSystemHolder {
    private static final LocalFileSystem ourInstance = LocalFileSystem.get(VirtualFileManager.getInstance());
  }

  public static LocalFileSystem getInstance() {
    return LocalFileSystemHolder.ourInstance;
  }

  public static LocalFileSystem get(VirtualFileManager manager) {
    return (LocalFileSystem)manager.getRequiredFileSystem(PROTOCOL);
  }

  public @Nullable VirtualFile findFileByIoFile(File file) {
    return findFileByPath(file.getAbsolutePath());
  }

  public @Nullable VirtualFile refreshAndFindFileByIoFile(File file) {
    return refreshAndFindFileByPath(file.getAbsolutePath());
  }

  public @Nullable VirtualFile findFileByNioFile(Path file) {
    return findFileByPath(file.toAbsolutePath().toString());
  }

  public @Nullable VirtualFile refreshAndFindFileByNioFile(Path file) {
    return refreshAndFindFileByPath(file.toAbsolutePath().toString());
  }

  /**
   * Performs a non-recursive synchronous refresh of specified files.
   *
   * @param files files to refresh.
   */
  public abstract void refreshIoFiles(Iterable<? extends File> files);

  public abstract void refreshIoFiles(Iterable<? extends File> files, boolean async, boolean recursive, @Nullable Runnable onFinish);

  /**
   * Performs a non-recursive synchronous refresh of specified files.
   *
   * @param files files to refresh.
   */
  public abstract void refreshFiles(Iterable<? extends VirtualFile> files);

  public abstract void refreshFiles(Iterable<? extends VirtualFile> files, boolean async, boolean recursive, @Nullable Runnable onFinish);

  public interface WatchRequest {
    @SystemIndependent String getRootPath();

    boolean isToWatchRecursively();
  }

  public @Nullable WatchRequest addRootToWatch(String rootPath, boolean watchRecursively) {
    Set<WatchRequest> result = addRootsToWatch(singleton(rootPath), watchRecursively);
    return result.size() == 1 ? result.iterator().next() : null;
  }

  public Set<WatchRequest> addRootsToWatch(Collection<String> rootPaths, boolean watchRecursively) {
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

  public void removeWatchedRoot(WatchRequest watchRequest) {
    removeWatchedRoots(singleton(watchRequest));
  }

  public void removeWatchedRoots(Collection<WatchRequest> watchRequests) {
    if (!watchRequests.isEmpty()) {
      replaceWatchedRoots(watchRequests, null, null);
    }
  }

  public @Nullable WatchRequest replaceWatchedRoot(@Nullable WatchRequest watchRequest, String rootPath, boolean watchRecursively) {
    Set<WatchRequest> requests = watchRequest != null ? singleton(watchRequest) : Collections.emptySet();
    Set<String> roots = singleton(rootPath);
    Set<WatchRequest> result = watchRecursively ? replaceWatchedRoots(requests, roots, null) : replaceWatchedRoots(requests, null, roots);
    return result.size() == 1 ? result.iterator().next() : null;
  }

  /**
   * Stops watching given watch requests and starts watching new paths.
   * May do nothing and return the same set of requests when it contains exactly the same paths.
   */
  public abstract Set<WatchRequest> replaceWatchedRoots(
    Collection<WatchRequest> watchRequests,
    @Nullable Collection<String> recursiveRoots,
    @Nullable Collection<String> flatRoots
  );

  /**
   * Registers a handler that allows a version control system plugin to intercept file operations in the local file system
   * and to perform them through the VCS tool.
   *
   * @param handler the handler instance.
   */
  public abstract void registerAuxiliaryFileOperationsHandler(LocalFileOperationsHandler handler);

  /**
   * Unregisters a handler that allows a version control system plugin to intercept file operations in the local file system
   * and to perform them through the VCS tool.
   *
   * @param handler the handler instance.
   */
  public abstract void unregisterAuxiliaryFileOperationsHandler(LocalFileOperationsHandler handler);
}