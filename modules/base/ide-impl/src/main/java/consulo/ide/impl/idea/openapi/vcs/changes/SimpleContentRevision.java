
package consulo.ide.impl.idea.openapi.vcs.changes;

import consulo.versionControlSystem.FilePath;
import consulo.versionControlSystem.history.VcsRevisionNumber;
import consulo.versionControlSystem.change.ContentRevision;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

/**
 * @author yole
*/
public class SimpleContentRevision implements ContentRevision {
  private final String myContent;
  private final FilePath myNewFilePath;
  private final String myRevision;

  public SimpleContentRevision(String content, FilePath newFilePath, String revision) {
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

      public int compareTo(VcsRevisionNumber o) {
        return 0;
      }
    };
  }
}