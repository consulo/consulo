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

package consulo.ide.impl.idea.openapi.vcs.vfs;

import consulo.application.Application;
import consulo.component.ProcessCanceledException;
import consulo.ide.impl.idea.util.ArrayUtil;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.awt.Messages;
import consulo.ui.ex.awt.UIUtil;
import consulo.util.collection.Maps;
import consulo.versionControlSystem.VcsException;
import consulo.versionControlSystem.change.ContentRevision;
import consulo.versionControlSystem.localize.VcsLocalize;
import jakarta.annotation.Nonnull;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Map;

/**
 * @author yole
 */
public class ContentRevisionVirtualFile extends AbstractVcsVirtualFile {
  @Nonnull
  private final ContentRevision myContentRevision;
  private byte[] myContent;
  private boolean myContentLoadFailed;

  private static final Map<ContentRevision, ContentRevisionVirtualFile> ourMap = Maps.newWeakHashMap();

  public static ContentRevisionVirtualFile create(@Nonnull ContentRevision contentRevision) {
    synchronized(ourMap) {
      ContentRevisionVirtualFile revisionVirtualFile = ourMap.get(contentRevision);
      if (revisionVirtualFile == null) {
        revisionVirtualFile = new ContentRevisionVirtualFile(contentRevision);
        ourMap.put(contentRevision, revisionVirtualFile);
      }
      return revisionVirtualFile;
    }
  }

  private ContentRevisionVirtualFile(@Nonnull ContentRevision contentRevision) {
    super(contentRevision.getFile().getPath(), VcsFileSystem.getInstance());
    myContentRevision = contentRevision;
    setCharset(StandardCharsets.UTF_8);
  }

  @Override
  public boolean isDirectory() {
    return false;
  }

  @Override
  @Nonnull
  @RequiredUIAccess
  public byte[] contentsToByteArray() throws IOException {
    if (myContentLoadFailed || myProcessingBeforeContentsChange) {
      return ArrayUtil.EMPTY_BYTE_ARRAY;
    }
    if (myContent == null) {
      loadContent();
    }
    return myContent;
  }

  @RequiredUIAccess
  private void loadContent() {
    final VcsFileSystem vcsFileSystem = ((VcsFileSystem)getFileSystem());

    try {
      final String content = myContentRevision.getContent();
      if (content == null) {
        throw new VcsException("Could not load content");
      }
      fireBeforeContentsChange();

      myModificationStamp++;
      setRevision(myContentRevision.getRevisionNumber().asString());
      myContent = content.getBytes(getCharset());
      Application.get().runWriteAction(new Runnable() {
        @Override
        public void run() {
          vcsFileSystem.fireContentsChanged(this, ContentRevisionVirtualFile.this, 0);
        }
      });

    }
    catch (VcsException e) {
      myContentLoadFailed = true;
      Application.get().runWriteAction(new Runnable() {
        @Override
        public void run() {
          vcsFileSystem.fireBeforeFileDeletion(this, ContentRevisionVirtualFile.this);
        }
      });
      myContent = ArrayUtil.EMPTY_BYTE_ARRAY;
      setRevision("0");

      Messages.showMessageDialog(
        VcsLocalize.messageTextCouldNotLoadVirtualFileContent(getPresentableUrl(), e.getLocalizedMessage()).get(),
        VcsLocalize.messageTitleCouldNotLoadContent().get(),
        UIUtil.getInformationIcon()
      );

      Application.get().runWriteAction(new Runnable() {
        @Override
        public void run() {
          vcsFileSystem.fireFileDeleted(this, ContentRevisionVirtualFile.this, getName(), getParent());
        }
      });
    }
    catch (ProcessCanceledException ex) {
      myContent = ArrayUtil.EMPTY_BYTE_ARRAY;
    }
  }

  @Nonnull
  public ContentRevision getContentRevision() {
    return myContentRevision;
  }
}