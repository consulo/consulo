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
package com.intellij.openapi.vcs.impl;

import com.intellij.openapi.application.Application;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.ModalityState;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.EditorFactory;
import com.intellij.openapi.editor.colors.EditorColorKey;
import com.intellij.openapi.editor.colors.EditorColorsManager;
import com.intellij.openapi.editor.event.DocumentAdapter;
import com.intellij.openapi.editor.event.DocumentEvent;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.project.DumbAwareRunnable;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.startup.StartupManager;
import com.intellij.openapi.vcs.FileStatus;
import com.intellij.openapi.vcs.FileStatusListener;
import com.intellij.openapi.vcs.FileStatusManager;
import com.intellij.openapi.vfs.NonPhysicalFileSystem;
import com.intellij.openapi.vfs.VFileProperty;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.util.ThreeState;
import com.intellij.util.containers.ContainerUtil;
import consulo.disposer.Disposable;
import consulo.disposer.Disposer;
import consulo.localize.LocalizeValue;
import consulo.ui.color.ColorValue;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;

import javax.annotation.Nonnull;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author mike
 */
@Singleton
public class FileStatusManagerImpl extends FileStatusManager implements Disposable {
  private final Map<VirtualFile, FileStatus> myCachedStatuses = Collections.synchronizedMap(new HashMap<VirtualFile, FileStatus>());
  private final Map<VirtualFile, Boolean> myWhetherExactlyParentToChanged =
          Collections.synchronizedMap(new HashMap<VirtualFile, Boolean>());
  private final Project myProject;
  private final List<FileStatusListener> myListeners = ContainerUtil.createLockFreeCopyOnWriteList();
  private FileStatusProvider myFileStatusProvider;

  private static class FileStatusNull implements FileStatus {
    private static final FileStatus INSTANCE = new FileStatusNull();

    @Override
    public LocalizeValue getText() {
      throw new AssertionError("Should not be called");
    }

    @Override
    public ColorValue getColor() {
      throw new AssertionError("Should not be called");
    }

    @Nonnull
    @Override
    public EditorColorKey getColorKey() {
      throw new AssertionError("Should not be called");
    }

    @Nonnull
    @Override
    public String getId() {
      throw new AssertionError("Should not be called");
    }
  }

  @Inject
  public FileStatusManagerImpl(Project project, StartupManager startupManager) {
    myProject = project;

    if (project.isDefault()) return;

    project.getMessageBus().connect().subscribe(EditorColorsManager.TOPIC, scheme -> fileStatusesChanged());

    startupManager.registerPreStartupActivity(() -> {
      DocumentAdapter documentListener = new DocumentAdapter() {
        @Override
        public void documentChanged(DocumentEvent event) {
          if (event.getOldLength() == 0 && event.getNewLength() == 0) return;
          VirtualFile file = FileDocumentManager.getInstance().getFile(event.getDocument());
          if (file != null) {
            refreshFileStatusFromDocument(file, event.getDocument());
          }
        }
      };

      final EditorFactory factory = EditorFactory.getInstance();
      factory.getEventMulticaster().addDocumentListener(documentListener, myProject);
    });
    
    startupManager.registerPostStartupActivity(new DumbAwareRunnable() {
      @Override
      public void run() {
        fileStatusesChanged();
      }
    });
  }

  public void setFileStatusProvider(final FileStatusProvider fileStatusProvider) {
    myFileStatusProvider = fileStatusProvider;
  }

  public FileStatus calcStatus(@Nonnull final VirtualFile virtualFile) {
    for (FileStatusProvider extension : FileStatusProvider.EP_NAME.getExtensionList(myProject)) {
      final FileStatus status = extension.getFileStatus(virtualFile);
      if (status != null) {
        return status;
      }
    }

    if (virtualFile.isInLocalFileSystem() && myFileStatusProvider != null) {
      return myFileStatusProvider.getFileStatus(virtualFile);
    }

    return getDefaultStatus(virtualFile);
  }

  @Nonnull
  public static FileStatus getDefaultStatus(@Nonnull final VirtualFile file) {
    return file.isValid() && file.is(VFileProperty.SPECIAL) ? FileStatus.IGNORED : FileStatus.NOT_CHANGED;
  }

  @Override
  public void dispose() {
    myCachedStatuses.clear();
  }


