// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.virtualFileSystem;


import org.jspecify.annotations.Nullable;
import java.io.File;
import java.nio.file.Path;


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