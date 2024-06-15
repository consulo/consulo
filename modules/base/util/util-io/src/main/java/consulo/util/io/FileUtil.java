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

import consulo.util.collection.ArrayUtil;
import consulo.util.collection.ContainerUtil;
import consulo.util.collection.HashingStrategy;
import consulo.util.io.internal.OSInfo;
import consulo.util.lang.StringUtil;
import consulo.util.lang.ThreeState;
import org.intellij.lang.annotations.RegExp;
import org.jetbrains.annotations.Contract;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import java.io.*;
import java.nio.channels.FileChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.*;
import java.util.function.BiPredicate;
import java.util.function.Predicate;
import java.util.regex.Pattern;

public class FileUtil {
  private static final Logger LOG = LoggerFactory.getLogger(FileUtil.class);

  public static final int KILOBYTE = 1024;
  public static final int MEGABYTE = KILOBYTE * KILOBYTE;

  private static final int MAX_FILE_IO_ATTEMPTS = 10;
  private static final boolean USE_FILE_CHANNELS = "true".equalsIgnoreCase(System.getProperty("consulo.fs.useChannels"));
  public static final HashingStrategy<String> PATH_HASHING_STRATEGY = OSInfo.isFileSystemCaseSensitive ? HashingStrategy.caseInsensitive() : HashingStrategy.canonical();

  public static final HashingStrategy<File> FILE_HASHING_STRATEGY = OSInfo.isFileSystemCaseSensitive ? ContainerUtil.<File>canonicalStrategy() : new HashingStrategy<File>() {
    @Override
    public int hashCode(File object) {
      return fileHashCode(object);
    }

    @Override
    public boolean equals(File o1, File o2) {
      return filesEqual(o1, o2);
    }
  };

  public static final int THREAD_LOCAL_BUFFER_LENGTH = 1024 * 20;
  protected static final ThreadLocal<byte[]> BUFFER = new ThreadLocal<byte[]>() {
    @Override
    protected byte[] initialValue() {
      return new byte[THREAD_LOCAL_BUFFER_LENGTH];
    }
  };

  public static boolean visitFiles(@Nonnull File root, @Nonnull Predicate<? super File> processor) {
    if (!processor.test(root)) {
      return false;
    }

    File[] children = root.listFiles();
    if (children != null) {
      for (File child : children) {
        if (!visitFiles(child, processor)) {
          return false;
        }
      }
    }

    return true;
  }

  public static boolean extensionEquals(@Nonnull String filePath, @Nonnull String extension) {
    int extLen = extension.length();
    if (extLen == 0) {
      int lastSlash = Math.max(filePath.lastIndexOf('/'), filePath.lastIndexOf('\\'));
      return filePath.indexOf('.', lastSlash + 1) == -1;
    }
    int extStart = filePath.length() - extLen;
    return extStart >= 1 && filePath.charAt(extStart - 1) == '.' && filePath.regionMatches(!OSInfo.isFileSystemCaseSensitive, extStart, extension, 0, extLen);
  }

  @Nonnull
  public static File createTempFile(@Nonnull String prefix, @Nullable String suffix) throws IOException {
    return createTempFile(prefix, suffix, false); //false until TeamCity fixes its plugin
  }

  @Nonnull
  public static File createTempFile(@Nonnull String prefix, @Nullable String suffix, boolean deleteOnExit) throws IOException {
    final File dir = new File(getTempDirectory());
    return createTempFile(dir, prefix, suffix, true, deleteOnExit);
  }

  @Nonnull
  public static File createTempFile(File dir, @Nonnull String prefix, @Nullable String suffix) throws IOException {
    return createTempFile(dir, prefix, suffix, true, true);
  }

  @Nonnull
  public static File createTempFile(File dir, @Nonnull String prefix, @Nullable String suffix, boolean create) throws IOException {
    return createTempFile(dir, prefix, suffix, create, true);
  }

