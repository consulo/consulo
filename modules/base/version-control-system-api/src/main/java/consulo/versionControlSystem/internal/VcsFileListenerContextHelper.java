// Copyright 2000-2021 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.versionControlSystem.internal;

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ServiceAPI;
import consulo.project.Project;
import consulo.versionControlSystem.FilePath;
import jakarta.annotation.Nonnull;

import java.util.Collection;

/**
 * Allows excluding specific files from being processed by {@link VcsVFSListener}.
 * <p>
 * NB: processing order is different for Added and Deleted files, {@link VcsVFSListener} implementation depends on it.
 * <p/>
 * For DELETED files {@link VFileDeleteEvent} MUST be fired AFTER {@link #ignoreDeleted} method invocation.
 * For ADDED files {@link VFileCreateEvent} CAN be fired BEFORE {@link #ignoreAdded} method invocation, in the same command.
 */
@ServiceAPI(ComponentScope.PROJECT)
public interface VcsFileListenerContextHelper {

  static VcsFileListenerContextHelper getInstance(@Nonnull Project project) {
    return project.getInstance(VcsFileListenerContextHelper.class);
  }

  void ignoreDeleted(@Nonnull Collection<? extends FilePath> filePath);

  boolean isDeletionIgnored(@Nonnull FilePath filePath);

  void ignoreAdded(@Nonnull Collection<? extends FilePath> filePaths);

  void ignoreAddedRecursive(@Nonnull Collection<? extends FilePath> filePaths);

  boolean isAdditionIgnored(@Nonnull FilePath filePath);

  void clearContext();

  boolean isAdditionContextEmpty();

  boolean isDeletionContextEmpty();
}
