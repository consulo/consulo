package consulo.versionControlSystem.change;

import consulo.platform.Platform;
import consulo.util.io.FileUtil;
import consulo.util.lang.StringUtil;
import consulo.versionControlSystem.FilePath;
import consulo.virtualFileSystem.VirtualFile;

public class FilePathsHelper {
  private FilePathsHelper() {
  }

  public static String convertPath(VirtualFile vf) {
    return convertPath(vf.getPath());
  }

  public static String convertPath(FilePath fp) {
    return convertPath(fp.getPath());
  }
  
  public static String convertWithLastSeparator(VirtualFile vf) {
    return convertWithLastSeparatorImpl(vf.getPath(), vf.isDirectory());
  }

  public static String convertWithLastSeparator(FilePath fp) {
    return convertWithLastSeparatorImpl(fp.getPath(), fp.isDirectory());
  }

  private static String convertWithLastSeparatorImpl(String initPath, boolean isDir) {
    String path = isDir ? (initPath.endsWith("/") || initPath.endsWith("\\") ? initPath : initPath + "/") : initPath;
    return convertPath(path);
  }

  public static String convertPath(String parent, String subpath) {
    String convParent = FileUtil.toSystemIndependentName(parent);
    String convPath = FileUtil.toSystemIndependentName(subpath);

    String withSlash = StringUtil.trimEnd(convParent, "/") + "/" + StringUtil.trimStart(convPath, "/");
    return Platform.current().fs().isCaseSensitive() ? withSlash : withSlash.toUpperCase();
  }

  public static String convertPath(String s) {
    String result = FileUtil.toSystemIndependentName(s);
    return Platform.current().fs().isCaseSensitive() ? result : result.toUpperCase();
  }
}
