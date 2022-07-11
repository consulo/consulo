// Copyright 2000-2021 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.ide.impl.idea.openapi.file.exclude;

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ServiceAPI;
import consulo.annotation.component.ServiceImpl;
import consulo.application.ApplicationManager;
import consulo.component.persist.State;
import consulo.component.persist.Storage;
import consulo.virtualFileSystem.fileType.DirectoryFileType;
import consulo.virtualFileSystem.fileType.FileType;
import consulo.language.plain.PlainTextFileType;
import consulo.virtualFileSystem.fileType.UnknownFileType;
import consulo.ide.impl.idea.openapi.fileTypes.ex.FakeFileType;
import consulo.virtualFileSystem.fileType.FileTypeIdentifiableByVirtualFile;
import consulo.virtualFileSystem.VirtualFile;
import consulo.ui.ex.awt.UIUtil;
import jakarta.inject.Singleton;
import org.jetbrains.annotations.TestOnly;

import javax.annotation.Nonnull;

/**
 * Storage for file types user selected in "Override File Type" action
 */
@State(name = "OverrideFileTypeManager", storages = @Storage("overrideFileTypes.xml"))
@Singleton
@ServiceAPI(ComponentScope.APPLICATION)
@ServiceImpl
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
    // or we will risk creating an inconsistency otherwise (see consulo.ide.impl.idea.openapi.fileTypes.impl.FileTypesUltimateTest.testFileTypesIdentifiableByFileHaveConsistentIsMyFile)
    if (type instanceof FileTypeIdentifiableByVirtualFile) return false;

    return true;
  }
}
