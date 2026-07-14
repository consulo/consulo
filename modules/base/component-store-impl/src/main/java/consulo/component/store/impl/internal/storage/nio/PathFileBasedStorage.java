/*
 * Copyright 2013-2024 consulo.io
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
package consulo.component.store.impl.internal.storage.nio;

import consulo.application.Application;
import consulo.component.persist.RoamingType;
import consulo.component.persist.StoragePathMacros;
import consulo.component.store.internal.StorageNotificationService;
import consulo.component.store.impl.internal.storage.*;
import consulo.component.store.internal.PathMacrosService;
import consulo.component.store.internal.StreamProvider;
import consulo.component.store.internal.TrackingPathMacroSubstitutor;
import consulo.disposer.Disposable;
import consulo.platform.LineSeparator;
import consulo.platform.Platform;
import consulo.ui.NotificationType;
import consulo.util.jdom.JDOMUtil;
import consulo.virtualFileSystem.LocalFileSystem;
import consulo.virtualFileSystem.RefreshQueue;
import consulo.virtualFileSystem.SavingRequestor;
import consulo.virtualFileSystem.VirtualFile;
import consulo.virtualFileSystem.event.VFileContentChangeEvent;
import consulo.virtualFileSystem.event.VFileDeleteEvent;
import consulo.virtualFileSystem.event.VFileEvent;
import consulo.virtualFileSystem.event.VirtualFileEvent;
import consulo.virtualFileSystem.event.VirtualFileListener;
import consulo.virtualFileSystem.event.VirtualFileMoveEvent;
import consulo.virtualFileSystem.internal.VirtualFileTracker;
import org.jspecify.annotations.Nullable;
import org.jdom.Element;
import org.jdom.JDOMException;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/**
 * File storage that writes bytes directly to disk (NIO), never taking a write action.
 * <p>
 * When {@code collectVfsEvents} is {@code true} (project-level stores) it also keeps the VFS in sync: after the
 * lock-free write it appends an explicit, {@link SavingRequestor save-tagged} {@link VFileContentChangeEvent} carrying
 * the real on-disk timestamp/length to the store's save batch. The store applies the whole batch once, synchronously,
 * at the end of the save - so the VFS record matches disk before any later filesystem refresh runs, the refresh finds
 * no diff, {@code StoreReloadManager} skips the event (SaveSession requestor) and the document manager skips reload
 * ({@code isFromSave}). When {@code false} (application-level stores) it is a pure NIO write with no VFS involvement.
 *
 * @author VISTALL
 * @since 2024-08-10
 */
public class PathFileBasedStorage extends XmlElementStorage implements FileBasedStorage {
    private final boolean myUseXmlProlog;
    private final boolean myCollectVfsEvents;
    private final Path myFile;
    private LineSeparator myLineSeparator;
    private volatile VirtualFile myCachedVirtualFile;

    public PathFileBasedStorage(
        String filePath,
        String fileSpec,
        @Nullable RoamingType roamingType,
        @Nullable TrackingPathMacroSubstitutor pathMacroManager,
        String rootElementName,
        Disposable parentDisposable,
        final @Nullable StateStorageListener listener,
        @Nullable StreamProvider streamProvider,
        boolean useXmlProlog,
        boolean collectVfsEvents,
        PathMacrosService pathMacrosService
    ) {
        super(fileSpec, roamingType, pathMacroManager, rootElementName, streamProvider, pathMacrosService);

        myUseXmlProlog = useXmlProlog;
        myCollectVfsEvents = collectVfsEvents;
        myFile = Path.of(filePath);

        if (myCollectVfsEvents && listener != null) {
            VirtualFileTracker virtualFileTracker = Application.get().getInstance(VirtualFileTracker.class);
            String url = LocalFileSystem.PROTOCOL_PREFIX + myFile.toAbsolutePath().toString().replace(File.separatorChar, '/');
            virtualFileTracker.addTracker(url, new VirtualFileListener() {
                @Override
                public void fileMoved(VirtualFileMoveEvent event) {
                    myCachedVirtualFile = null;
                }

                @Override
                public void fileDeleted(VirtualFileEvent event) {
                    myCachedVirtualFile = null;
                }

                @Override
                public void fileCreated(VirtualFileEvent event) {
                    myCachedVirtualFile = event.getFile();
                }

                @Override
                public void contentsChanged(VirtualFileEvent event) {
                    listener.storageFileChanged(event, PathFileBasedStorage.this);
                }
            }, false, parentDisposable);
        }
    }

    protected boolean isUseXmlProlog() {
        return myUseXmlProlog;
    }

    protected boolean isUseLfLineSeparatorByDefault() {
        return isUseXmlProlog();
    }

    public @Nullable VirtualFile getVirtualFile() {
        if (!myCollectVfsEvents) {
            return null;
        }
        VirtualFile virtualFile = myCachedVirtualFile;
        if (virtualFile == null) {
            myCachedVirtualFile = virtualFile = LocalFileSystem.getInstance().findFileByNioFile(myFile);
        }
        return virtualFile;
    }

