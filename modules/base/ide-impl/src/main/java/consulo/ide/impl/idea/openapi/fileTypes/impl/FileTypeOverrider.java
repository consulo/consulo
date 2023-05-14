// Copyright 2000-2021 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.ide.impl.idea.openapi.fileTypes.impl;

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ExtensionAPI;
import consulo.component.extension.ExtensionPointName;
import consulo.document.util.FileContentUtilCore;
import consulo.virtualFileSystem.fileType.FileType;
import consulo.ide.impl.idea.openapi.fileTypes.ex.FileTypeManagerEx;
import consulo.virtualFileSystem.VirtualFile;
import consulo.virtualFileSystem.fileType.FileTypeRegistry;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

/**
 * Allows to override the file type for a file. Overrides take precedence over all other ways of determining the type of the file
 * (name checks, content checks, {@link FileTypeRegistry.FileTypeDetector}). An overridden file type
 * completely replaces the file's normal file type for PSI, actions and all other features.
 * <p>
 * If the override conditions for a given {@code FileTypeOverrider} change, it needs to call
 * {@link FileContentUtilCore#reparseFiles(VirtualFile...)} if it's possible to identify specific files affected
 * by the change, or {@link FileTypeManagerEx#makeFileTypesChange(String, Runnable)} ()} if the change affects an unknown number of files.
 */
@ExtensionAPI(ComponentScope.APPLICATION)
public interface FileTypeOverrider {
  ExtensionPointName<FileTypeOverrider> EP_NAME = ExtensionPointName.create(FileTypeOverrider.class);

  @Nullable
  FileType getOverriddenFileType(@Nonnull VirtualFile file);
}
