// Copyright 2000-2021 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.openapi.file.exclude;

import com.intellij.openapi.fileTypes.FileType;
import com.intellij.openapi.fileTypes.FileTypeManager;
import com.intellij.openapi.fileTypes.impl.FileTypeOverrider;
import com.intellij.openapi.vfs.VirtualFile;
import jakarta.inject.Inject;
import jakarta.inject.Provider;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * Substitutes type for files which users explicitly marked with "Override File Type" action
 */
public class UserFileTypeOverrider implements FileTypeOverrider {
  private final Provider<OverrideFileTypeManager> myOverrideFileTypeManagerProvider;
  private final Provider<FileTypeManager> myFileTypeManagerProvider;

  @Inject
  public UserFileTypeOverrider(Provider<OverrideFileTypeManager> overrideFileTypeManagerProvider, Provider<FileTypeManager> fileTypeManagerProvider) {
    myOverrideFileTypeManagerProvider = overrideFileTypeManagerProvider;
    myFileTypeManagerProvider = fileTypeManagerProvider;
  }

  @Nullable
  @Override
  public FileType getOverriddenFileType(@Nonnull VirtualFile file) {
    String overriddenType = myOverrideFileTypeManagerProvider.get().getFileValue(file);
    if (overriddenType != null) {
      return myFileTypeManagerProvider.get().findFileTypeByName(overriddenType);
    }
    return null;
  }
}