  @Nonnull
  public static File createTempFile(File dir, @Nonnull String prefix, @Nullable String suffix, boolean create, boolean deleteOnExit) throws IOException {
    File file = doCreateTempFile(dir, prefix, suffix, false);
    if (deleteOnExit) {
      //noinspection SSBasedInspection
      file.deleteOnExit();
    }
    if (!create) {
      if (!file.delete() && file.exists()) {
        throw new IOException("Cannot delete a file: " + file);
      }
    }
    return file;
  }

  private static final Random RANDOM = new Random();

  private static String ourCanonicalTempPathCache;

  @Nonnull
  public static String getTempDirectory() {
    if (ourCanonicalTempPathCache == null) {
      ourCanonicalTempPathCache = calcCanonicalTempPath();
    }
    return ourCanonicalTempPathCache;
  }

  @Nonnull
  private static String calcCanonicalTempPath() {
    final File file = new File(System.getProperty("java.io.tmpdir"));
    try {
      final String canonical = file.getCanonicalPath();
      if (!OSInfo.isWindows || !canonical.contains(" ")) {
        return canonical;
      }
    }
    catch (IOException ignore) {
    }
    return file.getAbsolutePath();
  }

  @Nonnull
  private static File doCreateTempFile(@Nonnull File dir, @Nonnull String prefix, @Nullable String suffix, boolean isDirectory) throws IOException {
    //noinspection ResultOfMethodCallIgnored
    dir.mkdirs();

    if (prefix.length() < 3) {
      prefix = (prefix + "___").substring(0, 3);
    }
    if (suffix == null) {
      suffix = "";
    }
    // normalize and use only the file name from the prefix
    prefix = new File(prefix).getName();

    int attempts = 0;
    int i = 0;
    int maxFileNumber = 10;
    IOException exception = null;
    while (true) {
      File f = null;
      try {
        f = calcName(dir, prefix, suffix, i);

        boolean success = isDirectory ? f.mkdir() : f.createNewFile();
        if (success) {
          return normalizeFile(f);
        }
      }
      catch (IOException e) { // Win32 createFileExclusively access denied
        exception = e;
      }
      attempts++;
      int MAX_ATTEMPTS = 100;
      if (attempts > maxFileNumber / 2 || attempts > MAX_ATTEMPTS) {
        String[] children = dir.list();
        int size = children == null ? 0 : children.length;
        maxFileNumber = Math.max(10, size * 10); // if too many files are in tmp dir, we need a bigger random range than meager 10
        if (attempts > MAX_ATTEMPTS) {
          throw exception != null ? exception : new IOException("Unable to create a temporary file " + f + "\nDirectory '" + dir + "' list (" + size + " children): " + Arrays.toString(children));
        }
      }

      i++; // for some reason the file1 can't be created (previous file1 was deleted but got locked by anti-virus?). Try file2.
      if (i > 2) {
        i = 2 + RANDOM.nextInt(maxFileNumber); // generate random suffix if too many failures
      }
    }
  }

  @Nonnull
  private static File calcName(@Nonnull File dir, @Nonnull String prefix, @Nonnull String suffix, int i) throws IOException {
    prefix = i == 0 ? prefix : prefix + i;
    if (prefix.endsWith(".") && suffix.startsWith(".")) {
      prefix = prefix.substring(0, prefix.length() - 1);
    }
    String name = prefix + suffix;
    File f = new File(dir, name);
    if (!name.equals(f.getName())) {
      throw new IOException("A generated name is malformed: '" + name + "' (" + f + ")");
    }
    return f;
  }

  @Nonnull
  private static File normalizeFile(@Nonnull File temp) throws IOException {
    final File canonical = temp.getCanonicalFile();
    return OSInfo.isWindows && canonical.getAbsolutePath().contains(" ") ? temp.getAbsoluteFile() : canonical;
  }

  public static boolean processFilesRecursively(@Nonnull File root, @Nonnull Predicate<File> processor) {
    return processFilesRecursively(root, processor, null);
  }

