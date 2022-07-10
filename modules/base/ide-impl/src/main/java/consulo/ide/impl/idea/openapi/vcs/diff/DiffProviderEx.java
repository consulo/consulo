package consulo.ide.impl.idea.openapi.vcs.diff;

import consulo.vcs.diff.DiffProvider;
import consulo.vcs.history.VcsRevisionNumber;
import consulo.virtualFileSystem.VirtualFile;
import consulo.ide.impl.idea.util.containers.ContainerUtil;

import java.util.Map;

/**
 * @author peter
 */
public abstract class DiffProviderEx implements DiffProvider {
  public Map<VirtualFile, VcsRevisionNumber> getCurrentRevisions(Iterable<VirtualFile> files) {
    return getCurrentRevisions(files, this);
  }

  public static Map<VirtualFile, VcsRevisionNumber> getCurrentRevisions(Iterable<VirtualFile> file, DiffProvider provider) {
    Map<VirtualFile, VcsRevisionNumber> result = ContainerUtil.newHashMap();
    for (VirtualFile virtualFile : file) {
      result.put(virtualFile, provider.getCurrentRevision(virtualFile));
    }
    return result;
  }
}