  @Override
  public void addFileStatusListener(@Nonnull FileStatusListener listener) {
    myListeners.add(listener);
  }

  @Override
  public void addFileStatusListener(@Nonnull final FileStatusListener listener, @Nonnull Disposable parentDisposable) {
    addFileStatusListener(listener);
    Disposer.register(parentDisposable, new Disposable() {
      @Override
      public void dispose() {
        removeFileStatusListener(listener);
      }
    });
  }

  @Override
  public void fileStatusesChanged() {
    if (myProject.isDisposed()) {
      return;
    }
    if (!ApplicationManager.getApplication().isDispatchThread()) {
      ApplicationManager.getApplication().invokeLater(new DumbAwareRunnable() {
        @Override
        public void run() {
          fileStatusesChanged();
        }
      }, ModalityState.NON_MODAL);
      return;
    }

    myCachedStatuses.clear();
    myWhetherExactlyParentToChanged.clear();

    for (FileStatusListener listener : myListeners) {
      listener.fileStatusesChanged();
    }
  }

  private void cacheChangedFileStatus(final VirtualFile virtualFile, final FileStatus fs) {
    myCachedStatuses.put(virtualFile, fs);
    if (FileStatus.NOT_CHANGED.equals(fs)) {
      final ThreeState parentingStatus = myFileStatusProvider.getNotChangedDirectoryParentingStatus(virtualFile);
      if (ThreeState.YES.equals(parentingStatus)) {
        myWhetherExactlyParentToChanged.put(virtualFile, true);
      }
      else if (ThreeState.UNSURE.equals(parentingStatus)) {
        myWhetherExactlyParentToChanged.put(virtualFile, false);
      }
    }
    else {
      myWhetherExactlyParentToChanged.remove(virtualFile);
    }
  }

  @Override
  public void fileStatusChanged(final VirtualFile file) {
    final Application application = ApplicationManager.getApplication();
    if (!application.isDispatchThread() && !application.isUnitTestMode()) {
      ApplicationManager.getApplication().invokeLater(new DumbAwareRunnable() {
        @Override
        public void run() {
          fileStatusChanged(file);
        }
      });
      return;
    }

    if (file == null || !file.isValid()) return;
    FileStatus cachedStatus = getCachedStatus(file);
    if (cachedStatus == FileStatusNull.INSTANCE) {
      return;
    }
    if (cachedStatus == null) {
      cacheChangedFileStatus(file, FileStatusNull.INSTANCE);
      return;
    }
    FileStatus newStatus = calcStatus(file);
    if (cachedStatus == newStatus) return;
    cacheChangedFileStatus(file, newStatus);

    for (FileStatusListener listener : myListeners) {
      listener.fileStatusChanged(file);
    }
  }

  @Override
  public FileStatus getStatus(@Nonnull final VirtualFile file) {
    if (file.getFileSystem() instanceof NonPhysicalFileSystem) {
      return FileStatus.SUPPRESSED;  // do not leak light files via cache
    }

    FileStatus status = getCachedStatus(file);
    if (status == null || status == FileStatusNull.INSTANCE) {
      status = calcStatus(file);
      cacheChangedFileStatus(file, status);
    }

    return status;
  }

  public FileStatus getCachedStatus(final VirtualFile file) {
    return myCachedStatuses.get(file);
  }

  @Override
  public void removeFileStatusListener(@Nonnull FileStatusListener listener) {
    myListeners.remove(listener);
  }

  @Override
  public ColorValue getNotChangedDirectoryColor(@Nonnull VirtualFile file) {
    return getRecursiveStatus(file).getColor();
  }

  @Nonnull
  @Override
  public FileStatus getRecursiveStatus(@Nonnull VirtualFile file) {
    FileStatus status = super.getRecursiveStatus(file);
    if (status != FileStatus.NOT_CHANGED || !file.isValid() || !file.isDirectory()) return status;
    Boolean immediate = myWhetherExactlyParentToChanged.get(file);
    if (immediate == null) return status;
    return immediate ? FileStatus.NOT_CHANGED_IMMEDIATE : FileStatus.NOT_CHANGED_RECURSIVE;
  }

  public void refreshFileStatusFromDocument(final VirtualFile file, final Document doc) {
    if (myFileStatusProvider != null) {
      myFileStatusProvider.refreshFileStatusFromDocument(file, doc);
    }
  }
}
