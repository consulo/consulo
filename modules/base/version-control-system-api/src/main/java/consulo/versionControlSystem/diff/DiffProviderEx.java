package consulo.versionControlSystem.diff;

import consulo.versionControlSystem.history.VcsRevisionNumber;
import consulo.virtualFileSystem.VirtualFile;

import java.util.HashMap;
import java.util.Map;

/**
 * @author peter
 */
public abstract class DiffProviderEx implements DiffProvider {
  public Map<VirtualFile, VcsRevisionNumber> getCurrentRevisions(Iterable<VirtualFile> files) {
    return getCurrentRevisions(files, this);
  }

  public static Map<VirtualFile, VcsRevisionNumber> getCurrentRevisions(Iterable<VirtualFile> file, DiffProvider provider) {
    Map<VirtualFile, VcsRevisionNumber> result = new HashMap<>();
    for (VirtualFile virtualFile : file) {
      result.put(virtualFile, provider.getCurrentRevision(virtualFile));
    }
    return result;
  }
}
