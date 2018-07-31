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
package com.intellij.openapi.components.impl.stores;

import com.intellij.notification.Notification;
import com.intellij.notification.NotificationType;
import com.intellij.notification.Notifications;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.RoamingType;
import com.intellij.openapi.components.StoragePathMacros;
import com.intellij.openapi.components.TrackingPathMacroSubstitutor;
import com.intellij.openapi.util.JDOMUtil;
import com.intellij.openapi.util.io.BufferExposingByteArrayOutputStream;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.vfs.*;
import com.intellij.openapi.vfs.tracker.VirtualFileTracker;
import com.intellij.util.LineSeparator;
import consulo.application.options.PathMacrosService;
import org.jdom.Element;
import org.jdom.JDOMException;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.util.Set;

public class FileBasedStorage extends XmlElementStorage {
  private final String myFilePath;
  private final LocalFileSystem myLocalFileSystem;
  private final File myFile;
  private volatile VirtualFile myCachedVirtualFile;
  private LineSeparator myLineSeparator;

  public FileBasedStorage(@Nonnull String filePath,
                          @Nonnull String fileSpec,
                          @Nullable RoamingType roamingType,
                          @Nullable TrackingPathMacroSubstitutor pathMacroManager,
                          @Nonnull String rootElementName,
                          @Nonnull Disposable parentDisposable,
                          @Nullable final Listener listener,
                          @Nullable StreamProvider streamProvider,
                          @Nonnull LocalFileSystem localFileSystem,
                          @Nonnull VirtualFileTracker virtualFileTracker,
                          @Nonnull PathMacrosService pathMacrosService) {
    super(fileSpec, roamingType, pathMacroManager, rootElementName, streamProvider, pathMacrosService);

    myFilePath = filePath;
    myLocalFileSystem = localFileSystem;
    myFile = new File(filePath);

    if (listener != null) {
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
          listener.storageFileChanged(event, FileBasedStorage.this);
        }
      }, false, parentDisposable);
    }
  }

  protected boolean isUseXmlProlog() {
    return false;
  }

  protected boolean isUseLfLineSeparatorByDefault() {
    return isUseXmlProlog();
  }

  @Override
  protected XmlElementStorageSaveSession createSaveSession(@Nonnull StorageData storageData) {
    return new FileSaveSession(storageData);
  }

  public void forceSave() {
    XmlElementStorageSaveSession externalizationSession = startExternalization();
    if (externalizationSession != null) {
      externalizationSession.forceSave();
    }
  }

  private class FileSaveSession extends XmlElementStorageSaveSession {
    protected FileSaveSession(@Nonnull StorageData storageData) {
      super(storageData);
    }

    @Override
    protected void doSave(@Nullable Element element) throws IOException {
      if (myLineSeparator == null) {
        myLineSeparator = isUseLfLineSeparatorByDefault() ? LineSeparator.LF : LineSeparator.getSystemLineSeparator();
      }

      BufferExposingByteArrayOutputStream content = element == null ? null : StorageUtil.writeToBytes(element, myLineSeparator.getSeparatorString());
      try {
        if (myStreamProvider != null && myStreamProvider.isEnabled()) {
          // stream provider always use LF separator
          saveForProvider(myLineSeparator == LineSeparator.LF ? content : null, element);
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

    @Override
    protected void doSaveAsync(@Nullable Element element) throws IOException {
      if (myLineSeparator == null) {
        myLineSeparator = isUseLfLineSeparatorByDefault() ? LineSeparator.LF : LineSeparator.getSystemLineSeparator();
      }

      BufferExposingByteArrayOutputStream content = element == null ? null : StorageUtil.writeToBytes(element, myLineSeparator.getSeparatorString());

      try {
        if (myStreamProvider != null && myStreamProvider.isEnabled()) {
          // stream provider always use LF separator
          saveForProvider(myLineSeparator == LineSeparator.LF ? content : null, element);
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

        StorageUtil.writeFileAsync(myFile, this, file, content, isUseXmlProlog() ? myLineSeparator : null).doWhenDone((f) -> myCachedVirtualFile = f);
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
      myCachedVirtualFile = virtualFile = myLocalFileSystem.findFileByIoFile(myFile);
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

      CharBuffer charBuffer = CharsetToolkit.UTF8_CHARSET.decode(ByteBuffer.wrap(file.contentsToByteArray()));
      myLineSeparator = StorageUtil.detectLineSeparators(charBuffer, isUseLfLineSeparatorByDefault() ? null : LineSeparator.LF);
      return JDOMUtil.loadDocument(charBuffer).getRootElement();
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
