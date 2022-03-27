/*
 * Copyright 2000-2016 JetBrains s.r.o.
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
package com.intellij.openapi.vfs;

import com.intellij.openapi.util.Comparing;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.util.Function;
import com.intellij.util.containers.ContainerUtil;
import com.intellij.util.containers.Convertor;
import com.intellij.util.containers.DistinctRootsCollection;
import com.intellij.util.io.URLUtil;
import com.intellij.util.text.StringFactory;
import consulo.application.util.SystemInfo;
import consulo.application.util.function.Processor;
import consulo.content.ContentIterator;
import consulo.logging.Logger;
import consulo.virtualFileSystem.*;
import consulo.virtualFileSystem.event.VirtualFileEvent;
import consulo.virtualFileSystem.impl.internal.RawFileLoaderImpl;
import consulo.virtualFileSystem.util.VirtualFilePathUtil;
import consulo.virtualFileSystem.util.VirtualFileUtil;
import consulo.virtualFileSystem.util.VirtualFileVisitor;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.*;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.*;

import static consulo.virtualFileSystem.util.VirtualFileVisitor.VisitorException;

public class VfsUtilCore {
  private static final Logger LOG = Logger.getInstance(VfsUtilCore.class);

  private static final String MAILTO = "mailto";

  public static final String LOCALHOST_URI_PATH_PREFIX = URLUtil.LOCALHOST_URI_PATH_PREFIX;
  public static final char VFS_SEPARATOR_CHAR = VirtualFileUtil.VFS_SEPARATOR_CHAR;

  private static final String PROTOCOL_DELIMITER = ":";

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
    return VirtualFileUtil.isAncestor(ancestor, file, strict);
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

  public static boolean isAncestor(@Nonnull File ancestor, @Nonnull File file, boolean strict) {
    return FileUtil.isAncestor(ancestor, file, strict);
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
    return VirtualFileUtil.getRelativePath(file, ancestor);
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
    return VirtualFileUtil.getRelativePath(file, ancestor, separator);
  }

  @Nullable
  public static VirtualFile getVirtualFileForJar(@Nullable VirtualFile entryVFile) {
    if (entryVFile == null) return null;
    final String path = entryVFile.getPath();
    final int separatorIndex = path.indexOf("!/");
    if (separatorIndex < 0) return null;

    String localPath = path.substring(0, separatorIndex);
    return VirtualFileManager.getInstance().findFileByUrl("file://" + localPath);
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
    return VirtualFileUtil.copyFile(requestor, file, toDir);
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
    return VirtualFileUtil.copyFile(requestor, file, toDir, newName);
  }

  @Nonnull
  public static InputStream byteStreamSkippingBOM(@Nonnull byte[] buf, @Nonnull VirtualFile file) throws IOException {
    return VirtualFileUtil.byteStreamSkippingBOM(buf, file);
  }

  @Nonnull
  public static InputStream inputStreamSkippingBOM(@Nonnull InputStream stream, @SuppressWarnings("UnusedParameters") @Nonnull VirtualFile file) throws IOException {
    return VirtualFileUtil.inputStreamSkippingBOM(stream, file);
  }

  @Nonnull
  public static OutputStream outputStreamAddingBOM(@Nonnull OutputStream stream, @Nonnull VirtualFile file) throws IOException {
    return VirtualFileUtil.outputStreamAddingBOM(stream, file);
  }

  public static boolean iterateChildrenRecursively(@Nonnull final VirtualFile root, @Nullable final VirtualFileFilter filter, @Nonnull final ContentIterator iterator) {
    return VirtualFileUtil.iterateChildrenRecursively(root, filter, iterator);
  }

  @SuppressWarnings({"UnsafeVfsRecursion", "Duplicates"})
  @Nonnull
  public static VirtualFileVisitor.Result visitChildrenRecursively(@Nonnull VirtualFile file, @Nonnull VirtualFileVisitor<?> visitor) throws VisitorException {
    return VirtualFileUtil.visitChildrenRecursively(file, visitor);
  }

  public static <E extends Exception> VirtualFileVisitor.Result visitChildrenRecursively(@Nonnull VirtualFile file, @Nonnull VirtualFileVisitor visitor, @Nonnull Class<E> eClass) throws E {
    return VirtualFileUtil.visitChildrenRecursively(file, visitor, eClass);
  }

  /**
   * Returns {@code true} if given virtual file represents broken symbolic link (which points to non-existent file).
   */
  public static boolean isBrokenLink(@Nonnull VirtualFile file) {
    return VirtualFileUtil.isBrokenLink(file);
  }

  /**
   * Returns {@code true} if given virtual file represents broken or recursive symbolic link.
   */
  public static boolean isInvalidLink(@Nonnull VirtualFile link) {
    return VirtualFileUtil.isInvalidLink(link);
  }

  @Nonnull
  public static String loadText(@Nonnull VirtualFile file) throws IOException {
    return loadText(file, (int)file.getLength());
  }

  @Nonnull
  public static String loadText(@Nonnull VirtualFile file, int length) throws IOException {
    InputStreamReader reader = new InputStreamReader(file.getInputStream(), file.getCharset());
    try {
      return StringFactory.createShared(FileUtil.loadText(reader, length));
    }
    finally {
      reader.close();
    }
  }

  @Nonnull
  public static byte[] loadBytes(@Nonnull VirtualFile file) throws IOException {
    return RawFileLoader.getInstance().isTooLarge(file.getLength()) ? FileUtil.loadFirstAndClose(file.getInputStream(), RawFileLoaderImpl.LARGE_FILE_PREVIEW_SIZE) : file.contentsToByteArray();
  }

  @Nonnull
  public static VirtualFile[] toVirtualFileArray(@Nonnull Collection<? extends VirtualFile> files) {
    return VirtualFileUtil.toVirtualFileArray(files);
  }

  @Nonnull
  public static String urlToPath(@Nullable String url) {
    if (url == null) return "";
    return VirtualFileManager.extractPath(url);
  }

  @Nonnull
  public static File virtualToIoFile(@Nonnull VirtualFile file) {
    return new File(VirtualFilePathUtil.toPresentableUrl(file.getUrl()));
  }

  @Nonnull
  public static String pathToUrl(@Nonnull String path) {
    return VirtualFileUtil.pathToUrl(path);
  }

  public static List<File> virtualToIoFiles(@Nonnull Collection<VirtualFile> scope) {
    return ContainerUtil.map2List(scope, new Function<VirtualFile, File>() {
      @Override
      public File fun(VirtualFile file) {
        return virtualToIoFile(file);
      }
    });
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
    if (SystemInfo.isWindows || SystemInfo.isOS2) {
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

  @Nullable
  public static VirtualFile findRelativeFile(@Nonnull String uri, @Nullable VirtualFile base) {
    return VirtualFileUtil.findRelativeFile(uri, base);
  }

  public static boolean processFilesRecursively(@Nonnull VirtualFile root, @Nonnull Processor<VirtualFile> processor) {
    if (!processor.process(root)) return false;

    if (root.isDirectory()) {
      final LinkedList<VirtualFile[]> queue = new LinkedList<VirtualFile[]>();

      queue.add(root.getChildren());

      do {
        final VirtualFile[] files = queue.removeFirst();

        for (VirtualFile file : files) {
          if (!processor.process(file)) return false;
          if (file.isDirectory()) {
            queue.add(file.getChildren());
          }
        }
      }
      while (!queue.isEmpty());
    }

    return true;
  }

  /**
   * Gets the common ancestor for passed files, or null if the files do not have common ancestors.
   *
   * @param file1 fist file
   * @param file2 second file
   * @return common ancestor for the passed files. Returns <code>null</code> if
   * the files do not have common ancestor
   */
  @Nullable
  public static VirtualFile getCommonAncestor(@Nonnull VirtualFile file1, @Nonnull VirtualFile file2) {
    if (!file1.getFileSystem().equals(file2.getFileSystem())) {
      return null;
    }

    VirtualFile[] path1 = getPathComponents(file1);
    VirtualFile[] path2 = getPathComponents(file2);

    int lastEqualIdx = -1;
    for (int i = 0; i < path1.length && i < path2.length; i++) {
      if (path1[i].equals(path2[i])) {
        lastEqualIdx = i;
      }
      else {
        break;
      }
    }
    return lastEqualIdx == -1 ? null : path1[lastEqualIdx];
  }

  /**
   * Gets an array of files representing paths from root to the passed file.
   *
   * @param file the file
   * @return virtual files which represents paths from root to the passed file
   */
  @Nonnull
  static VirtualFile[] getPathComponents(@Nonnull VirtualFile file) {
    ArrayList<VirtualFile> componentsList = new ArrayList<VirtualFile>();
    while (file != null) {
      componentsList.add(file);
      file = file.getParent();
    }
    int size = componentsList.size();
    VirtualFile[] components = new VirtualFile[size];
    for (int i = 0; i < size; i++) {
      components[i] = componentsList.get(size - i - 1);
    }
    return components;
  }

  public static boolean hasInvalidFiles(@Nonnull Iterable<VirtualFile> files) {
    for (VirtualFile file : files) {
      if (!file.isValid()) {
        return true;
      }
    }
    return false;
  }

  @Nullable
  public static VirtualFile findContainingDirectory(@Nonnull VirtualFile file, @Nonnull CharSequence name) {
    VirtualFile parent = file.isDirectory() ? file : file.getParent();
    while (parent != null) {
      if (Comparing.equal(parent.getNameSequence(), name, SystemInfo.isFileSystemCaseSensitive)) {
        return parent;
      }
      parent = parent.getParent();
    }
    return null;
  }

  /**
   * this collection will keep only distinct files/folders, e.g. C:\foo\bar will be removed when C:\foo is added
   */
  public static class DistinctVFilesRootsCollection extends DistinctRootsCollection<VirtualFile> {
    public DistinctVFilesRootsCollection() {
    }

    public DistinctVFilesRootsCollection(Collection<VirtualFile> virtualFiles) {
      super(virtualFiles);
    }

    public DistinctVFilesRootsCollection(VirtualFile[] collection) {
      super(collection);
    }

    @Override
    protected boolean isAncestor(@Nonnull VirtualFile ancestor, @Nonnull VirtualFile virtualFile) {
      return VfsUtilCore.isAncestor(ancestor, virtualFile, false);
    }
  }

  public static void processFilesRecursively(@Nonnull VirtualFile root, @Nonnull Processor<VirtualFile> processor, @Nonnull Convertor<VirtualFile, Boolean> directoryFilter) {
    if (!processor.process(root)) return;

    if (root.isDirectory() && directoryFilter.convert(root)) {
      final LinkedList<VirtualFile[]> queue = new LinkedList<VirtualFile[]>();

      queue.add(root.getChildren());

      do {
        final VirtualFile[] files = queue.removeFirst();

        for (VirtualFile file : files) {
          if (!processor.process(file)) return;
          if (file.isDirectory() && directoryFilter.convert(file)) {
            queue.add(file.getChildren());
          }
        }
      }
      while (!queue.isEmpty());
    }
  }
}
