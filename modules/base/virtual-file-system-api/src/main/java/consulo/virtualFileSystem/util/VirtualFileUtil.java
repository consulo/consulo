/*
 * Copyright 2013-2022 consulo.io
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
package consulo.virtualFileSystem.util;

import consulo.application.WriteAction;
import consulo.application.util.SystemInfo;
import consulo.logging.Logger;
import consulo.util.io.BufferExposingByteArrayInputStream;
import consulo.util.io.CharsetToolkit;
import consulo.util.io.FileUtil;
import consulo.util.io.URLUtil;
import consulo.util.lang.Comparing;
import consulo.util.lang.StringUtil;
import consulo.virtualFileSystem.*;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.Charset;
import java.util.Collection;
import java.util.function.Predicate;

/**
 * Extract part of com.intellij.openapi.vfs.VfsUtilCore
 */
public final class VirtualFileUtil {
  private static final Logger LOG = Logger.getInstance(VirtualFileUtil.class);
  public static final char VFS_SEPARATOR_CHAR = '/';

  public static void saveText(@Nonnull VirtualFile file, @Nonnull String text) throws IOException {
    Charset charset = file.getCharset();
    file.setBinaryContent(text.getBytes(charset));
  }

  /**
   * @return correct URL, must be used only for external communication
   */
  @Nonnull
  public static URI toUri(@Nonnull VirtualFile file) {
    String path = file.getPath();
    try {
      String protocol = file.getFileSystem().getProtocol();
      if (file.isInLocalFileSystem()) {
        if (SystemInfo.isWindows && path.charAt(0) != '/') {
          path = '/' + path;
        }
        return new URI(protocol, "", path, null, null);
      }
      if (URLUtil.HTTP_PROTOCOL.equals(protocol)) {
        return new URI(URLUtil.HTTP_PROTOCOL + URLUtil.SCHEME_SEPARATOR + path);
      }
      return new URI(protocol, path, null);
    }
    catch (URISyntaxException e) {
      throw new IllegalArgumentException(e);
    }
  }

  @Nullable
  public static VirtualFile findFileByIoFile(@Nonnull File file, boolean refreshIfNeeded) {
    LocalFileSystem fileSystem = LocalFileSystem.getInstance();
    VirtualFile virtualFile = fileSystem.findFileByIoFile(file);
    if (refreshIfNeeded && (virtualFile == null || !virtualFile.isValid())) {
      virtualFile = fileSystem.refreshAndFindFileByIoFile(file);
    }
    return virtualFile;
  }

  public static VirtualFile createDirectories(@Nonnull final String directoryPath) throws IOException {
    return WriteAction.compute(() -> {
      VirtualFile res = createDirectoryIfMissing(directoryPath);
      return res;
    });
  }

  public static VirtualFile createDirectoryIfMissing(VirtualFile parent, String relativePath) throws IOException {
    for (String each : StringUtil.split(relativePath, "/")) {
      VirtualFile child = parent.findChild(each);
      if (child == null) {
        child = parent.createChildDirectory(LocalFileSystem.getInstance(), each);
      }
      parent = child;
    }
    return parent;
  }

  @Nullable
  public static VirtualFile createDirectoryIfMissing(@Nonnull String directoryPath) throws IOException {
    String path = FileUtil.toSystemIndependentName(directoryPath);
    final VirtualFile file = LocalFileSystem.getInstance().refreshAndFindFileByPath(path);
    if (file == null) {
      int pos = path.lastIndexOf('/');
      if (pos < 0) return null;
      VirtualFile parent = createDirectoryIfMissing(path.substring(0, pos));
      if (parent == null) return null;
      final String dirName = path.substring(pos + 1);
      VirtualFile child = parent.findChild(dirName);
      if (child != null && child.isDirectory()) return child;
      return parent.createChildDirectory(LocalFileSystem.getInstance(), dirName);
    }
    return file;
  }

