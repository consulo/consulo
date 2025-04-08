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
package consulo.virtualFileSystem.status.impl.internal;

import consulo.annotation.component.ServiceImpl;
import consulo.application.Application;
import consulo.colorScheme.EditorColorKey;
import consulo.colorScheme.event.EditorColorsListener;
import consulo.disposer.Disposable;
import consulo.disposer.Disposer;
import consulo.document.Document;
import consulo.localize.LocalizeValue;
import consulo.project.Project;
import consulo.ui.color.ColorValue;
import consulo.util.collection.Lists;
import consulo.util.lang.ThreeState;
import consulo.virtualFileSystem.NonPhysicalFileSystem;
import consulo.virtualFileSystem.VFileProperty;
import consulo.virtualFileSystem.VirtualFile;
import consulo.virtualFileSystem.status.FileStatus;
import consulo.virtualFileSystem.status.FileStatusListener;
import consulo.virtualFileSystem.status.FileStatusManager;
import consulo.virtualFileSystem.status.FileStatusProvider;
import jakarta.annotation.Nonnull;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author mike
 */
@Singleton
@ServiceImpl
public class FileStatusManagerImpl extends FileStatusManager implements Disposable {
    private final Map<VirtualFile, FileStatus> myCachedStatuses = Collections.synchronizedMap(new HashMap<VirtualFile, FileStatus>());
    private final Map<VirtualFile, Boolean> myWhetherExactlyParentToChanged =
        Collections.synchronizedMap(new HashMap<VirtualFile, Boolean>());
    private final Project myProject;
    private final List<FileStatusListener> myListeners = Lists.newLockFreeCopyOnWriteList();
    private FileStatusProvider myFileStatusProvider;

    private static class FileStatusNull implements FileStatus {
        private static final FileStatus INSTANCE = new FileStatusNull();

        @Nonnull
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
    public FileStatusManagerImpl(Project project) {
        myProject = project;

        project.getMessageBus().connect().subscribe(EditorColorsListener.class, scheme -> fileStatusesChanged());
    }

    public void setFileStatusProvider(FileStatusProvider fileStatusProvider) {
        myFileStatusProvider = fileStatusProvider;
    }

    public FileStatus calcStatus(@Nonnull VirtualFile virtualFile) {
        for (FileStatusProvider extension : FileStatusProvider.EP_NAME.getExtensionList(myProject)) {
            FileStatus status = extension.getFileStatus(virtualFile);
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
    public static FileStatus getDefaultStatus(@Nonnull VirtualFile file) {
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
    public void addFileStatusListener(@Nonnull FileStatusListener listener, @Nonnull Disposable parentDisposable) {
        addFileStatusListener(listener);
        Disposer.register(parentDisposable, () -> removeFileStatusListener(listener));
    }

    @Override
    public void fileStatusesChanged() {
        if (myProject.isDisposed()) {
            return;
        }
        Application application = myProject.getApplication();

        if (!application.isDispatchThread()) {
            application.invokeLater(this::fileStatusesChanged, application.getNoneModalityState());
            return;
        }

        myCachedStatuses.clear();
        myWhetherExactlyParentToChanged.clear();

        for (FileStatusListener listener : myListeners) {
            listener.fileStatusesChanged();
        }
    }

    private void cacheChangedFileStatus(VirtualFile virtualFile, FileStatus fs) {
        myCachedStatuses.put(virtualFile, fs);
        if (FileStatus.NOT_CHANGED.equals(fs)) {
            ThreeState parentingStatus = myFileStatusProvider.getNotChangedDirectoryParentingStatus(virtualFile);
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
    public void fileStatusChanged(VirtualFile file) {
        Application application = Application.get();
        if (!application.isDispatchThread() && !application.isUnitTestMode()) {
            application.invokeLater(() -> fileStatusChanged(file));
            return;
        }

        if (file == null || !file.isValid()) {
            return;
        }
        FileStatus cachedStatus = getCachedStatus(file);
        if (cachedStatus == FileStatusNull.INSTANCE) {
            return;
        }
        if (cachedStatus == null) {
            cacheChangedFileStatus(file, FileStatusNull.INSTANCE);
            return;
        }
        FileStatus newStatus = calcStatus(file);
        if (cachedStatus == newStatus) {
            return;
        }
        cacheChangedFileStatus(file, newStatus);

        for (FileStatusListener listener : myListeners) {
            listener.fileStatusChanged(file);
        }
    }

    @Override
    public FileStatus getStatus(@Nonnull VirtualFile file) {
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

    public FileStatus getCachedStatus(VirtualFile file) {
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
        if (status != FileStatus.NOT_CHANGED || !file.isValid() || !file.isDirectory()) {
            return status;
        }
        Boolean immediate = myWhetherExactlyParentToChanged.get(file);
        if (immediate == null) {
            return status;
        }
        return immediate ? FileStatus.NOT_CHANGED_IMMEDIATE : FileStatus.NOT_CHANGED_RECURSIVE;
    }

    public void refreshFileStatusFromDocument(VirtualFile file, Document doc) {
        if (myFileStatusProvider != null) {
            myFileStatusProvider.refreshFileStatusFromDocument(file, doc);
        }
    }
}