    @Override
    protected XmlElementStorageSaveSession createSaveSession(StorageData storageData) {
        return new FileSaveSession(storageData);
    }

    protected class FileSaveSession extends XmlElementStorageSaveSession implements SavingRequestor {
        protected FileSaveSession(StorageData storageData) {
            super(storageData);
        }

        @Override
        protected void doSave(@Nullable Element element, @Nullable List<VFileEvent> events) throws IOException {
            if (myLineSeparator == null) {
                myLineSeparator = isUseLfLineSeparatorByDefault() ? LineSeparator.LF : Platform.current().os().lineSeparator();
            }

            byte[] content = element == null ? null : StorageUtil.writeToBytes(element);
            try {
                if (myStreamProvider != null && myStreamProvider.isEnabled()) {
                    saveForProvider(content, element);
                }
            }
            catch (Throwable e) {
                LOG.error(e);
            }

            VirtualFile file = getVirtualFile();
            long oldModificationStamp = file == null ? -1 : file.getModificationStamp();
            long oldTimestamp = file == null ? -1 : file.getTimeStamp();
            long oldLength = file == null ? -1 : file.getLength();

            if (content == null) {
                Files.deleteIfExists(myFile);
            }
            else {
                if (!Files.exists(myFile)) {
                    Files.createDirectories(myFile.getParent());
                }

                PathStorageUtil.writeFile(myFile, content, isUseXmlProlog() ? myLineSeparator : null);
            }

            if (!myCollectVfsEvents || file == null || !file.isValid()) {
                if (content == null) {
                    myCachedVirtualFile = null;
                }
                return;
            }

            if (content == null) {
                myCachedVirtualFile = null;
                collect(events, new VFileDeleteEvent(this, file, false));
                return;
            }

            // Keep the VFS in sync via an explicit save-tagged content-change event with the real on-disk timestamp/length,
            // so that a later filesystem refresh observes no diff and does not report an external change.
            File io = myFile.toFile();
            long newTimestamp = io.lastModified();
            long newLength = io.length();
            collect(events, new VFileContentChangeEvent(this, file, oldModificationStamp, -1, oldTimestamp, newTimestamp, oldLength, newLength, false));
        }
    }

    // Append the event to the store's batch, so it is applied once, synchronously, after all writes. When no batch is
    // supplied (a direct save outside the store flow), apply it immediately and synchronously - never async, otherwise a
    // refresh could observe the stale VFS record before the event is fired.
    private static void collect(@Nullable List<VFileEvent> events, VFileEvent event) {
        if (events != null) {
            events.add(event);
        }
        else {
            RefreshQueue.getInstance().processSingleEvent(event);
        }
    }

    @Override
    protected StorageData createStorageData() {
        return new StorageData(myRootElementName, myPathMacrosService);
    }

    public Path getFile() {
        return myFile;
    }

    @Override
    public String getFilePath() {
        return myFile.toString();
    }

    @Override
    protected @Nullable Element loadLocalData() {
        myBlockSavingTheContent = false;
        try {
            if (!Files.exists(myFile)) {
                if (LOG.isDebugEnabled()) {
                    LOG.debug("Document was not loaded for " + myFileSpec + " file not exists ");
                }
                return null;
            }

            Element element;
            try (DetectingSeparatorReader reader = new DetectingSeparatorReader(Files.newBufferedReader(myFile, StandardCharsets.UTF_8))) {
                element = JDOMUtil.loadDocument(reader).getRootElement();

                LineSeparator defaultSeparator = isUseLfLineSeparatorByDefault() ? Platform.current().os().lineSeparator() : LineSeparator.LF;

                myLineSeparator = reader.getLineSeparator(defaultSeparator);
            }
            return element;
        }
        catch (JDOMException e) {
            return processReadException(e);
        }
        catch (IOException e) {
            return processReadException(e);
        }
    }

    private @Nullable Element processReadException(@Nullable Exception e) {
        boolean contentTruncated = e == null;
        myBlockSavingTheContent = !contentTruncated
            && (StorageUtil.isProjectOrModuleFile(myFileSpec) || myFileSpec.equals(StoragePathMacros.WORKSPACE_FILE));
        Application application = Application.get();
        if (!application.isUnitTestMode() && !application.isHeadlessEnvironment()) {
            if (e != null) {
                LOG.info(e);
            }

            StorageNotificationService.getInstance().notify(
                NotificationType.WARNING,
                "Load Settings",
                "Cannot load settings from file '" + myFile + "': " +
                    (e == null ? "content truncated" : e.getMessage()) + "\n" +
                    (myBlockSavingTheContent ? "Please correct the file content" : "File content will be recreated"),
                null
            );
        }
        return null;
    }

    @Override
    public void setDefaultState(Element element) {
        element.setName(myRootElementName);
        super.setDefaultState(element);
    }

    @Override
    public String toString() {
        return getFilePath();
    }
}
