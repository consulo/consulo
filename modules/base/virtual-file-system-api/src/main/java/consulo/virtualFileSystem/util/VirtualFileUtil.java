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
import consulo.logging.Logger;
import consulo.platform.Platform;
import consulo.util.collection.ContainerUtil;
import consulo.util.collection.SmartList;
import consulo.util.io.BufferExposingByteArrayInputStream;
import consulo.util.io.CharsetToolkit;
import consulo.util.io.FileUtil;
import consulo.util.io.URLUtil;
import consulo.util.lang.Comparing;
import consulo.util.lang.StringUtil;
import consulo.virtualFileSystem.*;
import consulo.virtualFileSystem.archive.ArchiveFileSystem;
import consulo.virtualFileSystem.archive.ArchiveFileType;
import consulo.virtualFileSystem.fileType.FileType;
import consulo.virtualFileSystem.fileType.FileTypeRegistry;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.Charset;
import java.nio.file.Path;
import java.util.*;
import java.util.function.Function;
import java.util.function.Predicate;

/**
 * Extract part of consulo.ide.impl.idea.openapi.vfs.VfsUtilCore
 */
public final class VirtualFileUtil {
  private static final Logger LOG = Logger.getInstance(VirtualFileUtil.class);

  public static final char VFS_SEPARATOR_CHAR = '/';
  private static final String PROTOCOL_DELIMITER = ":";
  private static final String MAILTO = "mailto";

  @Nullable
  public static VirtualFile getUserHomeDir() {
    final Path path = Platform.current().user().homePath();
    return LocalFileSystem.getInstance().findFileByNioFile(path);
  }

  @Nullable
  public static URL getURL(String url) throws MalformedURLException {
    return URLUtil.isAbsoluteURL(url) ? convertToURL(url) : new URL("file", "", url);
  }

  /**
   * Searches for the file specified by given java,net.URL.
   * Note that this method currently tested only for "file" and "jar" protocols under Unix and Windows
   *
   * @param url the URL to find file by
   * @return <code>{@link VirtualFile}</code> if the file was found, <code>null</code> otherwise
   */
  @Nullable
  public static VirtualFile findFileByURL(@Nonnull URL url) {
    VirtualFileManager virtualFileManager = VirtualFileManager.getInstance();
    return findFileByURL(url, virtualFileManager);
  }

  @Nullable
  public static VirtualFile findFileByURL(@Nonnull URL url, @Nonnull VirtualFileManager virtualFileManager) {
    String vfUrl = convertFromUrl(url);
    return virtualFileManager.findFileByUrl(vfUrl);
  }

  @Nonnull
  public static String convertFromUrl(@Nonnull URL url) {
    String protocol = url.getProtocol();
    String path = url.getPath();
    if (protocol.equals(URLUtil.JAR_PROTOCOL)) {
      if (StringUtil.startsWithConcatenation(path, URLUtil.FILE_PROTOCOL, PROTOCOL_DELIMITER)) {
        try {
          URL subURL = new URL(path);
          path = subURL.getPath();
        }
        catch (MalformedURLException e) {
          throw new RuntimeException(VfsBundle.message("url.parse.unhandled.exception"), e);
        }
      }
      else {
        throw new RuntimeException(new IOException(VfsBundle.message("url.parse.error", url.toExternalForm())));
      }
    }
    if (Platform.current().os().isWindows()) {
      while (!path.isEmpty() && path.charAt(0) == '/') {
        path = path.substring(1, path.length());
      }
    }

    path = URLUtil.unescapePercentSequences(path);
    return protocol + "://" + path;
  }

  /**
   * Converts VsfUrl info {@link URL}.
   *
   * @param vfsUrl VFS url (as constructed by {@link VirtualFile#getUrl()}
   * @return converted URL or null if error has occurred.
   */
  @Nullable
  public static URL convertToURL(@Nonnull String vfsUrl) {
    if (vfsUrl.startsWith("jar://") || vfsUrl.startsWith(StandardFileSystems.ZIP_PROTOCOL_PREFIX)) {
      try {
        // jar:// and zip:// have the same lenght
        return new URL("jar:file:///" + vfsUrl.substring(StandardFileSystems.ZIP_PROTOCOL_PREFIX.length()));
      }
      catch (MalformedURLException e) {
        return null;
      }
    }

    if (vfsUrl.startsWith(MAILTO)) {
      try {
        return new URL(vfsUrl);
      }
      catch (MalformedURLException e) {
        return null;
      }
    }

    String[] split = vfsUrl.split("://");

    if (split.length != 2) {
      LOG.debug("Malformed VFS URL: " + vfsUrl);
      return null;
    }

    String protocol = split[0];
    String path = split[1];

    try {
      if (protocol.equals(StandardFileSystems.FILE_PROTOCOL)) {
        return new URL(StandardFileSystems.FILE_PROTOCOL, "", path);
      }
      else {
        return URLUtil.internProtocol(new URL(vfsUrl));
      }
    }
    catch (MalformedURLException e) {
      LOG.debug("MalformedURLException occurred:" + e.getMessage());
      return null;
    }
  }

