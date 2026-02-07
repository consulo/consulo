/*
 * Copyright 2000-2009 JetBrains s.r.o.
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
package consulo.virtualFileSystem.http.impl.internal;

import consulo.application.WriteAction;
import consulo.localize.LocalizeValue;
import consulo.logging.Logger;
import consulo.util.collection.Lists;
import consulo.util.io.FilePermissionCopier;
import consulo.util.io.FileUtil;
import consulo.virtualFileSystem.LocalFileSystem;
import consulo.virtualFileSystem.VirtualFile;
import consulo.virtualFileSystem.fileType.FileType;
import consulo.virtualFileSystem.http.RemoteContentProvider;
import consulo.virtualFileSystem.http.RemoteFileInfo;
import consulo.virtualFileSystem.http.RemoteFileState;
import consulo.virtualFileSystem.http.event.FileDownloadingAdapter;
import consulo.virtualFileSystem.http.event.FileDownloadingListener;
import consulo.virtualFileSystem.localize.VirtualFileSystemLocalize;
import consulo.virtualFileSystem.util.VirtualFileUtil;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * @author nik
 */
public class RemoteFileInfoImpl implements RemoteFileInfo {
    private static final Logger LOG = Logger.getInstance(RemoteFileInfoImpl.class);
    private final Object myLock = new Object();
    private final String myUrl;
    private final RemoteFileManagerImpl myManager;
    private
    @Nullable
    RemoteContentProvider myContentProvider;
    private File myLocalFile;
    private VirtualFile myLocalVirtualFile;
    private VirtualFile myPrevLocalFile;
    private RemoteFileState myState = RemoteFileState.DOWNLOADING_NOT_STARTED;
    private LocalizeValue myErrorMessage = LocalizeValue.empty();
    private final AtomicBoolean myCancelled = new AtomicBoolean();
    private final List<FileDownloadingListener> myListeners = Lists.newLockFreeCopyOnWriteList();

    public RemoteFileInfoImpl(@Nonnull String url, @Nonnull RemoteFileManagerImpl manager) {
        myUrl = url;
        myManager = manager;
    }

    @Override
    public void addDownloadingListener(@Nonnull FileDownloadingListener listener) {
        myListeners.add(listener);
    }

    @Override
    public void removeDownloadingListener(@Nonnull FileDownloadingListener listener) {
        myListeners.remove(listener);
    }

    @Override
    public String getUrl() {
        return myUrl;
    }

    @Override
    public void restartDownloading() {
        synchronized (myLock) {
            myErrorMessage = null;
            myPrevLocalFile = myLocalVirtualFile;
            myLocalVirtualFile = null;
            myState = RemoteFileState.DOWNLOADING_NOT_STARTED;
            myLocalFile = null;
            startDownloading();
        }
    }

    @Override
    public void startDownloading() {
        LOG.debug("Downloading requested");

        File localFile;
        synchronized (myLock) {
            if (myState != RemoteFileState.DOWNLOADING_NOT_STARTED) {
                LOG.debug("File already downloaded: file = " + myLocalVirtualFile + ", state = " + myState);
                return;
            }
            myState = RemoteFileState.DOWNLOADING_IN_PROGRESS;

            try {
                myLocalFile = myManager.getStorage().createLocalFile(myUrl);
                LOG.debug("Local file created: " + myLocalFile.getAbsolutePath());
            }
            catch (IOException e) {
                LOG.info(e);
                errorOccurred(VirtualFileSystemLocalize.cannotCreateLocalFile(e.getMessage()), false);
                return;
            }
            myCancelled.set(false);
            localFile = myLocalFile;
        }
        for (FileDownloadingListener listener : myListeners) {
            listener.downloadingStarted();
        }

        if (myContentProvider == null) {
            myContentProvider = myManager.findContentProvider(myUrl);
        }
        myContentProvider.saveContent(myUrl, localFile, this);
    }

