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
package consulo.component.store.impl.internal.storage.io;

import consulo.application.Application;
import consulo.component.persist.RoamingType;
import consulo.component.persist.StoragePathMacros;
import consulo.component.store.impl.internal.PathMacrosService;
import consulo.component.store.impl.internal.StorageNotificationService;
import consulo.component.store.impl.internal.StreamProvider;
import consulo.component.store.impl.internal.TrackingPathMacroSubstitutor;
import consulo.component.store.impl.internal.storage.*;
import consulo.disposer.Disposable;
import consulo.platform.LineSeparator;
import consulo.platform.Platform;
import consulo.ui.NotificationType;
import consulo.util.io.FileUtil;
import consulo.util.jdom.JDOMUtil;
import consulo.virtualFileSystem.RawFileLoader;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.jdom.Element;
import org.jdom.JDOMException;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.StandardCharsets;

/**
 * @author VISTALL
 * @since 2019-02-13
 */
public final class IoFileBasedStorage extends XmlElementStorage implements FileBasedStorage {
  private final String myFilePath;
  private final boolean myUseXmlProlog;
  private final File myFile;
  private LineSeparator myLineSeparator;

  public IoFileBasedStorage(
    @Nonnull String filePath,
    @Nonnull String fileSpec,
    @Nullable RoamingType roamingType,
    @Nullable TrackingPathMacroSubstitutor pathMacroManager,
    @Nonnull String rootElementName,
    @Nonnull Disposable parentDisposable,
    @Nullable final StateStorageListener listener,
    @Nullable StreamProvider streamProvider,
    boolean useXmlProlog,
    @Nonnull PathMacrosService pathMacrosService
  ) {
    super(fileSpec, roamingType, pathMacroManager, rootElementName, streamProvider, pathMacrosService);

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
    return new StorageData(myRootElementName, myPathMacrosService);
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
      if (!myFile.exists()) {
        if (LOG.isDebugEnabled()) {
          LOG.debug("Document was not loaded for " + myFileSpec + " file not exists ");
        }
        return null;
      }
      if (myFile.length() == 0) {
        return processReadException(null);
      }

      CharBuffer charBuffer = StandardCharsets.UTF_8.decode(ByteBuffer.wrap(RawFileLoader.getInstance().loadFileBytes(myFile)));
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
        "Cannot load settings from file '" + myFile.getPath() + "': " +
          (e == null ? "content truncated" : e.getMessage()) + "\n" +
          (myBlockSavingTheContent ? "Please correct the file content" : "File content will be recreated"),
        null
      );
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
