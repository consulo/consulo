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
package consulo.versionControlSystem.virtualFileSystem;

import consulo.application.ApplicationManager;
import consulo.component.ProcessCanceledException;
import consulo.logging.Logger;
import consulo.ui.ex.awt.Messages;
import consulo.util.collection.ArrayUtil;
import consulo.util.io.CharsetToolkit;
import consulo.versionControlSystem.VcsBundle;
import consulo.versionControlSystem.VcsException;
import consulo.versionControlSystem.history.ShortVcsRevisionNumber;
import consulo.versionControlSystem.history.VcsFileRevision;
import consulo.versionControlSystem.history.VcsRevisionNumber;
import consulo.virtualFileSystem.VirtualFileSystem;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.io.IOException;
import java.nio.charset.Charset;

/**
 * author: lesya
 */
public class VcsVirtualFile extends AbstractVcsVirtualFile {
  private static final Logger LOG = Logger.getInstance(VcsVirtualFile.class);

  private byte[] myContent;
  private final VcsFileRevision myFileRevision;
  private boolean myContentLoadFailed = false;
  private Charset myCharset;

  public VcsVirtualFile(@Nonnull String path,
                        @Nullable VcsFileRevision revision,
                        @Nonnull VirtualFileSystem fileSystem) {
    super(path, fileSystem);
    myFileRevision = revision;
  }

  public VcsVirtualFile(@Nonnull String path,
                        @Nonnull byte[] content,
                        @Nullable String revision,
                        @Nonnull VirtualFileSystem fileSystem) {
    this(path, null, fileSystem);
    myContent = content;
    setRevision(revision);
  }

  @Override
  @Nonnull
  public byte[] contentsToByteArray() throws IOException {
    if (myContentLoadFailed || myProcessingBeforeContentsChange) {
      return ArrayUtil.EMPTY_BYTE_ARRAY;
    }
    if (myContent == null) {
      loadContent();
    }
    return myContent;
  }

  private void loadContent() throws IOException {
    if (myContent != null) return;
    assert myFileRevision != null;

    final VcsFileSystem vcsFileSystem = ((VcsFileSystem)getFileSystem());

    try {
      myFileRevision.loadContent();
      fireBeforeContentsChange();

      myModificationStamp++;
      VcsRevisionNumber revisionNumber = myFileRevision.getRevisionNumber();
      if (revisionNumber instanceof ShortVcsRevisionNumber) {
        setRevision(((ShortVcsRevisionNumber) revisionNumber).toShortString());
      }
      else {
        setRevision(revisionNumber.asString());
      }
      myContent = myFileRevision.getContent();
      myCharset = new CharsetToolkit(myContent).guessEncoding(myContent.length);
      ApplicationManager.getApplication().runWriteAction(new Runnable() {
        @Override
        public void run() {
          vcsFileSystem.fireContentsChanged(this, VcsVirtualFile.this, 0);
        }
      });

    }
    catch (VcsException e) {
      myContentLoadFailed = true;
      ApplicationManager.getApplication().runWriteAction(new Runnable() {
        @Override
        public void run() {
          vcsFileSystem.fireBeforeFileDeletion(this, VcsVirtualFile.this);
        }
      });
      myContent = ArrayUtil.EMPTY_BYTE_ARRAY;
      setRevision("0");

      Messages.showMessageDialog(
              VcsBundle.message("message.text.could.not.load.virtual.file.content", getPresentableUrl(), e.getLocalizedMessage()),
              VcsBundle.message("message.title.could.not.load.content"),
              Messages.getInformationIcon());

      ApplicationManager.getApplication().runWriteAction(new Runnable() {
        @Override
        public void run() {
          vcsFileSystem.fireFileDeleted(this, VcsVirtualFile.this, getName(), getParent());
        }
      });

    }
    catch (ProcessCanceledException ex) {
      myContent = null;
    }

  }

  @Nullable
  public VcsFileRevision getFileRevision() {
    return myFileRevision;
  }

  @Nonnull
  @Override
  public Charset getCharset() {
    if (myCharset != null) return myCharset;
    return super.getCharset();
  }

  @Override
  public boolean isDirectory() {
    return false;
  }

  public String getRevision() {
    if (myRevision == null) {
      try {
        loadContent();
      }
      catch (IOException e) {
        LOG.info(e);
      }
    }
    return myRevision;
  }
}
