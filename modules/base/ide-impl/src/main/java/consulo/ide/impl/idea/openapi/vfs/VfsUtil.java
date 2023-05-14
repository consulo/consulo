/*
 * Copyright 2000-2015 JetBrains s.r.o.
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
package consulo.ide.impl.idea.openapi.vfs;

import consulo.application.util.SystemInfo;
import consulo.application.util.function.Processor;
import consulo.ide.impl.idea.openapi.util.Comparing;
import consulo.ide.impl.idea.openapi.util.SystemInfoRt;
import consulo.ide.impl.idea.openapi.util.io.FileUtil;
import consulo.ide.impl.idea.util.ArrayUtil;
import consulo.ide.impl.idea.util.PathUtil;
import consulo.ide.impl.idea.util.containers.ContainerUtil;
import consulo.ide.impl.idea.util.containers.Convertor;
import consulo.language.file.FileTypeManager;
import consulo.logging.Logger;
import consulo.util.io.Url;
import consulo.util.io.Urls;
import consulo.util.lang.SystemProperties;
import consulo.virtualFileSystem.*;
import consulo.virtualFileSystem.event.VirtualFileEvent;
import consulo.virtualFileSystem.util.VirtualFileUtil;
import org.jetbrains.annotations.NonNls;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.*;
import java.util.function.Function;

public class VfsUtil extends VfsUtilCore {
  private static final Logger LOG = Logger.getInstance(VfsUtil.class);

  public static void saveText(@Nonnull VirtualFile file, @Nonnull String text) throws IOException {
    VirtualFileUtil.saveText(file, text);
  }

  /**
   * Copies all files matching the <code>filter</code> from <code>fromDir</code> to <code>toDir</code>.
   * Symlinks end special files are ignored.
   *
   * @param requestor any object to control who called this method. Note that
   *                  it is considered to be an external change if <code>requestor</code> is <code>null</code>.
   *                  See {@link VirtualFileEvent#getRequestor}
   * @param fromDir   the directory to copy from
   * @param toDir     the directory to copy to
   * @param filter    {@link VirtualFileFilter}
   * @throws IOException if files failed to be copied
   */
  public static void copyDirectory(Object requestor, @Nonnull VirtualFile fromDir, @Nonnull VirtualFile toDir, @Nullable VirtualFileFilter filter) throws IOException {
    @SuppressWarnings("UnsafeVfsRecursion") VirtualFile[] children = fromDir.getChildren();
    for (VirtualFile child : children) {
      if (!child.is(VFileProperty.SYMLINK) && !child.is(VFileProperty.SPECIAL) && (filter == null || filter.accept(child))) {
        if (!child.isDirectory()) {
          copyFile(requestor, child, toDir);
        }
        else {
          VirtualFile newChild = toDir.findChild(child.getName());
          if (newChild == null) {
            newChild = toDir.createChildDirectory(requestor, child.getName());
          }
          copyDirectory(requestor, child, newChild, filter);
        }
      }
    }
  }

  /**
   * Copies content of resource to the given file
   *
   * @param file        to copy to
   * @param resourceUrl url of the resource to be copied
   * @throws IOException if resource not found or copying failed
   */
  public static void copyFromResource(@Nonnull VirtualFile file, @NonNls @Nonnull String resourceUrl) throws IOException {
    InputStream out = VfsUtil.class.getResourceAsStream(resourceUrl);
    if (out == null) {
      throw new FileNotFoundException(resourceUrl);
    }
    try {
      byte[] bytes = FileUtil.adaptiveLoadBytes(out);
      file.setBinaryContent(bytes);
    }
    finally {
      out.close();
    }
  }

  /**
   * Makes a copy of the <code>file</code> in the <code>toDir</code> folder and returns it.
   * Handles both files and directories.
   *
   * @param requestor any object to control who called this method. Note that
   *                  it is considered to be an external change if <code>requestor</code> is <code>null</code>.
   *                  See {@link VirtualFileEvent#getRequestor}
   * @param file      file or directory to make a copy of
   * @param toDir     directory to make a copy in
   * @return a copy of the file
   * @throws IOException if file failed to be copied
   */
  public static VirtualFile copy(Object requestor, @Nonnull VirtualFile file, @Nonnull VirtualFile toDir) throws IOException {
    if (file.isDirectory()) {
      VirtualFile newDir = toDir.createChildDirectory(requestor, file.getName());
      copyDirectory(requestor, file, newDir, null);
      return newDir;
    }
    else {
      return copyFile(requestor, file, toDir);
    }
  }

  /**
   * Gets the array of common ancestors for passed files.
   *
   * @param files array of files
   * @return array of common ancestors for passed files
   */
  @Nonnull
  public static VirtualFile[] getCommonAncestors(@Nonnull VirtualFile[] files) {
    // Separate files by first component in the path.
    HashMap<VirtualFile, Set<VirtualFile>> map = new HashMap<VirtualFile, Set<VirtualFile>>();
    for (VirtualFile aFile : files) {
      VirtualFile directory = aFile.isDirectory() ? aFile : aFile.getParent();
      if (directory == null) return VirtualFile.EMPTY_ARRAY;
      VirtualFile[] path = getPathComponents(directory);
      Set<VirtualFile> filesSet;
      final VirtualFile firstPart = path[0];
      if (map.containsKey(firstPart)) {
        filesSet = map.get(firstPart);
      }
      else {
        filesSet = new HashSet<VirtualFile>();
        map.put(firstPart, filesSet);
      }
      filesSet.add(directory);
    }
    // Find common ancestor for each set of files.
    ArrayList<VirtualFile> ancestorsList = new ArrayList<VirtualFile>();
    for (Set<VirtualFile> filesSet : map.values()) {
      VirtualFile ancestor = null;
      for (VirtualFile file : filesSet) {
        if (ancestor == null) {
          ancestor = file;
          continue;
        }
        ancestor = getCommonAncestor(ancestor, file);
        //assertTrue(ancestor != null);
      }
      ancestorsList.add(ancestor);
      filesSet.clear();
    }
    return toVirtualFileArray(ancestorsList);
  }

  /**
   * Gets the common ancestor for passed files, or {@code null} if the files do not have common ancestors.
   */
  @Nullable
  public static VirtualFile getCommonAncestor(@Nonnull Collection<? extends VirtualFile> files) {
    VirtualFile ancestor = null;
    for (VirtualFile file : files) {
      if (ancestor == null) {
        ancestor = file;
      }
      else {
        ancestor = getCommonAncestor(ancestor, file);
        if (ancestor == null) return null;
      }
    }
    return ancestor;
  }

  @Nullable
  public static VirtualFile findRelativeFile(@Nullable VirtualFile base, String... path) {
    return VirtualFileUtil.findRelativeFile(base, path);
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
    return VirtualFileUtil.findFileByURL(url);
  }

  @Nullable
  public static VirtualFile findFileByURL(@Nonnull URL url, @Nonnull VirtualFileManager virtualFileManager) {
    return VirtualFileUtil.findFileByURL(url, virtualFileManager);
  }

  @Nullable
  public static VirtualFile findFileByIoFile(@Nonnull File file, boolean refreshIfNeeded) {
    return VirtualFileUtil.findFileByIoFile(file, refreshIfNeeded);
  }

  public static VirtualFile copyFileRelative(Object requestor, @Nonnull VirtualFile file, @Nonnull VirtualFile toDir, @Nonnull String relativePath) throws IOException {
    StringTokenizer tokenizer = new StringTokenizer(relativePath, "/");
    VirtualFile curDir = toDir;

    while (true) {
      String token = tokenizer.nextToken();
      if (tokenizer.hasMoreTokens()) {
        VirtualFile childDir = curDir.findChild(token);
        if (childDir == null) {
          childDir = curDir.createChildDirectory(requestor, token);
        }
        curDir = childDir;
      }
      else {
        return copyFile(requestor, file, curDir, token);
      }
    }
  }

  @Nonnull
  public static String toIdeaUrl(@Nonnull String url) {
    return toIdeaUrl(url, true);
  }

  /**
   * @return correct URL, must be used only for external communication
   */
  @Nonnull
  public static URI toUri(@Nonnull VirtualFile file) {
    return VirtualFileUtil.toUri(file);
  }

  /**
   * @return correct URL, must be used only for external communication
   */
  @Nonnull
  public static URI toUri(@Nonnull File file) {
    String path = file.toURI().getPath();
    try {
      if (SystemInfo.isWindows && path.charAt(0) != '/') {
        path = '/' + path;
      }
      return new URI(StandardFileSystems.FILE_PROTOCOL, "", path, null, null);
    }
    catch (URISyntaxException e) {
      throw new IllegalArgumentException(e);
    }
  }

  /**
   * uri - may be incorrect (escaping or missed "/" before disk name under windows), may be not fully encoded,
   * may contains query and fragment
   *
   * @return correct URI, must be used only for external communication
   */
  @Nullable
  public static URI toUri(@NonNls @Nonnull String uri) {
    int index = uri.indexOf("://");
    if (index < 0) {
      // true URI, like mailto:
      try {
        return new URI(uri);
      }
      catch (URISyntaxException e) {
        LOG.debug(e);
        return null;
      }
    }

    if (SystemInfo.isWindows && uri.startsWith(LocalFileSystem.PROTOCOL_PREFIX)) {
      int firstSlashIndex = index + "://".length();
      if (uri.charAt(firstSlashIndex) != '/') {
        uri = LocalFileSystem.PROTOCOL_PREFIX + '/' + uri.substring(firstSlashIndex);
      }
    }

    try {
      return new URI(uri);
    }
    catch (URISyntaxException e) {
      LOG.debug("uri is not fully encoded", e);
      // so, uri is not fully encoded (space)
      try {
        int fragmentIndex = uri.lastIndexOf('#');
        String path = uri.substring(index + 1, fragmentIndex > 0 ? fragmentIndex : uri.length());
        String fragment = fragmentIndex > 0 ? uri.substring(fragmentIndex + 1) : null;
        return new URI(uri.substring(0, index), path, fragment);
      }
      catch (URISyntaxException e1) {
        LOG.debug(e1);
        return null;
      }
    }
  }

  /**
   * Returns the relative path from one virtual file to another.
   *
   * @param src           the file from which the relative path is built.
   * @param dst           the file to which the path is built.
   * @param separatorChar the separator for the path components.
   * @return the relative path, or null if the files have no common ancestor.
   * @since 5.0.2
   */
  @Nullable
  public static String getPath(@Nonnull VirtualFile src, @Nonnull VirtualFile dst, char separatorChar) {
    final VirtualFile commonAncestor = getCommonAncestor(src, dst);
    if (commonAncestor != null) {
      StringBuilder buffer = new StringBuilder();
      if (!Comparing.equal(src, commonAncestor)) {
        while (!Comparing.equal(src.getParent(), commonAncestor)) {
          buffer.append("..").append(separatorChar);
          src = src.getParent();
        }
      }
      buffer.append(getRelativePath(dst, commonAncestor, separatorChar));
      return buffer.toString();
    }

    return null;
  }

  public static String getUrlForLibraryRoot(@Nonnull File libraryRoot) {
    return VirtualFileUtil.getUrlForLibraryRoot(libraryRoot);
  }

  public static VirtualFile createChildSequent(Object requestor, @Nonnull VirtualFile dir, @Nonnull String prefix, @Nonnull String extension) throws IOException {
    String dotExt = PathUtil.makeFileName("", extension);
    String fileName = prefix + dotExt;
    int i = 1;
    while (dir.findChild(fileName) != null) {
      fileName = prefix + "_" + i + dotExt;
      i++;
    }
    return dir.createChildData(requestor, fileName);
  }

  @Nonnull
  public static String[] filterNames(@Nonnull String[] names) {
    int filteredCount = 0;
    for (String string : names) {
      if (isBadName(string)) filteredCount++;
    }
    if (filteredCount == 0) return names;

    String[] result = ArrayUtil.newStringArray(names.length - filteredCount);
    int count = 0;
    for (String string : names) {
      if (isBadName(string)) continue;
      result[count++] = string;
    }

    return result;
  }

  public static boolean isBadName(String name) {
    return name == null || name.isEmpty() || "/".equals(name) || "\\".equals(name);
  }

  public static VirtualFile createDirectories(@Nonnull final String directoryPath) throws IOException {
    return VirtualFileUtil.createDirectories(directoryPath);
  }

  public static VirtualFile createDirectoryIfMissing(VirtualFile parent, String relativePath) throws IOException {
    return VirtualFileUtil.createDirectoryIfMissing(parent, relativePath);
  }

  @Nullable
  public static VirtualFile createDirectoryIfMissing(@Nonnull String directoryPath) throws IOException {
    return VirtualFileUtil.createDirectoryIfMissing(directoryPath);
  }

  /**
   * Returns all files in some virtual files recursively
   *
   * @param root virtual file to get descendants
   * @return descendants
   */
  @Nonnull
  public static List<VirtualFile> collectChildrenRecursively(@Nonnull final VirtualFile root) {
    final List<VirtualFile> result = new ArrayList<VirtualFile>();
    processFilesRecursively(root, t -> {
      result.add(t);
      return true;
    });
    return result;
  }


  public static void processFileRecursivelyWithoutIgnored(@Nonnull final VirtualFile root, @Nonnull final Processor<VirtualFile> processor) {
    final FileTypeManager ftm = FileTypeManager.getInstance();
    processFilesRecursively(root, processor, new Convertor<VirtualFile, Boolean>() {
      public Boolean convert(final VirtualFile vf) {
        return !ftm.isFileIgnored(vf);
      }
    });
  }

  @Nullable
  public static <T> T processInputStream(@Nonnull final VirtualFile file, @Nonnull Function<InputStream, T> function) {
    InputStream stream = null;
    try {
      stream = file.getInputStream();
      return function.apply(stream);
    }
    catch (IOException e) {
      LOG.error(e);
    }
    finally {
      try {
        if (stream != null) {
          stream.close();
        }
      }
      catch (IOException e) {
        LOG.error(e);
      }
    }
    return null;
  }

  @Nonnull
  public static String getReadableUrl(@Nonnull final VirtualFile file) {
    String url = null;
    if (file.isInLocalFileSystem()) {
      url = file.getPresentableUrl();
    }
    if (url == null) {
      url = file.getUrl();
    }
    return url;
  }

  @Nullable
  public static VirtualFile getUserHomeDir() {
    final String path = SystemProperties.getUserHome();
    return LocalFileSystem.getInstance().findFileByPath(FileUtil.toSystemIndependentName(path));
  }

  @Nonnull
  public static VirtualFile[] getChildren(@Nonnull VirtualFile dir) {
    VirtualFile[] children = dir.getChildren();
    return children == null ? VirtualFile.EMPTY_ARRAY : children;
  }

  @Nonnull
  public static List<VirtualFile> getChildren(@Nonnull VirtualFile dir, @Nonnull VirtualFileFilter filter) {
    List<VirtualFile> result = null;
    for (VirtualFile child : dir.getChildren()) {
      if (filter.accept(child)) {
        if (result == null) result = ContainerUtil.newSmartList();
        result.add(child);
      }
    }
    return result != null ? result : ContainerUtil.<VirtualFile>emptyList();
  }

  /**
   * @param url Url for virtual file
   * @return url for parent directory of virtual file
   */
  @Nullable
  public static String getParentDir(@Nullable final String url) {
    if (url == null) {
      return null;
    }
    final int index = url.lastIndexOf(VfsUtilCore.VFS_SEPARATOR_CHAR);
    return index < 0 ? null : url.substring(0, index);
  }

  /**
   * @param urlOrPath Url for virtual file
   * @return file name
   */
  @Nullable
  public static String extractFileName(@Nullable final String urlOrPath) {
    return VirtualFileUtil.extractFileName(urlOrPath);
  }

  @Nonnull
  public static List<VirtualFile> markDirty(boolean recursive, boolean reloadChildren, @Nonnull VirtualFile... files) {
    return VirtualFileUtil.markDirty(recursive, reloadChildren, files);
  }

  public static void markDirtyAndRefresh(boolean async, boolean recursive, boolean reloadChildren, @Nonnull VirtualFile... files) {
    VirtualFileUtil.markDirtyAndRefresh(async, recursive, reloadChildren, files);
  }

  @Nonnull
  public static VirtualFile getRootFile(@Nonnull VirtualFile file) {
    while (true) {
      VirtualFile parent = file.getParent();
      if (parent == null) break;
      file = parent;
    }
    return file;
  }

  @Nonnull
  public static Url newLocalFileUrl(@Nonnull VirtualFile file) {
    return Urls.newLocalFileUrl(file.getPath());
  }

  @Nonnull
  public static Url newFromVirtualFile(@Nonnull VirtualFile file) {
    if (file.isInLocalFileSystem()) {
      return Urls.newUri(file.getFileSystem().getProtocol(), file.getPath());
    }
    else {
      Url url = Urls.parseUrlUnsafe(file.getUrl());
      return url == null ? Urls.newPathUrl(file.getPath()) : url;
    }
  }

  public static boolean equalsIgnoreParameters(@Nonnull Url url, @Nonnull VirtualFile file) {
    if (file.isInLocalFileSystem()) {
      return url.isInLocalFileSystem() && (SystemInfoRt.isFileSystemCaseSensitive ? url.getPath().equals(file.getPath()) : url.getPath().equalsIgnoreCase(file.getPath()));
    }
    else if (url.isInLocalFileSystem()) {
      return false;
    }

    Url fileUrl = Urls.parseUrlUnsafe(file.getUrl());
    return fileUrl != null && fileUrl.equalsIgnoreParameters(url);
  }
}