  @Nullable
  public static VirtualFile findRelativeFile(@Nullable VirtualFile base, String... path) {
    VirtualFile file = base;

    for (String pathElement : path) {
      if (file == null) return null;
      if ("..".equals(pathElement)) {
        file = file.getParent();
      }
      else {
        file = file.findChild(pathElement);
      }
    }

    return file;
  }

  @Nullable
  public static VirtualFile findRelativeFile(@Nonnull String uri, @Nullable VirtualFile base) {
    if (base != null) {
      if (!base.isValid()) {
        LOG.error("Invalid file name: " + base.getName() + ", url: " + uri);
      }
    }

    uri = uri.replace('\\', '/');

    if (uri.startsWith("file:///")) {
      uri = uri.substring("file:///".length());
      if (!SystemInfo.isWindows) uri = "/" + uri;
    }
    else if (uri.startsWith("file:/")) {
      uri = uri.substring("file:/".length());
      if (!SystemInfo.isWindows) uri = "/" + uri;
    }
    else {
      uri = StringUtil.trimStart(uri, "file:");
    }

    VirtualFile file = null;

    if (uri.startsWith("jar:file:/")) {
      uri = uri.substring("jar:file:/".length());
      if (!SystemInfo.isWindows) uri = "/" + uri;
      file = VirtualFileManager.getInstance().findFileByUrl(StandardFileSystems.ZIP_PROTOCOL_PREFIX + uri);
    }
    else if (!SystemInfo.isWindows && StringUtil.startsWithChar(uri, '/') || SystemInfo.isWindows && uri.length() >= 2 && Character.isLetter(uri.charAt(0)) && uri.charAt(1) == ':') {
      file = StandardFileSystems.local().findFileByPath(uri);
    }

    if (file == null && uri.contains(URLUtil.ARCHIVE_SEPARATOR)) {
      file = StandardFileSystems.zip().findFileByPath(uri);
      if (file == null && base == null) {
        file = VirtualFileManager.getInstance().findFileByUrl(uri);
      }
    }

    if (file == null) {
      if (base == null) return StandardFileSystems.local().findFileByPath(uri);
      if (!base.isDirectory()) base = base.getParent();
      if (base == null) return StandardFileSystems.local().findFileByPath(uri);
      file = VirtualFileManager.getInstance().findFileByUrl(base.getUrl() + "/" + uri);
      if (file == null) return null;
    }

    return file;
  }

  @Nullable
  public static String getRelativePath(@Nonnull VirtualFile file, @Nonnull VirtualFile ancestor) {
    return getRelativePath(file, ancestor, VFS_SEPARATOR_CHAR);
  }

  /**
   * Gets the relative path of <code>file</code> to its <code>ancestor</code>. Uses <code>separator</code> for
   * separating files.
   *
   * @param file      the file
   * @param ancestor  parent file
   * @param separator character to use as files separator
   * @return the relative path or {@code null} if {@code ancestor} is not ancestor for {@code file}
   */
  @Nullable
  public static String getRelativePath(@Nonnull VirtualFile file, @Nonnull VirtualFile ancestor, char separator) {
    if (!file.getFileSystem().equals(ancestor.getFileSystem())) {
      return null;
    }

    int length = 0;
    VirtualFile parent = file;
    while (true) {
      if (parent == null) return null;
      if (parent.equals(ancestor)) break;
      if (length > 0) {
        length++;
      }
      length += parent.getNameSequence().length();
      parent = parent.getParent();
    }

    char[] chars = new char[length];
    int index = chars.length;
    parent = file;
    while (true) {
      if (parent.equals(ancestor)) break;
      if (index < length) {
        chars[--index] = separator;
      }
      CharSequence name = parent.getNameSequence();
      for (int i = name.length() - 1; i >= 0; i--) {
        chars[--index] = name.charAt(i);
      }
      parent = parent.getParent();
    }
    return new String(chars);
  }