  public static boolean processFilesRecursively(@Nonnull File root, @Nonnull Predicate<File> processor, @Nullable final Predicate<File> directoryFilter) {
    final LinkedList<File> queue = new LinkedList<>();
    queue.add(root);
    while (!queue.isEmpty()) {
      final File file = queue.removeFirst();
      if (!processor.test(file)) return false;
      if (directoryFilter != null && (!file.isDirectory() || !directoryFilter.test(file))) continue;

      final File[] children = file.listFiles();
      if (children != null) {
        ContainerUtil.addAll(queue, children);
      }
    }
    return true;
  }

  public static boolean isAbsolute(@Nonnull String path) {
    return new File(path).isAbsolute();
  }

  public static boolean createIfDoesntExist(@Nonnull File file) {
    if (file.exists()) return true;
    try {
      if (!createParentDirs(file)) return false;

      OutputStream s = new FileOutputStream(file);
      s.close();
      return true;
    }
    catch (IOException e) {
      LOG.info(file.getPath(), e);
      return false;
    }
  }

  @Nonnull
  public static String unquote(@Nonnull String urlString) {
    urlString = urlString.replace('/', File.separatorChar);
    return URLUtil.unescapePercentSequences(urlString);
  }

  /**
   * optimized version of pathsEqual - it only compares pure names, without file separators
   */
  public static boolean namesEqual(@Nullable String name1, @Nullable String name2) {
    if (name1 == name2) return true;
    if (name1 == null || name2 == null) return false;

    return PATH_HASHING_STRATEGY.equals(name1, name2);
  }

  public static void writeToFile(@Nonnull File file, @Nonnull byte[] text) throws IOException {
    writeToFile(file, text, false);
  }

  public static void writeToFile(@Nonnull File file, @Nonnull String text) throws IOException {
    writeToFile(file, text, false);
  }

  public static void writeToFile(@Nonnull File file, @Nonnull String text, boolean append) throws IOException {
    writeToFile(file, text.getBytes(StandardCharsets.UTF_8), append);
  }

  public static void writeToFile(@Nonnull File file, @Nonnull byte[] text, int off, int len) throws IOException {
    writeToFile(file, text, off, len, false);
  }

  public static void writeToFile(@Nonnull File file, @Nonnull byte[] text, boolean append) throws IOException {
    writeToFile(file, text, 0, text.length, append);
  }

  private static void writeToFile(@Nonnull File file, @Nonnull byte[] text, int off, int len, boolean append) throws IOException {
    createParentDirs(file);

    try (OutputStream stream = new FileOutputStream(file, append)) {
      stream.write(text, off, len);
    }
  }

  @Nonnull
  public static String makeFileName(@Nonnull String name, @Nullable String extension) {
    return name + (StringUtil.isEmpty(extension) ? "" : "." + extension);
  }

  /**
   * <p>Gets the relative path from the {@code base} to the {@code file} regardless existence or the type of the {@code base}.</p>
   *
   * <p>NOTE: if a file (not a directory) is passed as the {@code base}, the result cannot be used as a relative path
   * from the {@code base} parent directory to the {@code file}.</p>
   *
   * @param base the base
   * @param file the file
   * @return the relative path from the {@code base} to the {@code file}, or {@code null}
   */
  @Nullable
  public static String getRelativePath(File base, File file) {
    if (base == null || file == null) return null;

    if (base.equals(file)) return ".";

    String filePath = file.getAbsolutePath();
    String basePath = base.getAbsolutePath();
    return getRelativePath(basePath, filePath, File.separatorChar);
  }

  @Nullable
  public static String getRelativePath(@Nonnull String basePath, @Nonnull String filePath, char separator) {
    return getRelativePath(basePath, filePath, separator, OSInfo.isFileSystemCaseSensitive);
  }

