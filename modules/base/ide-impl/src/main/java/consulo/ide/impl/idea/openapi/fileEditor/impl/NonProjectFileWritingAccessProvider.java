/*
 * Copyright 2000-2016 JetBrains s.r.o.
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
package consulo.ide.impl.idea.openapi.fileEditor.impl;

import consulo.annotation.component.ExtensionImpl;
import consulo.application.Application;
import consulo.application.ApplicationManager;
import consulo.application.util.registry.Registry;
import consulo.disposer.Disposable;
import consulo.disposer.Disposer;
import consulo.fileEditor.NonProjectFileWritingAccessExtension;
import consulo.ide.impl.idea.openapi.fileEditor.ex.IdeDocumentHistory;
import consulo.module.content.ProjectFileIndex;
import consulo.project.Project;
import consulo.util.collection.ArrayUtil;
import consulo.ide.impl.idea.util.containers.ContainerUtil;
import consulo.util.dataholder.Key;
import consulo.util.dataholder.NotNullLazyKey;
import consulo.util.dataholder.UserDataHolder;
import consulo.util.io.FileUtil;
import consulo.virtualFileSystem.*;
import consulo.virtualFileSystem.event.VirtualFileAdapter;
import consulo.virtualFileSystem.event.VirtualFileCopyEvent;
import consulo.virtualFileSystem.event.VirtualFileEvent;
import consulo.virtualFileSystem.util.VirtualFileUtil;
import jakarta.inject.Inject;
import org.jetbrains.annotations.TestOnly;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import java.io.File;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@ExtensionImpl(id = "nonProjectFile", order = "first")
public class NonProjectFileWritingAccessProvider extends WritingAccessProvider {
  private static final Key<Boolean> ENABLE_IN_TESTS = Key.create("NON_PROJECT_FILE_ACCESS_ENABLE_IN_TESTS");
  private static final NotNullLazyKey<AtomicInteger, UserDataHolder> ACCESS_ALLOWED = NotNullLazyKey.create("NON_PROJECT_FILE_ACCESS", holder -> new AtomicInteger());

  private static final AtomicBoolean myInitialized = new AtomicBoolean();

  @Nonnull
  private final Project myProject;

  @Inject
  public NonProjectFileWritingAccessProvider(@Nonnull Project project, @Nonnull VirtualFileManager virtualFileManager) {
    myProject = project;

    if (myInitialized.compareAndSet(false, true)) {
      virtualFileManager.addVirtualFileListener(new OurVirtualFileAdapter());
    }
  }

  @Override
  public boolean isPotentiallyWritable(@Nonnull VirtualFile file) {
    return true;
  }

  @Nonnull
  @Override
  public Collection<VirtualFile> requestWriting(VirtualFile... files) {
    if (isAllAccessAllowed()) return Collections.emptyList();

    List<VirtualFile> deniedFiles = Stream.of(files).filter(o -> !isWriteAccessAllowed(o, myProject)).collect(Collectors.toList());
    if (deniedFiles.isEmpty()) return Collections.emptyList();

    UnlockOption unlockOption = askToUnlock(deniedFiles);

    if (unlockOption == null) return deniedFiles;

    switch (unlockOption) {
      case UNLOCK:
        allowWriting(deniedFiles);
        break;
      case UNLOCK_DIR:
        allowWriting(ContainerUtil.map(deniedFiles, VirtualFile::getParent));
        break;
      case UNLOCK_ALL:
        ACCESS_ALLOWED.getValue(getApp()).incrementAndGet();
        break;
    }

    return Collections.emptyList();
  }

  @Nullable
  private UnlockOption askToUnlock(@Nonnull List<VirtualFile> files) {
    NonProjectFileWritingAccessDialog dialog = new NonProjectFileWritingAccessDialog(myProject, files);
    if (!dialog.showAndGet()) return null;
    return dialog.getUnlockOption();
  }

  public static boolean isWriteAccessAllowed(@Nonnull VirtualFile file, @Nonnull Project project) {
    if (isAllAccessAllowed()) return true;
    if (file.isDirectory()) return true;

    if (!(file.getFileSystem() instanceof LocalFileSystem)) return true; // do not block e.g., HttpFileSystem, LightFileSystem etc.
    if (file.getFileSystem() instanceof TempFileSystem) return true;

    if (ArrayUtil.contains(file, IdeDocumentHistory.getInstance(project).getChangedFiles())) return true;

    if (!getApp().isUnitTestMode() && FileUtil.isAncestor(new File(FileUtil.getTempDirectory()), VirtualFileUtil.virtualToIoFile(file), true)) {
      return true;
    }

    VirtualFile each = file;
    while (each != null) {
      if (ACCESS_ALLOWED.getValue(each).get() > 0) return true;
      each = each.getParent();
    }

    return isProjectFile(file, project);
  }

  private static boolean isProjectFile(@Nonnull VirtualFile file, @Nonnull Project project) {
    for (NonProjectFileWritingAccessExtension each : NonProjectFileWritingAccessExtension.EP_NAME.getExtensionList(project)) {
      if (each.isWritable(file)) return true;
      if (each.isNotWritable(file)) return false;
    }

    ProjectFileIndex fileIndex = ProjectFileIndex.getInstance(project);
    if (fileIndex.isInContent(file)) return true;
    if (!Registry.is("ide.hide.excluded.files") && fileIndex.isExcluded(file) && !fileIndex.isUnderIgnored(file)) return true;

    return false;
  }

  public static void allowWriting(VirtualFile... allowedFiles) {
    allowWriting(Arrays.asList(allowedFiles));
  }

  public static void allowWriting(Iterable<VirtualFile> allowedFiles) {
    for (VirtualFile eachAllowed : allowedFiles) {
      ACCESS_ALLOWED.getValue(eachAllowed).incrementAndGet();
    }
  }

  public static void disableChecksDuring(@Nonnull Runnable runnable) {
    Application app = getApp();
    ACCESS_ALLOWED.getValue(app).incrementAndGet();
    try {
      runnable.run();
    }
    finally {
      ACCESS_ALLOWED.getValue(app).decrementAndGet();
    }
  }

  @TestOnly
  public static void enableChecksInTests(@Nonnull Disposable disposable) {
    getApp().putUserData(ENABLE_IN_TESTS, Boolean.TRUE);
    getApp().putUserData(ACCESS_ALLOWED, null);

    Disposer.register(disposable, () -> {
      getApp().putUserData(ENABLE_IN_TESTS, null);
      getApp().putUserData(ACCESS_ALLOWED, null);
    });
  }

  private static boolean isAllAccessAllowed() {
    Application app = getApp();

    // disable checks in tests, if not asked
    if (app.isUnitTestMode() && app.getUserData(ENABLE_IN_TESTS) != Boolean.TRUE) {
      return true;
    }
    return ACCESS_ALLOWED.getValue(app).get() > 0;
  }

  private static Application getApp() {
    return ApplicationManager.getApplication();
  }

  public enum UnlockOption {
    UNLOCK,
    UNLOCK_DIR,
    UNLOCK_ALL
  }

  private static class OurVirtualFileAdapter extends VirtualFileAdapter {
    @Override
    public void fileCreated(@Nonnull VirtualFileEvent event) {
      unlock(event);
    }

    @Override
    public void fileCopied(@Nonnull VirtualFileCopyEvent event) {
      unlock(event);
    }

    private static void unlock(@Nonnull VirtualFileEvent event) {
      if (!event.isFromRefresh() && !event.getFile().isDirectory()) allowWriting(event.getFile());
    }
  }
}