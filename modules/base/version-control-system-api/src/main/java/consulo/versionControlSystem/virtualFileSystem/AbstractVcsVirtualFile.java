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
package consulo.versionControlSystem.virtualFileSystem;

import consulo.application.ApplicationManager;
import consulo.versionControlSystem.localize.VcsLocalize;
import consulo.virtualFileSystem.VirtualFile;
import consulo.virtualFileSystem.VirtualFileSystem;
import consulo.virtualFileSystem.util.VirtualFileUtil;
import jakarta.annotation.Nonnull;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public abstract class AbstractVcsVirtualFile extends VirtualFile {

  protected final String myName;
  protected final String myPath;
  protected String myRevision;
  private final VirtualFile myParent;
  protected int myModificationStamp = 0;
  private final VirtualFileSystem myFileSystem;
  protected boolean myProcessingBeforeContentsChange;

  protected AbstractVcsVirtualFile(String path, VirtualFileSystem fileSystem) {
    myFileSystem = fileSystem;
    myPath = path;
    File file = new File(myPath);
    myName = file.getName();
    if (!isDirectory())
      myParent = new VcsVirtualFolder(file.getParent(), this, myFileSystem);
    else
      myParent = null;
  }

  @Override
  @Nonnull
  public VirtualFileSystem getFileSystem() {
    return myFileSystem;
  }

  @Override
  public String getPath() {
    return myPath;
  }

  @Override
  @Nonnull
  public String getName() {
    return myName;
  }

  @Override
  public String getPresentableName() {
    if (myRevision == null)
      return myName;
    else
      return myName + " (" + myRevision + ")";
  }

  @Override
  public boolean isWritable() {
    return false;
  }

  @Override
  public boolean isValid() {
    return true;
  }

  @Override
  public VirtualFile getParent() {
    return myParent;

  }

  @Override
  public VirtualFile[] getChildren() {
    return null;
  }

  @Override
  public InputStream getInputStream() throws IOException {
    return VirtualFileUtil.byteStreamSkippingBOM(contentsToByteArray(), this);
  }

  @Override
  @Nonnull
  public OutputStream getOutputStream(Object requestor, long newModificationStamp, long newTimeStamp) throws IOException {
    throw new RuntimeException(VcsLocalize.exceptionTextInternalErrrorCouldNotImplementMethod().get());
  }

  @Override
  @Nonnull
  public abstract byte[] contentsToByteArray() throws IOException;

  @Override
  public long getModificationStamp() {
    return myModificationStamp;
  }

  @Override
  public long getTimeStamp() {
    return myModificationStamp;
  }

  @Override
  public long getLength() {
    try {
      return contentsToByteArray().length;
    } catch (IOException e) {
      return 0;
    }
  }

  @Override
  public void refresh(boolean asynchronous, boolean recursive, Runnable postRunnable) {
    if (postRunnable != null)
      postRunnable.run();
  }

  protected void setRevision(String revision) {
    myRevision = revision;
  }

  protected void fireBeforeContentsChange() {
    myProcessingBeforeContentsChange = true;
    try {
      ApplicationManager.getApplication().runWriteAction(new Runnable() {
        @Override
        public void run() {
          ((VcsFileSystem)getFileSystem()).fireBeforeContentsChange(this, AbstractVcsVirtualFile.this);
        }
      });
    }
    finally {
      myProcessingBeforeContentsChange = false;
    }
  }
}
