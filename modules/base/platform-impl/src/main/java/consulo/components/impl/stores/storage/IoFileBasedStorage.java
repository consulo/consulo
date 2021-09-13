/*
 * Copyright 2013-2019 consulo.io
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
import com.intellij.openapi.components.StoragePathMacros;
import com.intellij.openapi.components.TrackingPathMacroSubstitutor;
import com.intellij.openapi.util.JDOMUtil;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.vfs.CharsetToolkit;
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
import java.nio.ByteBuffer;
import java.nio.CharBuffer;

/**
 * @author VISTALL
 * @since 2019-02-13
 */
public final class IoFileBasedStorage extends XmlElementStorage {
  private final String myFilePath;
  private final boolean myUseXmlProlog;
  private final File myFile;
  private LineSeparator myLineSeparator;

  public IoFileBasedStorage(@Nonnull String filePath,
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
  }

  protected boolean isUseXmlProlog() {
    return myUseXmlProlog;
  }

  protected boolean isUseLfLineSeparatorByDefault() {
    return isUseXmlProlog();
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
      if (myLineSeparator == null) {
        myLineSeparator = isUseLfLineSeparatorByDefault() ? LineSeparator.LF : LineSeparator.getSystemLineSeparator();
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

      if (content == null) {
        StorageUtil.deleteFile(myFile);
      }
      else {
        FileUtil.createParentDirs(myFile);

        StorageUtil.writeFile(myFile, content, isUseXmlProlog() ? myLineSeparator : null);
      }
    }
  }

  @Override
  @Nonnull
  protected StorageData createStorageData() {
    return new StorageData(myRootElementName);
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
      if (!myFile.exists()) {
        if (LOG.isDebugEnabled()) {
          LOG.debug("Document was not loaded for " + myFileSpec + " file not exists ");
        }
        return null;
      }
      if (myFile.length() == 0) {
        return processReadException(null);
      }

      CharBuffer charBuffer = CharsetToolkit.UTF8_CHARSET.decode(ByteBuffer.wrap(FileUtil.loadFileBytes(myFile)));
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
      new Notification(Notifications.SYSTEM_MESSAGES_GROUP_ID, "Load Settings", "Cannot load settings from file '" +
                                                                                myFile.getPath() +
                                                                                "': " +
                                                                                (e == null ? "content truncated" : e.getMessage()) +
                                                                                "\n" +
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

  @Override
  public String toString() {
    return getFilePath();
  }
}