    @Override
    public void finished(@Nullable FileType fileType) {
        File localIOFile;

        synchronized (myLock) {
            LOG.debug(
                "Downloading finished, size = " + myLocalFile.length() + "," +
                    " file type=" + (fileType != null ? fileType.getName() : "null")
            );
            if (fileType != null) {
                String fileName = myLocalFile.getName();
                int dot = fileName.lastIndexOf('.');
                String extension = fileType.getDefaultExtension();
                if (dot == -1 || !extension.equals(fileName.substring(dot + 1))) {
                    File newFile = FileUtil.findSequentNonexistentFile(myLocalFile.getParentFile(), fileName, extension);
                    try {
                        FileUtil.rename(myLocalFile, newFile, FilePermissionCopier.BY_NIO2);
                        myLocalFile = newFile;
                    }
                    catch (IOException e) {
                        LOG.debug(e);
                    }
                }
            }

            localIOFile = myLocalFile;
        }

        VirtualFile localFile = WriteAction.compute(() -> {
            VirtualFile file = LocalFileSystem.getInstance().refreshAndFindFileByIoFile(localIOFile);
            if (file != null) {
                file.refresh(false, false);
            }
            return file;
        });
        LOG.assertTrue(localFile != null, "Virtual local file not found for " + localIOFile.getAbsolutePath());
        LOG.debug("Virtual local file: " + localFile + ", size = " + localFile.getLength());
        synchronized (myLock) {
            myLocalVirtualFile = localFile;
            myPrevLocalFile = null;
            myState = RemoteFileState.DOWNLOADED;
            myErrorMessage = null;
        }
        for (FileDownloadingListener listener : myListeners) {
            listener.fileDownloaded(localFile);
        }
    }

    @Override
    public boolean isCancelled() {
        return myCancelled.get();
    }

    @Nonnull
    @Override
    public LocalizeValue getErrorMessage() {
        synchronized (myLock) {
            return myErrorMessage;
        }
    }

    @Override
    public void errorOccurred(@Nonnull LocalizeValue errorMessage, boolean cancelled) {
        LOG.debug("Error: " + errorMessage);
        synchronized (myLock) {
            myLocalVirtualFile = null;
            myPrevLocalFile = null;
            myState = RemoteFileState.ERROR_OCCURRED;
            myErrorMessage = errorMessage;
        }
        for (FileDownloadingListener listener : myListeners) {
            if (!cancelled) {
                listener.errorOccurred(errorMessage);
            }
        }
    }

    @Override
    public void setProgressFraction(double fraction) {
        for (FileDownloadingListener listener : myListeners) {
            listener.progressFractionChanged(fraction);
        }
    }

    @Override
    public void setProgressText(@Nonnull LocalizeValue text, boolean indeterminate) {
        for (FileDownloadingListener listener : myListeners) {
            listener.progressMessageChanged(indeterminate, text);
        }
    }

    public VirtualFile getLocalFile() {
        synchronized (myLock) {
            return myLocalVirtualFile;
        }
    }

    @Override
    public String toString() {
        LocalizeValue errorMessage = getErrorMessage();
        return "state=" + getState() + ", local file=" + myLocalFile + (errorMessage != LocalizeValue.empty() ? ", error=" + errorMessage : "") + (isCancelled() ? ", cancelled" : "");
    }

    @Override
    public RemoteFileState getState() {
        synchronized (myLock) {
            return myState;
        }
    }

    @Override
    public void cancelDownloading() {
        synchronized (myLock) {
            myCancelled.set(true);
            if (myPrevLocalFile != null) {
                myLocalVirtualFile = myPrevLocalFile;
                myLocalFile = VirtualFileUtil.virtualToIoFile(myLocalVirtualFile);
                myState = RemoteFileState.DOWNLOADED;
                myErrorMessage = null;
            }
            else {
                myState = RemoteFileState.ERROR_OCCURRED;
            }
        }
        for (FileDownloadingListener listener : myListeners) {
            listener.downloadingCancelled();
        }
    }

    public void refresh(@Nullable Runnable postRunnable) {
        VirtualFile localVirtualFile;
        synchronized (myLock) {
            localVirtualFile = myLocalVirtualFile;
        }
        RemoteContentProvider contentProvider = myManager.findContentProvider(myUrl);
        if ((localVirtualFile == null || !contentProvider.equals(myContentProvider) || !contentProvider.isUpToDate(myUrl, localVirtualFile))) {
            myContentProvider = contentProvider;
            addDownloadingListener(new MyRefreshingDownloadingListener(postRunnable));
            restartDownloading();
        }
    }

    private class MyRefreshingDownloadingListener extends FileDownloadingAdapter {
        private final Runnable myPostRunnable;

        public MyRefreshingDownloadingListener(Runnable postRunnable) {
            myPostRunnable = postRunnable;
        }

        @Override
        public void downloadingCancelled() {
            removeDownloadingListener(this);
            if (myPostRunnable != null) {
                myPostRunnable.run();
            }
        }

        @Override
        public void fileDownloaded(VirtualFile localFile) {
            removeDownloadingListener(this);
            if (myPostRunnable != null) {
                myPostRunnable.run();
            }
        }

        @Override
        public void errorOccurred(@Nonnull LocalizeValue errorMessage) {
            removeDownloadingListener(this);
            if (myPostRunnable != null) {
                myPostRunnable.run();
            }
        }
    }
}
