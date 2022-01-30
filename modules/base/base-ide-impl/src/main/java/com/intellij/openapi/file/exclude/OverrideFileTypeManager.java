// Copyright 2000-2021 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.openapi.file.exclude;

import consulo.application.ApplicationManager;
import consulo.component.persist.State;
import consulo.component.persist.Storage;
import consulo.virtualFileSystem.fileType.DirectoryFileType;
import consulo.virtualFileSystem.fileType.FileType;
import com.intellij.openapi.fileTypes.PlainTextFileType;
import com.intellij.openapi.fileTypes.UnknownFileType;
import com.intellij.openapi.fileTypes.ex.FakeFileType;
import com.intellij.openapi.fileTypes.ex.FileTypeIdentifiableByVirtualFile;
import consulo.virtualFileSystem.VirtualFile;
import consulo.application.ui.awt.UIUtil;
import jakarta.inject.Singleton;
import org.jetbrains.annotations.TestOnly;

import javax.annotation.Nonnull;

/**
 * Storage for file types user selected in "Override File Type" action
 */
@State(name = "OverrideFileTypeManager", storages = @Storage("overrideFileTypes.xml"))
@Singleton
public final class OverrideFileTypeManager extends PersistentFileSetManager {
  public boolean isMarkedPlainText(@Nonnull VirtualFile file) {
    return PlainTextFileType.INSTANCE.getName().equals(getFileValue(file));
  }

  public static OverrideFileTypeManager getInstance() {
    return ApplicationManager.getApplication().getInstance(OverrideFileTypeManager.class);
  }

  @Override
  boolean addFile(@Nonnull VirtualFile file, @Nonnull FileType type) {
    if (!isOverridable(file.getFileType()) || !isOverridable(type)) {
      throw new IllegalArgumentException(
              "Cannot override filetype for file " + file + " from " + file.getFileType() + " to " + type + " because the " + (isOverridable(type) ? "former" : "latter") + " is not overridable");
    }
    return super.addFile(file, type);
  }

  @TestOnly
  public static void performTestWithMarkedAsPlainText(@Nonnull VirtualFile file, @Nonnull Runnable runnable) {
    ApplicationManager.getApplication().assertIsDispatchThread();
    getInstance().addFile(file, PlainTextFileType.INSTANCE);
    UIUtil.dispatchAllInvocationEvents();
    try {
      runnable.run();
    }
    finally {
      getInstance().removeFile(file);
      UIUtil.dispatchAllInvocationEvents();
    }
  }

  static boolean isOverridable(@Nonnull FileType type) {
    if (type instanceof DirectoryFileType) return false;
    if (type instanceof UnknownFileType) return false;
    if (type instanceof FakeFileType) return false;
    // FileTypeIdentifiableByVirtualFile has hard-coded isMyFileType() which we can't change, so we shouldn't override this,
    // or we will risk creating an inconsistency otherwise (see com.intellij.openapi.fileTypes.impl.FileTypesUltimateTest.testFileTypesIdentifiableByFileHaveConsistentIsMyFile)
    if (type instanceof FileTypeIdentifiableByVirtualFile) return false;

    return true;
  }
}