  /**
   * Returns {@code true} if given virtual file represents broken symbolic link (which points to non-existent file).
   */
  public static boolean isBrokenLink(@Nonnull VirtualFile file) {
    return file.is(VFileProperty.SYMLINK) && file.getCanonicalPath() == null;
  }

  /**
   * Returns {@code true} if given virtual file represents broken or recursive symbolic link.
   */
  public static boolean isInvalidLink(@Nonnull VirtualFile link) {
    final VirtualFile target = link.getCanonicalFile();
    return target == null || target.equals(link) || isAncestor(target, link, true);
  }

  /**
   * Makes a copy of the <code>file</code> in the <code>toDir</code> folder and returns it.
   *
   * @param requestor any object to control who called this method. Note that
   *                  it is considered to be an external change if <code>requestor</code> is <code>null</code>.
   *                  See {@link VirtualFileEvent#getRequestor}
   * @param file      file to make a copy of
   * @param toDir     directory to make a copy in
   * @return a copy of the file
   * @throws IOException if file failed to be copied
   */
  @Nonnull
  public static VirtualFile copyFile(Object requestor, @Nonnull VirtualFile file, @Nonnull VirtualFile toDir) throws IOException {
    return copyFile(requestor, file, toDir, file.getName());
  }

  /**
   * Makes a copy of the <code>file</code> in the <code>toDir</code> folder with the <code>newName</code> and returns it.
   *
   * @param requestor any object to control who called this method. Note that
   *                  it is considered to be an external change if <code>requestor</code> is <code>null</code>.
   *                  See {@link VirtualFileEvent#getRequestor}
   * @param file      file to make a copy of
   * @param toDir     directory to make a copy in
   * @param newName   new name of the file
   * @return a copy of the file
   * @throws IOException if file failed to be copied
   */
  @Nonnull
  public static VirtualFile copyFile(Object requestor, @Nonnull VirtualFile file, @Nonnull VirtualFile toDir, @Nonnull String newName) throws IOException {
    final VirtualFile newChild = toDir.createChildData(requestor, newName);
    newChild.setBinaryContent(file.contentsToByteArray());
    return newChild;
  }

  @Nonnull
  public static VirtualFile[] toVirtualFileArray(@Nonnull Collection<? extends VirtualFile> files) {
    int size = files.size();
    if (size == 0) return VirtualFile.EMPTY_ARRAY;
    //noinspection SSBasedInspection
    return files.toArray(new VirtualFile[size]);
  }

  public static String pathToUrl(@Nonnull String path) {
    return VirtualFileManager.constructUrl(URLUtil.FILE_PROTOCOL, path);
  }

  public static boolean iterateChildrenRecursively(@Nonnull final VirtualFile root, @Nullable final VirtualFileFilter filter, @Nonnull final Predicate<VirtualFile> iterator) {
    final VirtualFileVisitor.Result result = visitChildrenRecursively(root, new VirtualFileVisitor() {
      @Nonnull
      @Override
      public Result visitFileEx(@Nonnull VirtualFile file) {
        if (filter != null && !filter.accept(file)) return SKIP_CHILDREN;
        if (!iterator.test(file)) return skipTo(root);
        return CONTINUE;
      }
    });
    return !Comparing.equal(result.skipToParent, root);
  }

