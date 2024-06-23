// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.versionControlSystem.change;

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ServiceAPI;
import consulo.project.Project;
import consulo.versionControlSystem.FilePath;
import consulo.virtualFileSystem.VirtualFile;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.util.Collection;

/**
 * Manages asynchronous file status updating for files under VCS.
 *
 * @author max
 */
@ServiceAPI(ComponentScope.PROJECT)
public abstract class VcsDirtyScopeManager {
  @Nonnull
  public static VcsDirtyScopeManager getInstance(@Nonnull Project project) {
    return project.getInstance(VcsDirtyScopeManager.class);
  }

  /**
   * Requests an asynchronous file status update for all files in the project.
   */
  public abstract void markEverythingDirty();

  /**
   * Requests an asynchronous file status update for the specified virtual file. Must be called from a read action.
   *
   * @param file the file for which the status update is requested.
   */
  public abstract void fileDirty(@Nonnull VirtualFile file);

  /**
   * Requests an asynchronous file status update for the specified file path. Must be called from a read action.
   *
   * @param file the file path for which the status update is requested.
   */
  public abstract void fileDirty(@Nonnull FilePath file);

  /**
   * Requests an asynchronous file status update for all files under the specified directory.
   *
   * @param dir the directory for which the file status update is requested.
   */
  public abstract void dirDirtyRecursively(@Nonnull VirtualFile dir);

  public abstract void dirDirtyRecursively(@Nonnull FilePath path);

  @Nonnull
  public abstract Collection<FilePath> whatFilesDirty(@Nonnull Collection<? extends FilePath> files);

  /**
   * Requests an asynchronous file status update for all files specified and under the specified directories
   */
  public abstract void filePathsDirty(@Nullable final Collection<? extends FilePath> filesDirty,
                                      @Nullable final Collection<? extends FilePath> dirsRecursivelyDirty);

  /**
   * Requests an asynchronous file status update for all files specified and under the specified directories
   */
  public abstract void filesDirty(@Nullable final Collection<? extends VirtualFile> filesDirty,
                                  @Nullable final Collection<? extends VirtualFile> dirsRecursivelyDirty);
}
