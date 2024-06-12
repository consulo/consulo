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
package consulo.ide.impl.idea.openapi.vcs.history;

import consulo.application.ApplicationManager;
import consulo.application.util.function.Computable;
import consulo.document.Document;
import consulo.document.FileDocumentManager;
import consulo.ui.ex.awt.Messages;
import consulo.ui.ex.awt.UIUtil;
import consulo.versionControlSystem.RepositoryLocation;
import consulo.versionControlSystem.VcsException;
import consulo.versionControlSystem.history.VcsFileRevision;
import consulo.versionControlSystem.history.VcsRevisionNumber;
import consulo.versionControlSystem.localize.VcsLocalize;
import consulo.virtualFileSystem.VirtualFile;
import jakarta.annotation.Nullable;

import java.io.IOException;
import java.util.Date;


public class CurrentRevision implements VcsFileRevision {
  private final VirtualFile myFile;
  public static final String CURRENT = VcsLocalize.vcsRevisionNameCurrent().get();
  private final VcsRevisionNumber myRevisionNumber;

  public CurrentRevision(VirtualFile file, VcsRevisionNumber revision) {
    myFile = file;
    myRevisionNumber = revision;
  }

  @Override
  public String getCommitMessage() {
    return "[" + CURRENT + "]";
  }

  @Override
  public byte[] loadContent() throws IOException, VcsException {
    return getContent();
  }

  @Override
  public Date getRevisionDate() {
    return new Date(myFile.getTimeStamp());
  }

  @Override
  public byte[] getContent() throws IOException, VcsException {
    try {
      Document document = ApplicationManager.getApplication().runReadAction(new Computable<Document>() {
        @Override
        public Document compute() {
          return FileDocumentManager.getInstance().getDocument(myFile);
        }
      });
      if (document != null) {
        return document.getText().getBytes(myFile.getCharset().name());
      }
      else {
        return myFile.contentsToByteArray();
      }
    }
    catch (final IOException e) {
      UIUtil.invokeLaterIfNeeded(() -> Messages.showMessageDialog(
        e.getLocalizedMessage(),
        VcsLocalize.messageTextCouldNotLoadFileContent().get(),
        Messages.getErrorIcon()
      ));
      return null;
    }

  }

  @Override
  public String getAuthor() {
    return "";
  }

  @Override
  public VcsRevisionNumber getRevisionNumber() {
    return myRevisionNumber;
  }

  @Override
  public String getBranchName() {
    return null;
  }

  @Nullable
  @Override
  public RepositoryLocation getChangedRepositoryPath() {
    return null;  // use initial url..
  }
}
