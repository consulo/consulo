
package consulo.ide.impl.idea.openapi.diff.impl.patch;

import consulo.versionControlSystem.FilePath;
import consulo.virtualFileSystem.VirtualFile;
import consulo.versionControlSystem.util.VcsUtil;

import java.util.HashMap;
import java.util.Map;

/**
 * @author yole
 */
public class ApplyPatchContext {
  private final VirtualFile myBaseDir;
  private final int mySkipTopDirs;
  private final boolean myCreateDirectories;
  private final boolean myAllowRename;
  private final Map<VirtualFile, FilePath> myPathsBeforeRename = new HashMap<VirtualFile, FilePath>();

  public ApplyPatchContext(VirtualFile baseDir, int skipTopDirs, boolean createDirectories, boolean allowRename) {
    myBaseDir = baseDir;
    mySkipTopDirs = skipTopDirs;
    myCreateDirectories = createDirectories;
    myAllowRename = allowRename;
  }

  public VirtualFile getBaseDir() {
    return myBaseDir;
  }

  public int getSkipTopDirs() {
    return mySkipTopDirs;
  }

  public boolean isAllowRename() {
    return myAllowRename;
  }

  public boolean isCreateDirectories() {
    return myCreateDirectories;
  }

  public ApplyPatchContext getPrepareContext() {
    return new ApplyPatchContext(myBaseDir, mySkipTopDirs, false, false);
  }

  public void registerBeforeRename(VirtualFile file) {
    FilePath path = VcsUtil.getFilePath(file);
    myPathsBeforeRename.put(file, path);
  }

  public FilePath getPathBeforeRename(VirtualFile file) {
    FilePath path = myPathsBeforeRename.get(file);
    if (path != null) return path;
    return VcsUtil.getFilePath(file);
  }
}