  @Nullable
  public static String getRelativePath(@Nonnull String basePath, @Nonnull String filePath, char separator, boolean caseSensitive) {
    basePath = ensureEnds(basePath, separator);

    if (caseSensitive ? basePath.equals(ensureEnds(filePath, separator)) : basePath.equalsIgnoreCase(ensureEnds(filePath, separator))) {
      return ".";
    }

    int len = 0;
    int lastSeparatorIndex = 0; // need this for cases like this: base="/temp/abc/base" and file="/temp/ab"
    BiPredicate<Character, Character> strategy = caseSensitive ? (o1, o2) -> o1.equals(o2) : (o1, o2) -> StringUtil.charsEqualIgnoreCase(o1, o2);
    while (len < filePath.length() && len < basePath.length() && strategy.test(filePath.charAt(len), basePath.charAt(len))) {
      if (basePath.charAt(len) == separator) {
        lastSeparatorIndex = len;
      }
      len++;
    }

    if (len == 0) return null;

    StringBuilder relativePath = new StringBuilder();
    for (int i = len; i < basePath.length(); i++) {
      if (basePath.charAt(i) == separator) {
        relativePath.append("..");
        relativePath.append(separator);
      }
    }
    relativePath.append(filePath.substring(lastSeparatorIndex + 1));

    return relativePath.toString();
  }

  private static String ensureEnds(@Nonnull String s, final char endsWith) {
    return StringUtil.endsWithChar(s, endsWith) ? s : s + endsWith;
  }

  @RegExp
  @Nonnull
  public static String convertAntToRegexp(@Nonnull String antPattern) {
    return convertAntToRegexp(antPattern, true);
  }

  /**
   * @param antPattern ant-style path pattern
   * @return java regexp pattern.
   * Note that no matter whether forward or backward slashes were used in the antPattern
   * the returned regexp pattern will use forward slashes ('/') as file separators.
   * Paths containing windows-style backslashes must be converted before matching against the resulting regexp
   * @see FileUtil#toSystemIndependentName
   */
  @RegExp
  @Nonnull
  public static String convertAntToRegexp(@Nonnull String antPattern, boolean ignoreStartingSlash) {
    final StringBuilder builder = new StringBuilder();
    int asteriskCount = 0;
    boolean recursive = true;
    final int start = ignoreStartingSlash && (StringUtil.startsWithChar(antPattern, '/') || StringUtil.startsWithChar(antPattern, '\\')) ? 1 : 0;
    for (int idx = start; idx < antPattern.length(); idx++) {
      final char ch = antPattern.charAt(idx);

      if (ch == '*') {
        asteriskCount++;
        continue;
      }

      final boolean foundRecursivePattern = recursive && asteriskCount == 2 && (ch == '/' || ch == '\\');
      final boolean asterisksFound = asteriskCount > 0;

      asteriskCount = 0;
      recursive = ch == '/' || ch == '\\';

      if (foundRecursivePattern) {
        builder.append("(?:[^/]+/)*?");
        continue;
      }

      if (asterisksFound) {
        builder.append("[^/]*?");
      }

      if (ch == '(' || ch == ')' || ch == '[' || ch == ']' || ch == '^' || ch == '$' || ch == '.' || ch == '{' || ch == '}' || ch == '+' || ch == '|') {
        // quote regexp-specific symbols
        builder.append('\\').append(ch);
        continue;
      }
      if (ch == '?') {
        builder.append("[^/]{1}");
        continue;
      }
      if (ch == '\\') {
        builder.append('/');
        continue;
      }
      builder.append(ch);
    }

    // handle ant shorthand: my_package/test/ is interpreted as if it were my_package/test/**
    final boolean isTrailingSlash = builder.length() > 0 && builder.charAt(builder.length() - 1) == '/';
    if (asteriskCount == 0 && isTrailingSlash || recursive && asteriskCount == 2) {
      if (isTrailingSlash) {
        builder.setLength(builder.length() - 1);
      }
      if (builder.length() == 0) {
        builder.append(".*");
      }
      else {
        builder.append("(?:$|/.+)");
      }
    }
    else if (asteriskCount > 0) {
      builder.append("[^/]*?");
    }
    return builder.toString();
  }

  @Nonnull
  public static byte[] loadBytes(@Nonnull InputStream stream, int length) throws IOException {
    if (length == 0) {
      return ArrayUtil.EMPTY_BYTE_ARRAY;
    }
    byte[] bytes = new byte[length];
    int count = 0;
    while (count < length) {
      int n = stream.read(bytes, count, length - count);
      if (n <= 0) break;
      count += n;
    }
    return bytes;
  }

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

