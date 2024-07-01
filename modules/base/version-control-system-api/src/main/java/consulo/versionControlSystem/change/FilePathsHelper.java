package consulo.versionControlSystem.change;

import consulo.platform.Platform;
import consulo.util.io.FileUtil;
import consulo.util.lang.StringUtil;
import consulo.versionControlSystem.FilePath;
import consulo.virtualFileSystem.VirtualFile;

public class FilePathsHelper {
  private FilePathsHelper() {
  }

  public static String convertPath(final VirtualFile vf) {
    return convertPath(vf.getPath());
  }

  public static String convertPath(final FilePath fp) {
    return convertPath(fp.getPath());
  }
  
  public static String convertWithLastSeparator(final VirtualFile vf) {
    return convertWithLastSeparatorImpl(vf.getPath(), vf.isDirectory());
  }

  public static String convertWithLastSeparator(final FilePath fp) {
    return convertWithLastSeparatorImpl(fp.getPath(), fp.isDirectory());
  }

  private static String convertWithLastSeparatorImpl(final String initPath, final boolean isDir) {
    final String path = isDir ? (initPath.endsWith("/") || initPath.endsWith("\\") ? initPath : initPath + "/") : initPath;
    return convertPath(path);
  }

  public static String convertPath(final String parent, final String subpath) {
    String convParent = FileUtil.toSystemIndependentName(parent);
    String convPath = FileUtil.toSystemIndependentName(subpath);

    String withSlash = StringUtil.trimEnd(convParent, "/") + "/" + StringUtil.trimStart(convPath, "/");
    return Platform.current().fs().isCaseSensitive() ? withSlash : withSlash.toUpperCase();
  }

  public static String convertPath(final String s) {
    String result = FileUtil.toSystemIndependentName(s);
    return Platform.current().fs().isCaseSensitive() ? result : result.toUpperCase();
  }
}
