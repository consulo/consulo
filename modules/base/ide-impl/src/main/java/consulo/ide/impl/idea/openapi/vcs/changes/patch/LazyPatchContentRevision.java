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
package consulo.ide.impl.idea.openapi.vcs.changes.patch;

import consulo.versionControlSystem.change.patch.TextFilePatch;
import consulo.ide.impl.idea.openapi.diff.impl.patch.apply.GenericPatchApplier;
import consulo.document.Document;
import consulo.document.FileDocumentManager;
import consulo.versionControlSystem.FilePath;
import consulo.versionControlSystem.change.ContentRevision;
import consulo.versionControlSystem.history.VcsRevisionNumber;
import consulo.virtualFileSystem.VirtualFile;
import consulo.application.AccessRule;

import jakarta.annotation.Nonnull;

public class LazyPatchContentRevision implements ContentRevision {
  private volatile String myContent;
  private final VirtualFile myVf;
  private final FilePath myNewFilePath;
  private final String myRevision;
  private final TextFilePatch myPatch;
  private volatile boolean myPatchApplyFailed;

  public LazyPatchContentRevision(VirtualFile vf, FilePath newFilePath, String revision, TextFilePatch patch) {
    myVf = vf;
    myNewFilePath = newFilePath;
    myRevision = revision;
    myPatch = patch;
  }

  @Override
  public String getContent() {
    if (myContent == null) {
      String localContext = AccessRule.read(() -> {
        Document doc = FileDocumentManager.getInstance().getDocument(myVf);
        if(doc == null) {
          return null;
        }

        return doc.getText();
      });

      if (localContext == null) {
        myPatchApplyFailed = true;
        return null;
      }

      GenericPatchApplier applier = new GenericPatchApplier(localContext, myPatch.getHunks());
      if (applier.execute()) {
        myContent = applier.getAfter();
      } else {
        myPatchApplyFailed = true;
      }
    }
    return myContent;
  }

  public boolean isPatchApplyFailed() {
    return myPatchApplyFailed;
  }

  @Nonnull
  public FilePath getFile() {
    return myNewFilePath;
  }

  @Nonnull
  public VcsRevisionNumber getRevisionNumber() {
    return new VcsRevisionNumber() {
      public String asString() {
        return myRevision;
      }

      public int compareTo(VcsRevisionNumber o) {
        return 0;
      }
    };
  }
}