  public static int fileHashCode(@Nullable File file) {
    return pathHashCode(file == null ? null : file.getPath());
  }

  public static int pathHashCode(@Nullable String path) {
    return StringUtil.isEmpty(path) ? 0 : PATH_HASHING_STRATEGY.hashCode(toCanonicalPath(path));
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

  @Contract("null -> null")
  public static String toCanonicalUriPath(@Nullable String path) {
    return toCanonicalPath(path, '/', false);
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

  public static boolean rename(@Nonnull File source, @Nonnull String newName, @Nonnull FilePermissionCopier permissionCopier) throws IOException {
    File target = new File(source.getParent(), newName);
    if (!OSInfo.isFileSystemCaseSensitive && newName.equalsIgnoreCase(source.getName())) {
      File intermediate = createTempFile(source.getParentFile(), source.getName(), ".tmp", false, false);
      return source.renameTo(intermediate) && intermediate.renameTo(target);
    }
    else {
      return source.renameTo(target);
    }
  }

  public static void rename(@Nonnull File source, @Nonnull File target, @Nonnull FilePermissionCopier permissionCopier) throws IOException {
    if (source.renameTo(target)) return;
    if (!source.exists()) return;

    copy(source, target, permissionCopier);
    delete(source);
  }

  public static boolean deleteWithRenaming(File file) {
    File tempFileNameForDeletion = findSequentNonexistentFile(file.getParentFile(), file.getName(), "");
    boolean success = file.renameTo(tempFileNameForDeletion);
    return delete(success ? tempFileNameForDeletion : file);
  }

  public static File findSequentNonexistentFile(@Nonnull File parentFolder, @Nonnull String filePrefix, @Nonnull String extension) {
    int postfix = 0;
    String ext = extension.isEmpty() ? "" : '.' + extension;
    File candidate = new File(parentFolder, filePrefix + ext);
    while (candidate.exists()) {
      postfix++;
      candidate = new File(parentFolder, filePrefix + Integer.toString(postfix) + ext);
    }
    return candidate;
  }

  public static int compareFiles(@Nullable File file1, @Nullable File file2) {
    return comparePaths(file1 == null ? null : file1.getPath(), file2 == null ? null : file2.getPath());
  }

  public static int comparePaths(@Nullable String path1, @Nullable String path2) {
    path1 = path1 == null ? null : toSystemIndependentName(path1);
    path2 = path2 == null ? null : toSystemIndependentName(path2);
    return StringUtil.compare(path1, path2, !OSInfo.isFileSystemCaseSensitive);
  }

  public static boolean isAbsolutePlatformIndependent(@Nonnull String path) {
    return isUnixAbsolutePath(path) || isWindowsAbsolutePath(path);
  }

  public static boolean isUnixAbsolutePath(@Nonnull String path) {
    return path.startsWith("/");
  }

  public static boolean isWindowsAbsolutePath(@Nonnull String pathString) {
    return pathString.length() >= 2 && Character.isLetter(pathString.charAt(0)) && pathString.charAt(1) == ':';
  }

  @Nonnull
  public static String join(@Nonnull final String... parts) {
    return StringUtil.join(parts, File.separator);
  }

  @Nullable
  public static File findAncestor(@Nonnull File f1, @Nonnull File f2) {
    File ancestor = f1;
    while (ancestor != null && !isAncestor(ancestor, f2, false)) {
      ancestor = ancestor.getParentFile();
    }
    return ancestor;
  }

  /**
   * Get parent for the file. The method correctly
   * processes "." and ".." in file names. The name
   * remains relative if was relative before.
   *
   * @param file a file to analyze
   * @return files's parent, or {@code null} if the file has no parent.
   */
  @Nullable
  public static File getParentFile(@Nonnull File file) {
    int skipCount = 0;
    File parentFile = file;
    while (true) {
      parentFile = parentFile.getParentFile();
      if (parentFile == null) {
        return null;
      }
      if (".".equals(parentFile.getName())) {
        continue;
      }
      if ("..".equals(parentFile.getName())) {
        skipCount++;
        continue;
      }
      if (skipCount > 0) {
        skipCount--;
        continue;
      }
      return parentFile;
    }
  }

  @Nonnull
  public static List<String> splitPath(@Nonnull String path) {
    ArrayList<String> list = new ArrayList<>();
    int index = 0;
    int nextSeparator;
    while ((nextSeparator = path.indexOf(File.separatorChar, index)) != -1) {
      list.add(path.substring(index, nextSeparator));
      index = nextSeparator + 1;
    }
    list.add(path.substring(index, path.length()));
    return list;
  }

  @Nullable
  public static File findFirstThatExist(@Nonnull String... paths) {
    for (String path : paths) {
      if (!StringUtil.isEmptyOrSpaces(path)) {
        File file = new File(toSystemDependentName(path));
        if (file.exists()) return file;
      }
    }

    return null;
  }

  @Nonnull
  public static List<File> findFilesByMask(@Nonnull Pattern pattern, @Nonnull File dir) {
    final ArrayList<File> found = new ArrayList<>();
    final File[] files = dir.listFiles();
    if (files != null) {
      for (File file : files) {
        if (file.isDirectory()) {
          found.addAll(findFilesByMask(pattern, file));
        }
        else if (pattern.matcher(file.getName()).matches()) {
          found.add(file);
        }
      }
    }
    return found;
  }

  @Nonnull
  public static List<File> findFilesOrDirsByMask(@Nonnull Pattern pattern, @Nonnull File dir) {
    final ArrayList<File> found = new ArrayList<>();
    final File[] files = dir.listFiles();
    if (files != null) {
      for (File file : files) {
        if (pattern.matcher(file.getName()).matches()) {
          found.add(file);
        }
        if (file.isDirectory()) {
          found.addAll(findFilesOrDirsByMask(pattern, file));
        }
      }
    }
    return found;
  }

  public static boolean isJarOrZip(File file) {
    return isJarOrZip(file, true);
  }

  public static boolean isJarOrZip(File file, boolean isCheckIsDirectory) {
    if (isCheckIsDirectory && file.isDirectory()) {
      return false;
    }

    // do not use getName to avoid extra String creation (File.getName() calls substring)
    final String path = file.getPath();
    return StringUtil.endsWithIgnoreCase(path, ".jar") || StringUtil.endsWithIgnoreCase(path, ".zip");
  }

  /**
   * Returns empty string for empty path.
   * First checks whether provided path is a path of a file with sought-for name.
   * Unless found, checks if provided file was a directory. In this case checks existence
   * of child files with given names in order "as provided". Finally checks filename among
   * brother-files of provided. Returns null if nothing found.
   *
   * @return path of the first of found files or empty string or null.
   */
  @Nullable
  public static String findFileInProvidedPath(String providedPath, String... fileNames) {
    if (StringUtil.isEmpty(providedPath)) {
      return "";
    }

    File providedFile = new File(providedPath);
    if (providedFile.exists() && ArrayUtil.indexOf(fileNames, providedFile.getName()) >= 0) {
      return toSystemDependentName(providedFile.getPath());
    }

    if (providedFile.isDirectory()) {  //user chose folder with file
      for (String fileName : fileNames) {
        File file = new File(providedFile, fileName);
        if (fileName.equals(file.getName()) && file.exists()) {
          return toSystemDependentName(file.getPath());
        }
      }
    }

    providedFile = providedFile.getParentFile();  //users chose wrong file in same directory
    if (providedFile != null && providedFile.exists()) {
      for (String fileName : fileNames) {
        File file = new File(providedFile, fileName);
        if (fileName.equals(file.getName()) && file.exists()) {
          return toSystemDependentName(file.getPath());
        }
      }
    }

    return null;
  }

  public static boolean isFilePathAcceptable(@Nonnull File root, @Nullable FileFilter fileFilter) {
    if (fileFilter == null) return true;
    File file = root;
    do {
      if (!fileFilter.accept(file)) return false;
      file = file.getParentFile();
    }
    while (file != null);
    return true;
  }
}
