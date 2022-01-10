
package com.intellij.openapi.vcs.changes;

import com.intellij.openapi.vcs.FilePath;
import com.intellij.openapi.vcs.history.VcsRevisionNumber;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * @author yole
*/
public class SimpleContentRevision implements ContentRevision {
  private final String myContent;
  private final FilePath myNewFilePath;
  private final String myRevision;

  public SimpleContentRevision(final String content, final FilePath newFilePath, final String revision) {
    myContent = content;
    myNewFilePath = newFilePath;
    myRevision = revision;
  }

  @Nullable
  public String getContent() {
    return myContent;
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

      public int compareTo(final VcsRevisionNumber o) {
        return 0;
      }
    };
  }
}