  /**
   * @return {@code true} if {@code file} is located under one of {@code roots} or equal to one of them
   */
  public static boolean isUnder(@Nonnull VirtualFile file, @Nullable Set<VirtualFile> roots) {
    if (roots == null || roots.isEmpty()) return false;

    VirtualFile parent = file;
    while (parent != null) {
      if (roots.contains(parent)) {
        return true;
      }
      parent = parent.getParent();
    }
    return false;
  }

  /**
   * @return {@code true} if {@code url} is located under one of {@code rootUrls} or equal to one of them
   */
  public static boolean isUnder(@Nonnull String url, @Nullable Collection<String> rootUrls) {
    if (rootUrls == null || rootUrls.isEmpty()) return false;

    for (String excludesUrl : rootUrls) {
      if (isEqualOrAncestor(excludesUrl, url)) {
        return true;
      }
    }
    return false;
  }

  public static boolean isEqualOrAncestor(@Nonnull String ancestorUrl, @Nonnull String fileUrl) {
    if (ancestorUrl.equals(fileUrl)) return true;
    if (StringUtil.endsWithChar(ancestorUrl, '/')) {
      return fileUrl.startsWith(ancestorUrl);
    }
    else {
      return StringUtil.startsWithConcatenation(fileUrl, ancestorUrl, "/");
    }
  }

  /**
   * @param urlOrPath Url for virtual file
   * @return file name
   */
  @Nullable
  public static String extractFileName(@Nullable final String urlOrPath) {
    if (urlOrPath == null) {
      return null;
    }
    final int index = urlOrPath.lastIndexOf(VirtualFileUtil.VFS_SEPARATOR_CHAR);
    return index < 0 ? null : urlOrPath.substring(index + 1);
  }

