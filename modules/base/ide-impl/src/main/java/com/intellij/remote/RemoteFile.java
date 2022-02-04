package com.intellij.remote;

import com.intellij.openapi.util.io.FileUtil;
import javax.annotation.Nonnull;

/**
 * @author traff
 */
public class RemoteFile {

  private final boolean myWin;
  private final String myPath;

  public RemoteFile(@Nonnull String path, boolean isWin) {
    myPath = toSystemDependent(path, isWin);
    myWin = isWin;
  }

  public RemoteFile(@Nonnull String parent, String child) {
    this(resolveChild(parent, child, isWindowsPath(parent)), isWindowsPath(parent));
  }

  public RemoteFile(@Nonnull String parent, String child, boolean isWin) {
    this(resolveChild(parent, child, isWin), isWin);
  }

  @Nonnull
  public String getName() {
    int ind = myPath.lastIndexOf(getSeparator(myWin));
    if (ind != -1 && ind < myPath.length() - 1) { //not last char
      return myPath.substring(ind + 1);
    }
    else {
      return myPath;
    }
  }

  private static String resolveChild(@Nonnull String parent, @Nonnull String child, boolean win) {
    String separator = getSeparator(win);

    String path;
    if (parent.endsWith(separator)) {
      path = parent + child;
    }
    else {
      path = parent + separator + child;
    }
    return path;
  }

  private static String getSeparator(boolean win) {
    String separator;
    if (win) {
      separator = "\\";
    }
    else {
      separator = "/";
    }
    return separator;
  }


  public String getPath() {
    return myPath;
  }

  public boolean isWin() {
    return isWindowsPath(myPath);
  }

  public static boolean isWindowsPath(@Nonnull String path) {
    path = RemoteSdkCredentialsHolder.getInterpreterPathFromFullPath(path);

    return (path.length() > 1 && path.charAt(1) == ':');
  }

  private static String toSystemDependent(@Nonnull String path, boolean isWin) {
    char separator = isWin ? '\\' : '/';
    return FileUtil.toSystemIndependentName(path).replace('/', separator);
  }

  public static RemoteFileBuilder detectSystemByPath(@Nonnull String path) {
    return new RemoteFileBuilder(isWindowsPath(path));
  }

  public static RemoteFile createRemoteFile(String path, String script) {
    return detectSystemByPath(path).createRemoteFile(path, script);
  }

  public static RemoteFile createRemoteFile(String path) {
    return detectSystemByPath(path).createRemoteFile(path);
  }

  public static RemoteFile createRemoteFile(final String path, final String script, final boolean isWindows) {
    return new RemoteFileBuilder(isWindows).createRemoteFile(path, script);
  }

  public static class RemoteFileBuilder {
    private final boolean isWin;

    private RemoteFileBuilder(boolean win) {
      isWin = win;
    }

    public RemoteFile createRemoteFile(String path) {
      return new RemoteFile(path, isWin);
    }

    public RemoteFile createRemoteFile(String path, String child) {
      return new RemoteFile(path, child, isWin);
    }
  }
}
