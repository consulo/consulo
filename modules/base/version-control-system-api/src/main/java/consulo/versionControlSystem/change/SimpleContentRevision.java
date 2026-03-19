
package consulo.versionControlSystem.change;

import consulo.versionControlSystem.FilePath;
import consulo.versionControlSystem.history.VcsRevisionNumber;
import org.jspecify.annotations.Nullable;

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

  public @Nullable String getContent() {
    return myContent;
  }

  
  public FilePath getFile() {
    return myNewFilePath;
  }

  
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