/*
 * Copyright 2000-2014 JetBrains s.r.o.
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
package consulo.component.store.impl.internal.storage.vfs;

import consulo.application.Application;
import consulo.application.ApplicationManager;
import consulo.component.persist.RoamingType;
import consulo.component.persist.StoragePathMacros;
import consulo.component.store.impl.internal.PathMacrosService;
import consulo.component.store.impl.internal.StorageNotificationService;
import consulo.component.store.impl.internal.StreamProvider;
import consulo.component.store.impl.internal.TrackingPathMacroSubstitutor;
import consulo.component.store.impl.internal.storage.*;
import consulo.disposer.Disposable;
import consulo.platform.LineSeparator;
import consulo.ui.NotificationType;
import consulo.util.io.FileUtil;
import consulo.util.jdom.JDOMUtil;
import consulo.virtualFileSystem.LocalFileSystem;
import consulo.virtualFileSystem.VirtualFile;
import consulo.virtualFileSystem.event.VirtualFileEvent;
import consulo.virtualFileSystem.event.VirtualFileListener;
import consulo.virtualFileSystem.event.VirtualFileMoveEvent;
import consulo.virtualFileSystem.internal.VirtualFileTracker;
import org.jdom.Element;
import org.jdom.JDOMException;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import java.io.File;
import java.io.IOException;
import java.util.Set;

/**
 * File storage - based on Consulo VFS
 */
public final class VfsFileBasedStorage extends XmlElementStorage implements FileBasedStorage {
  private final String myFilePath;
  private final boolean myUseXmlProlog;
  private final File myFile;
  private volatile VirtualFile myCachedVirtualFile;
  private final LineSeparator myLineSeparator = LineSeparator.LF;

  public VfsFileBasedStorage(@Nonnull String filePath,
                             @Nonnull String fileSpec,
                             @Nullable RoamingType roamingType,
                             @Nullable TrackingPathMacroSubstitutor pathMacroManager,
                             @Nonnull String rootElementName,
                             @Nonnull Disposable parentDisposable,
                             @Nullable final StateStorageListener listener,
                             @Nullable StreamProvider streamProvider,
                             boolean useXmlProlog,
                             @Nonnull PathMacrosService pathMacrosService) {
    super(fileSpec, roamingType, pathMacroManager, rootElementName, streamProvider, pathMacrosService);

    myFilePath = filePath;
    myUseXmlProlog = useXmlProlog;
    myFile = new File(filePath);

    if (listener != null) {
      VirtualFileTracker virtualFileTracker = Application.get().getInstance(VirtualFileTracker.class);
      virtualFileTracker.addTracker(LocalFileSystem.PROTOCOL_PREFIX + myFile.getAbsolutePath().replace(File.separatorChar, '/'), new VirtualFileListener() {
        @Override
        public void fileMoved(@Nonnull VirtualFileMoveEvent event) {
          myCachedVirtualFile = null;
        }

        @Override
        public void fileDeleted(@Nonnull VirtualFileEvent event) {
          myCachedVirtualFile = null;
        }

        @Override
        public void fileCreated(@Nonnull VirtualFileEvent event) {
          myCachedVirtualFile = event.getFile();
        }

        @Override
        public void contentsChanged(@Nonnull final VirtualFileEvent event) {
          listener.storageFileChanged(event, VfsFileBasedStorage.this);
        }
      }, false, parentDisposable);
    }
  }

  protected boolean isUseXmlProlog() {
    return myUseXmlProlog;
  }

  @Override
  protected XmlElementStorageSaveSession createSaveSession(@Nonnull StorageData storageData) {
    return new FileSaveSession(storageData);
  }

  private class FileSaveSession extends XmlElementStorageSaveSession {
    protected FileSaveSession(@Nonnull StorageData storageData) {
      super(storageData);
    }