  @Nonnull
  public static String urlToPath(@Nullable String url) {
    if (url == null) return "";
    return VirtualFileManager.extractPath(url);
  }

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
        if (Platform.current().os().isWindows() && path.charAt(0) != '/') {
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

  @Nonnull
  public static String fixIDEAUrl(@Nonnull String ideaUrl) {
    final String ideaProtocolMarker = "://";
    int idx = ideaUrl.indexOf(ideaProtocolMarker);
    if (idx >= 0) {
      String s = ideaUrl.substring(0, idx);

      if (s.equals("jar") || s.equals(StandardFileSystems.ZIP_PROTOCOL)) {
        s = "jar:file";
      }
      final String urlWithoutProtocol = ideaUrl.substring(idx + ideaProtocolMarker.length());
      ideaUrl = s + ":" + (urlWithoutProtocol.startsWith("/") ? "" : "/") + urlWithoutProtocol;
    }

    return ideaUrl;
  }

  @Nonnull
  public static String toIdeaUrl(@Nonnull String url) {
    return toIdeaUrl(url, true);
  }

  @Nonnull
  public static String toIdeaUrl(@Nonnull String url, boolean removeLocalhostPrefix) {
    return URLUtil.toIdeaUrl(url, removeLocalhostPrefix);
  }

  @Nonnull
  public static String fixURLforIDEA(@Nonnull String url) {
    // removeLocalhostPrefix - false due to backward compatibility reasons
    return toIdeaUrl(url, false);
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
      if (!Platform.current().os().isWindows()) uri = "/" + uri;
    }
    else if (uri.startsWith("file:/")) {
      uri = uri.substring("file:/".length());
      if (!Platform.current().os().isWindows()) uri = "/" + uri;
    }
    else {
      uri = StringUtil.trimStart(uri, "file:");
    }

    VirtualFile file = null;

    if (uri.startsWith("jar:file:/")) {
      uri = uri.substring("jar:file:/".length());
      if (!Platform.current().os().isWindows()) uri = "/" + uri;
      file = VirtualFileManager.getInstance().findFileByUrl(StandardFileSystems.ZIP_PROTOCOL_PREFIX + uri);
    }
    else if (!Platform.current().os().isWindows() && StringUtil.startsWithChar(uri, '/') || Platform.current().os().isWindows() && uri.length() >= 2 && Character.isLetter(uri.charAt(0)) && uri.charAt(1) == ':') {
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

  /**
   * Gets relative path of <code>file</code> to <code>root</code> when it's possible
   * This method is designed to be used for file descriptions (in trees, lists etc.)
   *
   * @param file the file
   * @param root candidate to be parent file (Project base dir, any content roots etc.)
   * @return relative path of {@code file} or full path if {@code root} is not actual ancestor of {@code file}
   */
  @Nullable
  public static String getRelativeLocation(@Nullable VirtualFile file, @Nonnull VirtualFile root) {
    if (file == null) return null;
    String path = getRelativePath(file, root);
    return path != null ? path : file.getPresentableUrl();
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

  public static void processFileRecursivelyWithoutIgnored(@Nonnull final VirtualFile root, @Nonnull final Predicate<VirtualFile> processor) {
    final FileTypeRegistry ftm = FileTypeRegistry.getInstance();
    processFilesRecursively(root, processor, vf -> !ftm.isFileIgnored(vf));
  }

  public static boolean processFilesRecursively(@Nonnull VirtualFile root, @Nonnull Predicate<VirtualFile> processor) {
    if (!processor.test(root)) return false;

    if (root.isDirectory()) {
      final LinkedList<VirtualFile[]> queue = new LinkedList<>();

      queue.add(root.getChildren());

      do {
        final VirtualFile[] files = queue.removeFirst();

        for (VirtualFile file : files) {
          if (!processor.test(file)) return false;
          if (file.isDirectory()) {
            queue.add(file.getChildren());
          }
        }
      }
      while (!queue.isEmpty());
    }

    return true;
  }

  public static void processFilesRecursively(@Nonnull VirtualFile root, @Nonnull Predicate<VirtualFile> processor, @Nonnull Function<VirtualFile, Boolean> directoryFilter) {
    if (!processor.test(root)) return;

    if (root.isDirectory() && directoryFilter.apply(root)) {
      final LinkedList<VirtualFile[]> queue = new LinkedList<>();

      queue.add(root.getChildren());

      do {
        final VirtualFile[] files = queue.removeFirst();

        for (VirtualFile file : files) {
          if (!processor.test(file)) return;
          if (file.isDirectory() && directoryFilter.apply(file)) {
            queue.add(file.getChildren());
          }
        }
      }
      while (!queue.isEmpty());
    }
  }

  @Nonnull
  public static File virtualToIoFile(@Nonnull VirtualFile file) {
    return new File(VirtualFilePathUtil.toPresentableUrl(file.getUrl()));
  }

  @Nonnull
  public static List<File> virtualToIoFiles(@Nonnull Collection<VirtualFile> scope) {
    return ContainerUtil.map2List(scope, file -> virtualToIoFile(file));
  }

  @Nonnull
  public static List<VirtualFile> markDirty(boolean recursive, boolean reloadChildren, @Nonnull VirtualFile... files) {
    List<VirtualFile> list = ContainerUtil.filter(files, Objects::nonNull);
    if (list.isEmpty()) {
      return List.of();
    }

    for (VirtualFile file : list) {
      if (reloadChildren) {
        file.getChildren();
      }

      if (file instanceof NewVirtualFile) {
        if (recursive) {
          ((NewVirtualFile)file).markDirtyRecursively();
        }
        else {
          ((NewVirtualFile)file).markDirty();
        }
      }
    }
    return list;
  }

  /**
   * Refreshes the VFS information of the given files from the local file system.
   * <p>
   * This refresh is performed without help of the FileWatcher,
   * which means that all given files will be refreshed even if the FileWatcher didn't report any changes in them.
   * This method is slower, but more reliable, and should be preferred
   * when it is essential to make sure all the given VirtualFiles are actually refreshed from disk.
   * <p>
   * NB: when invoking synchronous refresh from a thread other than the event dispatch thread, the current thread must
   * NOT be in a read action.
   *
   * @see VirtualFile#refresh(boolean, boolean)
   */
  public static void markDirtyAndRefresh(boolean async, boolean recursive, boolean reloadChildren, @Nonnull VirtualFile... files) {
    List<VirtualFile> list = markDirty(recursive, reloadChildren, files);
    if (list.isEmpty()) return;
    LocalFileSystem.getInstance().refreshFiles(list, async, recursive, null);
  }

  public static String getUrlForLibraryRoot(@Nonnull File libraryRoot) {
    String path = FileUtil.toSystemIndependentName(libraryRoot.getAbsolutePath());
    final FileType fileTypeByFileName = FileTypeRegistry.getInstance().getFileTypeByFileName(libraryRoot.getName());
    if (fileTypeByFileName instanceof ArchiveFileType) {

      final String protocol = ((ArchiveFileType)fileTypeByFileName).getProtocol();

      return VirtualFileManager.constructUrl(protocol, path + ArchiveFileSystem.ARCHIVE_SEPARATOR);
    }
    else {
      return VirtualFileManager.constructUrl(LocalFileSystem.getInstance().getProtocol(), path);
    }
  }

  @Nonnull
  public static List<VirtualFile> getChildren(@Nonnull VirtualFile dir, @Nonnull VirtualFileFilter filter) {
    List<VirtualFile> result = null;
    for (VirtualFile child : dir.getChildren()) {
      if (filter.accept(child)) {
        if (result == null) result = new SmartList<>();
        result.add(child);
      }
    }
    return result != null ? result : List.of();
  }
}