  @SuppressWarnings({"UnsafeVfsRecursion", "Duplicates"})
  @Nonnull
  public static VirtualFileVisitor.Result visitChildrenRecursively(@Nonnull VirtualFile file, @Nonnull VirtualFileVisitor<?> visitor) throws VirtualFileVisitor.VisitorException {
    boolean pushed = false;
    try {
      final boolean visited = visitor.allowVisitFile(file);
      if (visited) {
        VirtualFileVisitor.Result result = visitor.visitFileEx(file);
        if (result.skipChildren) return result;
      }

      Iterable<VirtualFile> childrenIterable = null;
      VirtualFile[] children = null;

      try {
        if (file.isValid() && visitor.allowVisitChildren(file) && !visitor.depthLimitReached()) {
          childrenIterable = visitor.getChildrenIterable(file);
          if (childrenIterable == null) {
            children = file.getChildren();
          }
        }
      }
      catch (InvalidVirtualFileAccessException e) {
        LOG.info("Ignoring: " + e.getMessage());
        return VirtualFileVisitor.CONTINUE;
      }

      if (childrenIterable != null) {
        visitor.saveValue();
        pushed = true;
        for (VirtualFile child : childrenIterable) {
          VirtualFileVisitor.Result result = visitChildrenRecursively(child, visitor);
          if (result.skipToParent != null && !Comparing.equal(result.skipToParent, child)) return result;
        }
      }
      else if (children != null && children.length != 0) {
        visitor.saveValue();
        pushed = true;
        for (VirtualFile child : children) {
          VirtualFileVisitor.Result result = visitChildrenRecursively(child, visitor);
          if (result.skipToParent != null && !Comparing.equal(result.skipToParent, child)) return result;
        }
      }

      if (visited) {
        visitor.afterChildrenVisited(file);
      }

      return VirtualFileVisitor.CONTINUE;
    }
    finally {
      visitor.restoreValue(pushed);
    }
  }

  public static <E extends Exception> VirtualFileVisitor.Result visitChildrenRecursively(@Nonnull VirtualFile file, @Nonnull VirtualFileVisitor visitor, @Nonnull Class<E> eClass) throws E {
    try {
      return visitChildrenRecursively(file, visitor);
    }
    catch (VirtualFileVisitor.VisitorException e) {
      final Throwable cause = e.getCause();
      if (eClass.isInstance(cause)) {
        throw eClass.cast(cause);
      }
      throw e;
    }
  }

  @Nonnull
  public static InputStream byteStreamSkippingBOM(@Nonnull byte[] buf, @Nonnull VirtualFile file) throws IOException {
    @SuppressWarnings("IOResourceOpenedButNotSafelyClosed") BufferExposingByteArrayInputStream stream = new BufferExposingByteArrayInputStream(buf);
    return inputStreamSkippingBOM(stream, file);
  }

  @Nonnull
  public static InputStream inputStreamSkippingBOM(@Nonnull InputStream stream, @SuppressWarnings("UnusedParameters") @Nonnull VirtualFile file) throws IOException {
    return CharsetToolkit.inputStreamSkippingBOM(stream);
  }

  @Nonnull
  public static OutputStream outputStreamAddingBOM(@Nonnull OutputStream stream, @Nonnull VirtualFile file) throws IOException {
    byte[] bom = file.getBOM();
    if (bom != null) {
      stream.write(bom);
    }
    return stream;
  }

  /**
   * Checks whether the <code>ancestor {@link VirtualFile}</code> is parent of <code>file
   * {@link VirtualFile}</code>.
   *
   * @param ancestor the file
   * @param file     the file
   * @param strict   if <code>false</code> then this method returns <code>true</code> if <code>ancestor</code>
   *                 and <code>file</code> are equal
   * @return <code>true</code> if <code>ancestor</code> is parent of <code>file</code>; <code>false</code> otherwise
   */
  public static boolean isAncestor(@Nonnull VirtualFile ancestor, @Nonnull VirtualFile file, boolean strict) {
    if (!file.getFileSystem().equals(ancestor.getFileSystem())) return false;
    VirtualFile parent = strict ? file.getParent() : file;
    while (true) {
      if (parent == null) return false;
      if (parent.equals(ancestor)) return true;
      parent = parent.getParent();
    }
  }

  @Nonnull
  public static File virtualToIoFile(@Nonnull VirtualFile file) {
    return new File(VirtualFilePathUtil.toPresentableUrl(file.getUrl()));
  }
}