    @Override
    protected void doSave(@Nullable Element element) throws IOException {
      byte[] content = element == null ? null : StorageUtil.writeToBytes(element);
      try {
        if (myStreamProvider != null && myStreamProvider.isEnabled()) {
          saveForProvider(content, element);
        }
      }
      catch (Throwable e) {
        LOG.error(e);
      }

      if (content == null) {
        StorageUtil.deleteFile(myFile, this, getVirtualFile());
        myCachedVirtualFile = null;
      }
      else {
        VirtualFile file = getVirtualFile();
        if (file == null || !file.exists()) {
          FileUtil.createParentDirs(myFile);
          file = null;
        }
        myCachedVirtualFile = StorageUtil.writeFile(myFile, this, file, content, isUseXmlProlog() ? myLineSeparator : null);
      }
    }
  }

  @Override
  @Nonnull
  protected StorageData createStorageData() {
    return new StorageData(myRootElementName, myPathMacrosService);
  }

  @Nullable
  public VirtualFile getVirtualFile() {
    VirtualFile virtualFile = myCachedVirtualFile;
    if (virtualFile == null) {
      myCachedVirtualFile = virtualFile = LocalFileSystem.getInstance().findFileByIoFile(myFile);
    }
    return virtualFile;
  }

  @Nonnull
  public File getFile() {
    return myFile;
  }

  @Override
  @Nonnull
  public String getFilePath() {
    return myFilePath;
  }

  @Override
  @Nullable
  protected Element loadLocalData() {
    myBlockSavingTheContent = false;
    try {
      VirtualFile file = getVirtualFile();
      if (file == null || file.isDirectory() || !file.isValid()) {
        if (LOG.isDebugEnabled()) {
          LOG.debug("Document was not loaded for " + myFileSpec + " file is " + (file == null ? "null" : "directory"));
        }
        return null;
      }
      if (file.getLength() == 0) {
        return processReadException(null);
      }

      return JDOMUtil.load(file.getInputStream());
    }
    catch (JDOMException e) {
      return processReadException(e);
    }
    catch (IOException e) {
      return processReadException(e);
    }
  }

  @Nullable
  private Element processReadException(@Nullable Exception e) {
    boolean contentTruncated = e == null;
    myBlockSavingTheContent = !contentTruncated && (StorageUtil.isProjectOrModuleFile(myFileSpec) || myFileSpec.equals(StoragePathMacros.WORKSPACE_FILE));
    if (!ApplicationManager.getApplication().isUnitTestMode() && !ApplicationManager.getApplication().isHeadlessEnvironment()) {
      if (e != null) {
        LOG.info(e);
      }
      StorageNotificationService.getInstance().notify(NotificationType.WARNING, "Load Settings", "Cannot load settings from file '" +
                                                                                                 myFile.getPath() +
                                                                                                 "': " +
                                                                                                 (e == null ? "content truncated" : e.getMessage()) +
                                                                                                 "\n" +
                                                                                                 (myBlockSavingTheContent ? "Please correct the file content" : "File content will be recreated"),
                                                      null);
    }
    return null;
  }

  @Override
  public void setDefaultState(@Nonnull Element element) {
    element.setName(myRootElementName);
    super.setDefaultState(element);
  }

  public void updatedFromStreamProvider(@Nonnull Set<String> changedComponentNames, boolean deleted) {
    if (myRoamingType == RoamingType.DISABLED) {
      // storage roaming was changed to DISABLED, but settings repository has old state
      return;
    }

    try {
      Element newElement = deleted ? null : loadDataFromStreamProvider();
      if (newElement == null) {
        StorageUtil.deleteFile(myFile, this, myCachedVirtualFile);
        // if data was loaded, mark as changed all loaded components
        if (myLoadedData != null) {
          changedComponentNames.addAll(myLoadedData.getComponentNames());
          myLoadedData = null;
        }
      }
      else if (myLoadedData != null) {
        StorageData newStorageData = createStorageData();
        loadState(newStorageData, newElement);
        changedComponentNames.addAll(myLoadedData.getChangedComponentNames(newStorageData, myPathMacroSubstitutor));
        myLoadedData = newStorageData;
      }
    }
    catch (Throwable e) {
      LOG.error(e);
    }
  }

  @Override
  public String toString() {
    return getFilePath();
  }
}
