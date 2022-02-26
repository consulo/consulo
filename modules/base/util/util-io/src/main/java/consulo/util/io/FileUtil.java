/*
 * Copyright 2000-2016 JetBrains s.r.o.
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
package consulo.util.io;

import consulo.util.collection.HashingStrategy;
import consulo.util.lang.StringUtil;
import consulo.util.lang.ThreeState;
import org.jetbrains.annotations.Contract;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.*;
import java.nio.channels.FileChannel;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.List;

public class FileUtil {
  private static final Logger LOG = LoggerFactory.getLogger(FileUtil.class);

  private static final int MAX_FILE_IO_ATTEMPTS = 10;
  private static final boolean USE_FILE_CHANNELS = "true".equalsIgnoreCase(System.getProperty("consulo.fs.useChannels"));
  public static final HashingStrategy<String> PATH_HASHING_STRATEGY = OSInfo.isFileSystemCaseSensitive ? HashingStrategy.caseInsensitive() : HashingStrategy.canonical();

  public static final int THREAD_LOCAL_BUFFER_LENGTH = 1024 * 20;
  protected static final ThreadLocal<byte[]> BUFFER = new ThreadLocal<byte[]>() {
    @Override
    protected byte[] initialValue() {
      return new byte[THREAD_LOCAL_BUFFER_LENGTH];
    }
  };

  public static void copy(@Nonnull File fromFile, @Nonnull File toFile, @Nonnull FilePermissionCopier permissionCopier) throws IOException {
    performCopy(fromFile, toFile, true, permissionCopier);
  }

  public static void copyContent(@Nonnull File fromFile, @Nonnull File toFile, @Nonnull FilePermissionCopier permissionCopier) throws IOException {
    performCopy(fromFile, toFile, false, permissionCopier);
  }

  private static FileOutputStream openOutputStream(@Nonnull final File file) throws IOException {
    try {
      return new FileOutputStream(file);
    }
    catch (FileNotFoundException e) {
      final File parentFile = file.getParentFile();
      if (parentFile == null) {
        throw new IOException("Parent file is null for " + file.getPath(), e);
      }
      createParentDirs(file);
      return new FileOutputStream(file);
    }
  }

  @SuppressWarnings("Duplicates")
  private static void performCopy(@Nonnull File fromFile, @Nonnull File toFile, final boolean syncTimestamp, @Nonnull FilePermissionCopier permissionCopier) throws IOException {
    if (filesEqual(fromFile, toFile)) return;
    final FileOutputStream fos = openOutputStream(toFile);

    try {
      final FileInputStream fis = new FileInputStream(fromFile);
      try {
        copy(fis, fos);
      }
      finally {
        fis.close();
      }
    }
    finally {
      fos.close();
    }

    if (syncTimestamp) {
      final long timeStamp = fromFile.lastModified();
      if (timeStamp < 0) {
        LOG.warn("Invalid timestamp " + timeStamp + " of '" + fromFile + "'");
      }
      else if (!toFile.setLastModified(timeStamp)) {
        LOG.warn("Unable to set timestamp " + timeStamp + " to '" + toFile + "'");
      }
    }

    if (fromFile.canExecute()) {
      clonePermissionsToExecute(fromFile.getPath(), toFile.getPath(), permissionCopier);
    }
  }

  private static boolean clonePermissionsToExecute(@Nonnull String source, @Nonnull String target, @Nonnull FilePermissionCopier permissionCopier) {
    try {
      return permissionCopier.clonePermissions(source, target, true);
    }
    catch (Exception e) {
      LOG.warn("Source " + source + "/Target " + target, e);
      return false;
    }
  }

  public static void copyDir(@Nonnull File fromDir, @Nonnull File toDir, @Nonnull FilePermissionCopier permissionCopier) throws IOException {
    copyDir(fromDir, toDir, true, permissionCopier);
  }

  public static void copyDir(@Nonnull File fromDir, @Nonnull File toDir, boolean copySystemFiles, @Nonnull FilePermissionCopier permissionCopier) throws IOException {
    copyDir(fromDir, toDir, copySystemFiles ? null : (FileFilter)file -> !StringUtil.startsWithChar(file.getName(), '.'), permissionCopier);
  }

  public static void copyDir(@Nonnull File fromDir, @Nonnull File toDir, @Nullable final FileFilter filter, @Nonnull FilePermissionCopier permissionCopier) throws IOException {
    ensureExists(toDir);
    if (isAncestor(fromDir, toDir, true)) {
      LOG.error(fromDir.getAbsolutePath() + " is ancestor of " + toDir + ". Can't copy to itself.");
      return;
    }
    File[] files = fromDir.listFiles();
    if (files == null) throw new IOException("Directory is invalid " + fromDir.getPath());
    if (!fromDir.canRead()) throw new IOException("Directory is not readable " + fromDir.getPath());
    for (File file : files) {
      if (filter != null && !filter.accept(file)) {
        continue;
      }
      if (file.isDirectory()) {
        copyDir(file, new File(toDir, file.getName()), filter, permissionCopier);
      }
      else {
        copy(file, new File(toDir, file.getName()), permissionCopier);
      }
    }
  }

  public static void ensureExists(@Nonnull File dir) throws IOException {
    if (!dir.exists() && !dir.mkdirs()) {
      throw new IOException("Cannot create directory: " + dir.getPath());
    }
  }

  public static void copy(@Nonnull InputStream inputStream, @Nonnull OutputStream outputStream) throws IOException {
    if (USE_FILE_CHANNELS && inputStream instanceof FileInputStream && outputStream instanceof FileOutputStream) {
      try (FileChannel fromChannel = ((FileInputStream)inputStream).getChannel()) {
        try (FileChannel toChannel = ((FileOutputStream)outputStream).getChannel()) {
          fromChannel.transferTo(0, Long.MAX_VALUE, toChannel);
        }
      }
    }
    else {
      final byte[] buffer = getThreadLocalBuffer();
      while (true) {
        int read = inputStream.read(buffer);
        if (read < 0) break;
        outputStream.write(buffer, 0, read);
      }
    }
  }

  public static boolean createParentDirs(@Nonnull File file) {
    File parentPath = file.getParentFile();
    return parentPath == null || createDirectory(parentPath);
  }

  public static boolean createDirectory(@Nonnull File path) {
    return path.isDirectory() || path.mkdirs();
  }

  public static boolean filesEqual(@Nullable File file1, @Nullable File file2) {
    // on MacOS java.io.File.equals() is incorrectly case-sensitive
    return pathsEqual(file1 == null ? null : file1.getPath(), file2 == null ? null : file2.getPath());
  }

  public static boolean pathsEqual(@Nullable String path1, @Nullable String path2) {
    if (path1 == path2) return true;
    if (path1 == null || path2 == null) return false;

    path1 = toCanonicalPath(path1);
    path2 = toCanonicalPath(path2);
    return PATH_HASHING_STRATEGY.equals(path1, path2);
  }

  @Nonnull
  public static byte[] loadFirstAndClose(@Nonnull InputStream stream, int maxLength) throws IOException {
    ByteArrayOutputStream buffer = new ByteArrayOutputStream();
    try {
      copy(stream, maxLength, buffer);
    }
    finally {
      stream.close();
    }
    return buffer.toByteArray();
  }

  public static void copy(@Nonnull InputStream inputStream, int maxSize, @Nonnull OutputStream outputStream) throws IOException {
    final byte[] buffer = getThreadLocalBuffer();
    int toRead = maxSize;
    while (toRead > 0) {
      int read = inputStream.read(buffer, 0, Math.min(buffer.length, toRead));
      if (read < 0) break;
      toRead -= read;
      outputStream.write(buffer, 0, read);
    }
  }

  @Nonnull
  public static byte[] getThreadLocalBuffer() {
    return BUFFER.get();
  }

  /**
   * Check if the {@code ancestor} is an ancestor of {@code file}.
   *
   * @param ancestor supposed ancestor.
   * @param file     supposed descendant.
   * @param strict   if {@code false} then this method returns {@code true} if {@code ancestor} equals to {@code file}.
   * @return {@code true} if {@code ancestor} is parent of {@code file}; {@code false} otherwise.
   */
  public static boolean isAncestor(@Nonnull File ancestor, @Nonnull File file, boolean strict) {
    return isAncestor(ancestor.getPath(), file.getPath(), strict);
  }

  public static boolean isAncestor(@Nonnull String ancestor, @Nonnull String file, boolean strict) {
    return !ThreeState.NO.equals(isAncestorThreeState(ancestor, file, strict));
  }

  /**
   * Checks if the {@code ancestor} is an ancestor of the {@code file}, and if it is an immediate parent or not.
   *
   * @param ancestor supposed ancestor.
   * @param file     supposed descendant.
   * @param strict   if {@code false}, the file can be ancestor of itself,
   *                 i.e. the method returns {@code ThreeState.YES} if {@code ancestor} equals to {@code file}.
   * @return {@code ThreeState.YES} if ancestor is an immediate parent of the file,
   * {@code ThreeState.UNSURE} if ancestor is not immediate parent of the file,
   * {@code ThreeState.NO} if ancestor is not a parent of the file at all.
   */
  @Nonnull
  public static ThreeState isAncestorThreeState(@Nonnull String ancestor, @Nonnull String file, boolean strict) {
    String ancestorPath = toCanonicalPath(ancestor);
    String filePath = toCanonicalPath(file);
    return startsWith(filePath, ancestorPath, strict, OSInfo.isFileSystemCaseSensitive, true);
  }

  public static boolean startsWith(@Nonnull String path, @Nonnull String start) {
    return !ThreeState.NO.equals(startsWith(path, start, false, OSInfo.isFileSystemCaseSensitive, false));
  }

  public static boolean startsWith(@Nonnull String path, @Nonnull String start, boolean caseSensitive) {
    return !ThreeState.NO.equals(startsWith(path, start, false, caseSensitive, false));
  }

  @Nonnull
  private static ThreeState startsWith(@Nonnull String path, @Nonnull String prefix, boolean strict, boolean caseSensitive, boolean checkImmediateParent) {
    final int pathLength = path.length();
    final int prefixLength = prefix.length();
    if (prefixLength == 0) return pathLength == 0 ? ThreeState.YES : ThreeState.UNSURE;
    if (prefixLength > pathLength) return ThreeState.NO;
    if (!path.regionMatches(!caseSensitive, 0, prefix, 0, prefixLength)) return ThreeState.NO;
    if (pathLength == prefixLength) {
      return strict ? ThreeState.NO : ThreeState.YES;
    }
    char lastPrefixChar = prefix.charAt(prefixLength - 1);
    int slashOrSeparatorIdx = prefixLength;
    if (lastPrefixChar == '/' || lastPrefixChar == File.separatorChar) {
      slashOrSeparatorIdx = prefixLength - 1;
    }
    char next1 = path.charAt(slashOrSeparatorIdx);
    if (next1 == '/' || next1 == File.separatorChar) {
      if (!checkImmediateParent) return ThreeState.YES;

      if (slashOrSeparatorIdx == pathLength - 1) return ThreeState.YES;
      int idxNext = path.indexOf(next1, slashOrSeparatorIdx + 1);
      idxNext = idxNext == -1 ? path.indexOf(next1 == '/' ? '\\' : '/', slashOrSeparatorIdx + 1) : idxNext;
      return idxNext == -1 ? ThreeState.YES : ThreeState.UNSURE;
    }
    else {
      return ThreeState.NO;
    }
  }

  @Nonnull
  public static String loadTextAndClose(@Nonnull InputStream stream) throws IOException {
    //noinspection IOResourceOpenedButNotSafelyClosed
    return loadTextAndClose(new InputStreamReader(stream));
  }

  @Nonnull
  public static String loadTextAndClose(@Nonnull InputStream inputStream, boolean convertLineSeparators) throws IOException {
    String text = loadTextAndClose(inputStream);
    return convertLineSeparators ? StringUtil.convertLineSeparators(text) : text;
  }

  @Nonnull
  public static String loadTextAndClose(@Nonnull Reader reader) throws IOException {
    try {
      return new String(adaptiveLoadText(reader));
    }
    finally {
      reader.close();
    }
  }

  @Nonnull
  public static char[] adaptiveLoadText(@Nonnull Reader reader) throws IOException {
    char[] chars = new char[4096];
    List<char[]> buffers = null;
    int count = 0;
    int total = 0;
    while (true) {
      int n = reader.read(chars, count, chars.length - count);
      if (n <= 0) break;
      count += n;
      if (total > 1024 * 1024 * 10) throw new FileTooBigException("File too big " + reader);
      total += n;
      if (count == chars.length) {
        if (buffers == null) {
          buffers = new ArrayList<>();
        }
        buffers.add(chars);
        int newLength = Math.min(1024 * 1024, chars.length * 2);
        chars = new char[newLength];
        count = 0;
      }
    }
    char[] result = new char[total];
    if (buffers != null) {
      for (char[] buffer : buffers) {
        System.arraycopy(buffer, 0, result, result.length - total, buffer.length);
        total -= buffer.length;
      }
    }
    System.arraycopy(chars, 0, result, result.length - total, total);
    return result;
  }

  @Nonnull
  public static String getExtension(@Nonnull String fileName) {
    int index = fileName.lastIndexOf('.');
    if (index < 0) return "";
    return fileName.substring(index + 1);
  }

  @Nonnull
  public static CharSequence getExtension(@Nonnull CharSequence fileName) {
    return getExtension(fileName, "");
  }

  @Contract("_,!null -> !null")
  public static CharSequence getExtension(@Nonnull CharSequence fileName, @Nullable String defaultValue) {
    int index = StringUtil.lastIndexOf(fileName, '.', 0, fileName.length());
    if (index < 0) {
      return defaultValue;
    }
    return fileName.subSequence(index + 1, fileName.length());
  }

  @Nonnull
  public static List<String> loadLines(@Nonnull File file) throws IOException {
    return loadLines(file.getPath());
  }

  @Nonnull
  public static List<String> loadLines(@Nonnull File file, @Nullable String encoding) throws IOException {
    return loadLines(file.getPath(), encoding);
  }

  @Nonnull
  public static List<String> loadLines(@Nonnull String path) throws IOException {
    return loadLines(path, null);
  }

  @Nonnull
  public static List<String> loadLines(@Nonnull String path, @Nullable String encoding) throws IOException {
    try (InputStream stream = new FileInputStream(path)) {
      try (BufferedReader reader = new BufferedReader(encoding == null ? new InputStreamReader(stream) : new InputStreamReader(stream, encoding))) {
        return loadLines(reader);
      }
    }
  }

  @Nonnull
  public static List<String> loadLines(@Nonnull BufferedReader reader) throws IOException {
    List<String> lines = new ArrayList<>();
    String line;
    while ((line = reader.readLine()) != null) {
      lines.add(line);
    }
    return lines;
  }

  /**
   * @param file file or directory to delete
   * @return true if the file did not exist or was successfully deleted
   */
  public static boolean delete(@Nonnull File file) {
    try {
      deleteRecursivelyNIO2(file.toPath());
      return true;
    }
    catch (Exception e) {
      LOG.info("Fail to delete file: " + file, e);
      return false;
    }
  }

  /**
   * @param file file or directory to delete
   * @return true if the file did not exist or was successfully deleted
   */
  public static boolean delete(@Nonnull Path path) {
    try {
      deleteRecursivelyNIO2(path);
      return true;
    }
    catch (Exception e) {
      LOG.info("Fail to delete path: " + path, e);
      return false;
    }
  }

  static void deleteRecursivelyNIO2(Path path) throws IOException {
    if (Files.isRegularFile(path)) {
      performDeleteNIO2(path);
      return;
    }

    Files.walkFileTree(path, new FileVisitor<Path>() {
      @Override
      public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
        if (OSInfo.isWindows) {
          boolean notDirectory = attrs.isOther();

          if (notDirectory) {
            performDeleteNIO2(dir);
            return FileVisitResult.SKIP_SUBTREE;
          }
        }
        return FileVisitResult.CONTINUE;
      }

      @Override
      public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
        performDeleteNIO2(file);

        return FileVisitResult.CONTINUE;
      }

      @Override
      public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
        performDeleteNIO2(dir);

        return FileVisitResult.CONTINUE;
      }

      @Override
      public FileVisitResult visitFileFailed(Path file, IOException exc) throws IOException {
        return FileVisitResult.CONTINUE;
      }
    });
  }

  static void performDeleteNIO2(Path path) throws IOException {
    Boolean result = doIOOperation(lastAttempt -> {
      try {
        Files.deleteIfExists(path);
        return Boolean.TRUE;
      }
      catch (IOException e) {
        // file is read-only: fallback to standard java.io API
        if (e instanceof AccessDeniedException) {
          File file = path.toFile();
          if (file == null) {
            return Boolean.FALSE;
          }
          if (file.delete() || !file.exists()) {
            return Boolean.TRUE;
          }
        }
      }
      return lastAttempt ? Boolean.FALSE : null;
    });

    if (!Boolean.TRUE.equals(result)) {
      throw new IOException("Failed to delete " + path) {
        @Override
        public synchronized Throwable fillInStackTrace() {
          return this; // optimization: the stacktrace is not needed: the exception is used to terminate tree walking and to pass the result
        }
      };
    }
  }

  public interface RepeatableIOOperation<T, E extends Throwable> {
    @Nullable
    T execute(boolean lastAttempt) throws E;
  }

  @Nullable
  public static <T, E extends Throwable> T doIOOperation(@Nonnull RepeatableIOOperation<T, E> ioTask) throws E {
    for (int i = MAX_FILE_IO_ATTEMPTS; i > 0; i--) {
      T result = ioTask.execute(i == 1);
      if (result != null) return result;

      try {
        //noinspection BusyWait
        Thread.sleep(10);
      }
      catch (InterruptedException ignored) {
      }
    }
    return null;
  }

  protected interface SymlinkResolver {
    @Nonnull
    String resolveSymlinksAndCanonicalize(@Nonnull String path, char separatorChar, boolean removeLastSlash);

    boolean isSymlink(@Nonnull CharSequence path);
  }

  public static String toSystemDependentName(String fileName) {
    return toSystemDependentName(fileName, File.separatorChar);
  }


  public static String toSystemDependentName(String fileName, final char separatorChar) {
    return fileName.replace('/', separatorChar).replace('\\', separatorChar);
  }

  public static String toSystemIndependentName(String fileName) {
    return fileName.replace('\\', '/');
  }

  @Nonnull
  public static String getNameWithoutExtension(@Nonnull File file) {
    return getNameWithoutExtension(file.getName());
  }

  @Nonnull
  public static CharSequence getNameWithoutExtension(@Nonnull CharSequence name) {
    int i = StringUtil.lastIndexOf(name, '.', 0, name.length());
    return i < 0 ? name : name.subSequence(0, i);
  }

  @Nonnull
  public static String getNameWithoutExtension(@Nonnull String name) {
    return getNameWithoutExtension((CharSequence)name).toString();
  }


  @Nonnull
  public static String sanitizeFileName(@Nonnull String name) {
    return sanitizeFileName(name, true);
  }

  /**
   * @deprecated use {@link #sanitizeFileName(String, boolean)} (to be removed in IDEA 17)
   */
  @SuppressWarnings("unused")
  public static String sanitizeName(@Nonnull String name) {
    return sanitizeFileName(name, false);
  }

  @Nonnull
  public static String sanitizeFileName(@Nonnull String name, boolean strict) {
    StringBuilder result = null;

    int last = 0;
    int length = name.length();
    for (int i = 0; i < length; i++) {
      char c = name.charAt(i);
      boolean appendReplacement = true;
      if (c > 0 && c < 255) {
        if (strict ? Character.isLetterOrDigit(c) || c == '_' : Character.isJavaIdentifierPart(c) || c == ' ' || c == '@' || c == '-') {
          continue;
        }
      }
      else {
        appendReplacement = false;
      }

      if (result == null) {
        result = new StringBuilder();
      }
      if (last < i) {
        result.append(name, last, i);
      }
      if (appendReplacement) {
        result.append('_');
      }
      last = i + 1;
    }

    if (result == null) {
      return name;
    }

    if (last < length) {
      result.append(name, last, length);
    }

    return result.toString();
  }

  /**
   * Converts given path to canonical representation by eliminating '.'s, traversing '..'s, and omitting duplicate separators.
   * Please note that this method is symlink-unfriendly (i.e. result of "/path/to/link/../next" most probably will differ from
   * what {@link File#getCanonicalPath()} will return) - so use with care.<br>
   * <br>
   * If the path may contain symlinks, use {@link FileUtil#toCanonicalPath(String, boolean)} instead.
   */
  @Contract("null -> null")
  public static String toCanonicalPath(@Nullable String path) {
    return toCanonicalPath(path, File.separatorChar, true);
  }

  /**
   * When relative ../ parts do not escape outside of symlinks, the links are not expanded.<br>
   * That is, in the best-case scenario the original non-expanded path is preserved.<br>
   * <br>
   * Otherwise, returns a fully resolved path using {@link File#getCanonicalPath()}.<br>
   * <br>
   * Consider the following case:
   * <pre>
   * root/
   *   dir1/
   *     link_to_dir1
   *   dir2/
   * </pre>
   * 'root/dir1/link_to_dir1/../dir2' should be resolved to 'root/dir2'
   */
  @Contract("null, _ -> null")
  public static String toCanonicalPath(@Nullable String path, boolean resolveSymlinksIfNecessary) {
    return toCanonicalPath(path, File.separatorChar, true, resolveSymlinksIfNecessary);
  }

  private static final SymlinkResolver SYMLINK_RESOLVER = new SymlinkResolver() {
    @Nonnull
    @Override
    public String resolveSymlinksAndCanonicalize(@Nonnull String path, char separatorChar, boolean removeLastSlash) {
      try {
        return new File(path).getCanonicalPath().replace(separatorChar, '/');
      }
      catch (IOException ignore) {
        // fall back to the default behavior
        return toCanonicalPath(path, separatorChar, removeLastSlash, false);
      }
    }

    @Override
    public boolean isSymlink(@Nonnull CharSequence path) {
      return Files.isSymbolicLink(Paths.get(path.toString()));
    }
  };

  @Contract("null, _, _, _ -> null")
  private static String toCanonicalPath(@Nullable String path, final char separatorChar, final boolean removeLastSlash, final boolean resolveSymlinks) {
    SymlinkResolver symlinkResolver = resolveSymlinks ? SYMLINK_RESOLVER : null;
    return toCanonicalPath(path, separatorChar, removeLastSlash, symlinkResolver);
  }

  /**
   * Converts given path to canonical representation by eliminating '.'s, traversing '..'s, and omitting duplicate separators.
   * Please note that this method is symlink-unfriendly (i.e. result of "/path/to/link/../next" most probably will differ from
   * what {@link File#getCanonicalPath()} will return), so if the path may contain symlinks,
   * consider using {@link FileUtil#toCanonicalPath(String, boolean)} instead.
   */
  @Contract("null, _, _ -> null")
  public static String toCanonicalPath(@Nullable String path, char separatorChar, boolean removeLastSlash) {
    return toCanonicalPath(path, separatorChar, removeLastSlash, null);
  }

  @Contract("null, _, _, _ -> null")
  protected static String toCanonicalPath(@Nullable String path, final char separatorChar, final boolean removeLastSlash, final @Nullable SymlinkResolver resolver) {
    if (path == null || path.length() == 0) {
      return path;
    }
    if (path.charAt(0) == '.') {
      if (path.length() == 1) {
        return "";
      }
      char c = path.charAt(1);
      if (c == '/' || c == separatorChar) {
        path = path.substring(2);
      }
    }

    if (separatorChar != '/') {
      path = path.replace(separatorChar, '/');
    }
    // trying to speedup the common case when there are no "//" or "/."
    int index = -1;
    do {
      index = path.indexOf('/', index + 1);
      char next = index == path.length() - 1 ? 0 : path.charAt(index + 1);
      if (next == '.' || next == '/') {
        break;
      }
    }
    while (index != -1);
    if (index == -1) {
      if (removeLastSlash) {
        int start = processRoot(path, NullAppendable.INSTANCE);
        int slashIndex = path.lastIndexOf('/');
        return slashIndex != -1 && slashIndex > start && slashIndex == path.length() - 1 ? path.substring(0, path.length() - 1) : path;
      }
      return path;
    }

    StringBuilder result = new StringBuilder(path.length());
    int start = processRoot(path, result);
    int dots = 0;
    boolean separator = true;

    for (int i = start; i < path.length(); ++i) {
      char c = path.charAt(i);
      if (c == '/') {
        if (!separator) {
          if (!processDots(result, dots, start, resolver)) {
            return resolver.resolveSymlinksAndCanonicalize(path, separatorChar, removeLastSlash);
          }
          dots = 0;
        }
        separator = true;
      }
      else if (c == '.') {
        if (separator || dots > 0) {
          ++dots;
        }
        else {
          result.append('.');
        }
        separator = false;
      }
      else {
        while (dots > 0) {
          result.append('.');
          dots--;
        }
        result.append(c);
        separator = false;
      }
    }

    if (dots > 0) {
      if (!processDots(result, dots, start, resolver)) {
        return resolver.resolveSymlinksAndCanonicalize(path, separatorChar, removeLastSlash);
      }
    }

    int lastChar = result.length() - 1;
    if (removeLastSlash && lastChar >= 0 && result.charAt(lastChar) == '/' && lastChar > start) {
      result.deleteCharAt(lastChar);
    }

    return result.toString();
  }

  @SuppressWarnings("DuplicatedCode")
  private static int processRoot(@Nonnull String path, @Nonnull Appendable result) {
    try {
      if (OSInfo.isWindows && path.length() > 1 && path.charAt(0) == '/' && path.charAt(1) == '/') {
        result.append("//");

        int hostStart = 2;
        while (hostStart < path.length() && path.charAt(hostStart) == '/') hostStart++;
        if (hostStart == path.length()) return hostStart;
        int hostEnd = path.indexOf('/', hostStart);
        if (hostEnd < 0) hostEnd = path.length();
        result.append(path, hostStart, hostEnd);
        result.append('/');

        int shareStart = hostEnd;
        while (shareStart < path.length() && path.charAt(shareStart) == '/') shareStart++;
        if (shareStart == path.length()) return shareStart;
        int shareEnd = path.indexOf('/', shareStart);
        if (shareEnd < 0) shareEnd = path.length();
        result.append(path, shareStart, shareEnd);
        result.append('/');

        return shareEnd;
      }

      if (path.length() > 0 && path.charAt(0) == '/') {
        result.append('/');
        return 1;
      }

      if (path.length() > 2 && path.charAt(1) == ':' && path.charAt(2) == '/') {
        result.append(path, 0, 3);
        return 3;
      }

      return 0;
    }
    catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  @Contract("_, _, _, null -> true")
  private static boolean processDots(@Nonnull StringBuilder result, int dots, int start, SymlinkResolver symlinkResolver) {
    if (dots == 2) {
      int pos = -1;
      if (!StringUtil.endsWith(result, "/../") && !"../".contentEquals(result)) {
        pos = StringUtil.lastIndexOf(result, '/', start, result.length() - 1);
        if (pos >= 0) {
          ++pos;  // separator found, trim to next char
        }
        else if (start > 0) {
          pos = start;  // path is absolute, trim to root ('/..' -> '/')
        }
        else if (result.length() > 0) {
          pos = 0;  // path is relative, trim to default ('a/..' -> '')
        }
      }
      if (pos >= 0) {
        if (symlinkResolver != null && symlinkResolver.isSymlink(result)) {
          return false;
        }
        result.delete(pos, result.length());
      }
      else {
        result.append("../");  // impossible to traverse, keep as-is
      }
    }
    else if (dots != 1) {
      for (int i = 0; i < dots; i++) {
        result.append('.');
      }
      result.append('/');
    }
    return true;
  }
}
