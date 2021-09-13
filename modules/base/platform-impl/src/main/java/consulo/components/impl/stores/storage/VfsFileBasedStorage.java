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
package consulo.components.impl.stores.storage;

import com.intellij.notification.Notification;
import com.intellij.notification.NotificationType;
import com.intellij.notification.Notifications;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.RoamingType;
import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.components.StoragePathMacros;
import com.intellij.openapi.components.TrackingPathMacroSubstitutor;
import com.intellij.openapi.util.JDOMUtil;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.vfs.*;
import com.intellij.openapi.vfs.tracker.VirtualFileTracker;
import com.intellij.util.LineSeparator;
import consulo.components.impl.stores.StorageUtil;
import consulo.components.impl.stores.StreamProvider;
import consulo.disposer.Disposable;
import org.jdom.Element;
import org.jdom.JDOMException;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.File;
import java.io.IOException;
import java.util.Set;

/**
 * File storage - based on Consulo VFS
 */
public final class VfsFileBasedStorage extends XmlElementStorage {
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
                             @Nullable final Listener listener,
                             @Nullable StreamProvider streamProvider,
                             boolean useXmlProlog) {
    super(fileSpec, roamingType, pathMacroManager, rootElementName, streamProvider);

    myFilePath = filePath;
    myUseXmlProlog = useXmlProlog;
    myFile = new File(filePath);

    if (listener != null) {
      VirtualFileTracker virtualFileTracker = ServiceManager.getService(VirtualFileTracker.class);
      virtualFileTracker.addTracker(LocalFileSystem.PROTOCOL_PREFIX + myFile.getAbsolutePath().replace(File.separatorChar, '/'), new VirtualFileAdapter() {
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
    return new StorageData(myRootElementName);
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
      new Notification(Notifications.SYSTEM_MESSAGES_GROUP_ID, "Load Settings",
                       "Cannot load settings from file '" +
                       myFile.getPath() + "': " +
                       (e == null ? "content truncated" : e.getMessage()) + "\n" +
                       (myBlockSavingTheContent ? "Please correct the file content" : "File content will be recreated"),
                       NotificationType.WARNING).notify(null);
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
