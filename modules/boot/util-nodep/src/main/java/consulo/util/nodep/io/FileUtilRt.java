// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.util.nodep.io;

import consulo.util.nodep.ArrayUtilRt;
import consulo.util.nodep.LoggerRt;
import consulo.util.nodep.SystemInfoRt;
import consulo.util.nodep.text.StringUtilRt;

import java.io.*;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.nio.channels.FileChannel;
import java.nio.charset.Charset;
import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * A stripped-down version of {@link consulo.ide.impl.idea.openapi.util.io.FileUtil}.
 * Intended to use by external (out-of-IDE-process) runners and helpers, so it should not contain any library dependencies.
 */
@SuppressWarnings("UtilityClassWithoutPrivateConstructor")
public class FileUtilRt {
  private static final int MAX_FILE_IO_ATTEMPTS = 10;
  private static final boolean USE_FILE_CHANNELS = "true".equalsIgnoreCase(System.getProperty("idea.fs.useChannels"));

  public static final FileFilter ALL_FILES = new FileFilter() {
    public boolean accept(File file) {
      return true;
    }
  };
  public static final FileFilter ALL_DIRECTORIES = new FileFilter() {
    public boolean accept(File file) {
      return file.isDirectory();
    }
  };

  public static final int THREAD_LOCAL_BUFFER_LENGTH = 1024 * 20;
  protected static final ThreadLocal<byte[]> BUFFER = new ThreadLocal<byte[]>() {
    @Override
    protected byte[] initialValue() {
      return new byte[THREAD_LOCAL_BUFFER_LENGTH];
    }
  };

  private static String ourCanonicalTempPathCache;

  public static boolean isJarOrZip(File file) {
    return isJarOrZip(file, true);
  }

  public static boolean isJarOrZip(File file, boolean isCheckIsDirectory) {
    if (isCheckIsDirectory && file.isDirectory()) {
      return false;
    }

    // do not use getName to avoid extra String creation (File.getName() calls substring)
    final String path = file.getPath();
    return StringUtilRt.endsWithIgnoreCase(path, ".jar") || StringUtilRt.endsWithIgnoreCase(path, ".zip");
  }

  protected interface SymlinkResolver {

    String resolveSymlinksAndCanonicalize(String path, char separatorChar, boolean removeLastSlash);

    boolean isSymlink(CharSequence path);
  }

  /* NIO-reflection initialization placed in a separate class for lazy loading */
  //@ReviseWhenPortedToJDK("7")
  private static final class NIOReflect {
    static final boolean IS_AVAILABLE;

    static Object toPath(File file) throws InvocationTargetException, IllegalAccessException {
      return ourFileToPathMethod.invoke(file);
    }

    static void deleteRecursively(Object path) throws InvocationTargetException, IllegalAccessException {
      try {
        ourFilesWalkMethod.invoke(null, path, ourDeletionVisitor);
      }
      catch (InvocationTargetException e) {
        if (!ourNoSuchFileExceptionClass.isInstance(e.getCause())) {
          throw e;
        }
      }
    }

    private static Method ourFilesDeleteIfExistsMethod;
    private static Method ourFilesWalkMethod;
    private static Method ourFileToPathMethod;
    private static Method ourPathToFileMethod;
    private static Method ourAttributesIsOtherMethod;
    private static Object ourDeletionVisitor;
    private static Class<?> ourNoSuchFileExceptionClass;
    private static Class<?> ourAccessDeniedExceptionClass;

    static {
      boolean initSuccess = false;
      try {
        final Class<?> pathClass = Class.forName("java.nio.file.Path");
        final Class<?> visitorClass = Class.forName("java.nio.file.FileVisitor");
        final Class<?> filesClass = Class.forName("java.nio.file.Files");
        ourNoSuchFileExceptionClass = Class.forName("java.nio.file.NoSuchFileException");
        ourAccessDeniedExceptionClass = Class.forName("java.nio.file.AccessDeniedException");

        ourFileToPathMethod = Class.forName("java.io.File").getMethod("toPath");
        ourPathToFileMethod = pathClass.getMethod("toFile");
        ourFilesWalkMethod = filesClass.getMethod("walkFileTree", pathClass, visitorClass);
        ourAttributesIsOtherMethod = Class.forName("java.nio.file.attribute.BasicFileAttributes").getDeclaredMethod("isOther");
        ourFilesDeleteIfExistsMethod = filesClass.getMethod("deleteIfExists", pathClass);

        final Object Result_Continue = Class.forName("java.nio.file.FileVisitResult").getDeclaredField("CONTINUE").get(null);
        final Object Result_Skip = Class.forName("java.nio.file.FileVisitResult").getDeclaredField("SKIP_SUBTREE").get(null);

        ourDeletionVisitor = Proxy.newProxyInstance(FileUtilRt.class.getClassLoader(), new Class[]{visitorClass}, new InvocationHandler() {
          public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
            if (args.length == 2) {
              final Object second = args[1];
              if (second instanceof Throwable) {
                throw (Throwable)second;
              }
              final String methodName = method.getName();
              if ("visitFile".equals(methodName) || "postVisitDirectory".equals(methodName)) {
                performDelete(args[0]);
              }
              else if (SystemInfoRt.isWindows && "preVisitDirectory".equals(methodName)) {
                boolean notDirectory = false;
                try {
                  notDirectory = Boolean.TRUE.equals(ourAttributesIsOtherMethod.invoke(second));
                }
                catch (Throwable ignored) {
                }
                if (notDirectory) {
                  performDelete(args[0]);
                  return Result_Skip;
                }
              }
            }
            return Result_Continue;
          }

          private void performDelete(final Object fileObject) throws IOException {
            Boolean result = doIOOperation(new RepeatableIOOperation<Boolean, RuntimeException>() {
              public Boolean execute(boolean lastAttempt) {
                try {
                  //Files.deleteIfExists(file);
                  ourFilesDeleteIfExistsMethod.invoke(null, fileObject);
                  return Boolean.TRUE;
                }
                catch (InvocationTargetException e) {
                  final Throwable cause = e.getCause();
                  if (!(cause instanceof IOException)) {
                    return Boolean.FALSE;
                  }
                  if (ourAccessDeniedExceptionClass.isInstance(cause)) {
                    // file is read-only: fallback to standard java.io API
                    try {
                      final File file = (File)ourPathToFileMethod.invoke(fileObject);
                      if (file == null) {
                        return Boolean.FALSE;
                      }
                      if (file.delete() || !file.exists()) {
                        return Boolean.TRUE;
                      }
                    }
                    catch (Throwable ignored) {
                      return Boolean.FALSE;
                    }
                  }
                }
                catch (IllegalAccessException e) {
                  return Boolean.FALSE;
                }
                return lastAttempt ? Boolean.FALSE : null;
              }
            });
            if (!Boolean.TRUE.equals(result)) {
              throw new IOException("Failed to delete " + fileObject) {
                @Override
                public synchronized Throwable fillInStackTrace() {
                  return this; // optimization: the stacktrace is not needed: the exception is used to terminate tree walking and to pass the result
                }
              };
            }
          }
        });
        initSuccess = true;
      }
      catch (Throwable ignored) {
        logger().info("Was not able to detect NIO API");
        ourFileToPathMethod = null;
        ourFilesWalkMethod = null;
        ourFilesDeleteIfExistsMethod = null;
        ourDeletionVisitor = null;
        ourNoSuchFileExceptionClass = null;
      }
      IS_AVAILABLE = initSuccess;
    }
  }

  /**
   * Converts given path to canonical representation by eliminating '.'s, traversing '..'s, and omitting duplicate separators.
   * Please note that this method is symlink-unfriendly (i.e. result of "/path/to/link/../next" most probably will differ from
   * what {@link File#getCanonicalPath()} will return), so if the path may contain symlinks,
   * consider using {@link consulo.ide.impl.idea.openapi.util.io.FileUtil#toCanonicalPath(String, boolean)} instead.
   */
  public static String toCanonicalPath(String path, char separatorChar, boolean removeLastSlash) {
    return toCanonicalPath(path, separatorChar, removeLastSlash, null);
  }

  protected static String toCanonicalPath(String path, final char separatorChar, final boolean removeLastSlash, final SymlinkResolver resolver) {
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
  private static int processRoot(String path, Appendable result) {
    try {
      if (SystemInfoRt.isWindows && path.length() > 1 && path.charAt(0) == '/' && path.charAt(1) == '/') {
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

  private static boolean processDots(StringBuilder result, int dots, int start, SymlinkResolver symlinkResolver) {
    if (dots == 2) {
      int pos = -1;
      if (!StringUtilRt.endsWith(result, "/../") && !"../".contentEquals(result)) {
        pos = StringUtilRt.lastIndexOf(result, '/', start, result.length() - 1);
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


  public static String getExtension(String fileName) {
    int index = fileName.lastIndexOf('.');
    if (index < 0) return "";
    return fileName.substring(index + 1);
  }


  public static CharSequence getExtension(CharSequence fileName) {
    return getExtension(fileName, "");
  }

  public static CharSequence getExtension(CharSequence fileName, String defaultValue) {
    int index = StringUtilRt.lastIndexOf(fileName, '.', 0, fileName.length());
    if (index < 0) {
      return defaultValue;
    }
    return fileName.subSequence(index + 1, fileName.length());
  }

  public static boolean extensionEquals(String filePath, String extension) {
    int extLen = extension.length();
    if (extLen == 0) {
      int lastSlash = Math.max(filePath.lastIndexOf('/'), filePath.lastIndexOf('\\'));
      return filePath.indexOf('.', lastSlash + 1) == -1;
    }
    int extStart = filePath.length() - extLen;
    return extStart >= 1 && filePath.charAt(extStart - 1) == '.' && filePath.regionMatches(!SystemInfoRt.isFileSystemCaseSensitive, extStart, extension, 0, extLen);
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

  public static String getRelativePath(File base, File file) {
    if (base == null || file == null) return null;

    if (base.equals(file)) return ".";

    String filePath = file.getAbsolutePath();
    String basePath = base.getAbsolutePath();
    return getRelativePath(basePath, filePath, File.separatorChar);
  }


  public static String getRelativePath(String basePath, String filePath, char separator) {
    return getRelativePath(basePath, filePath, separator, SystemInfoRt.isFileSystemCaseSensitive);
  }


  public static String getRelativePath(String basePath, String filePath, char separator, boolean caseSensitive) {
    basePath = ensureEnds(basePath, separator);

    if (caseSensitive ? basePath.equals(ensureEnds(filePath, separator)) : basePath.equalsIgnoreCase(ensureEnds(filePath, separator))) {
      return ".";
    }

    int len = 0;
    int lastSeparatorIndex = 0; // need this for cases like this: base="/temp/abc/base" and file="/temp/ab"
    CharComparingStrategy strategy = caseSensitive ? CharComparingStrategy.IDENTITY : CharComparingStrategy.CASE_INSENSITIVE;
    while (len < filePath.length() && len < basePath.length() && strategy.charsEqual(filePath.charAt(len), basePath.charAt(len))) {
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

  private static String ensureEnds(String s, final char endsWith) {
    return StringUtilRt.endsWithChar(s, endsWith) ? s : s + endsWith;
  }


  public static CharSequence getNameWithoutExtension(CharSequence name) {
    int i = StringUtilRt.lastIndexOf(name, '.', 0, name.length());
    return i < 0 ? name : name.subSequence(0, i);
  }


  public static String getNameWithoutExtension(String name) {
    return getNameWithoutExtension((CharSequence)name).toString();
  }


  public static File createTempDirectory(String prefix, String suffix) throws IOException {
    return createTempDirectory(prefix, suffix, true);
  }


  public static File createTempDirectory(String prefix, String suffix, boolean deleteOnExit) throws IOException {
    final File dir = new File(getTempDirectory());
    return createTempDirectory(dir, prefix, suffix, deleteOnExit);
  }


  public static File createTempDirectory(File dir, String prefix, String suffix) throws IOException {
    return createTempDirectory(dir, prefix, suffix, true);
  }


  public static File createTempDirectory(File dir, String prefix, String suffix, boolean deleteOnExit) throws IOException {
    File file = doCreateTempFile(dir, prefix, suffix, true);
    if (deleteOnExit) {
      // default deleteOnExit does not remove dirs if they are not empty
      FilesToDeleteHolder.ourFilesToDelete.add(file.getPath());
    }
    if (!file.isDirectory()) {
      throw new IOException("Cannot create a directory: " + file);
    }
    return file;
  }

  private static class FilesToDeleteHolder {
    private static final Queue<String> ourFilesToDelete = createFilesToDelete();

    private static Queue<String> createFilesToDelete() {
      final ConcurrentLinkedQueue<String> queue = new ConcurrentLinkedQueue<String>();
      Runtime.getRuntime().addShutdownHook(new Thread("FileUtil deleteOnExit") {
        @Override
        public void run() {
          String name;
          while ((name = queue.poll()) != null) {
            delete(new File(name));
          }
        }
      });
      return queue;
    }
  }


  public static File createTempFile(String prefix, String suffix) throws IOException {
    return createTempFile(prefix, suffix, false); //false until TeamCity fixes its plugin
  }


  public static File createTempFile(String prefix, String suffix, boolean deleteOnExit) throws IOException {
    final File dir = new File(getTempDirectory());
    return createTempFile(dir, prefix, suffix, true, deleteOnExit);
  }


  public static File createTempFile(File dir, String prefix, String suffix) throws IOException {
    return createTempFile(dir, prefix, suffix, true, true);
  }


  public static File createTempFile(File dir, String prefix, String suffix, boolean create) throws IOException {
    return createTempFile(dir, prefix, suffix, create, true);
  }


  public static File createTempFile(File dir, String prefix, String suffix, boolean create, boolean deleteOnExit) throws IOException {
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


  private static File doCreateTempFile(File dir, String prefix, String suffix, boolean isDirectory) throws IOException {
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


  private static File calcName(File dir, String prefix, String suffix, int i) throws IOException {
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


  private static File normalizeFile(File temp) throws IOException {
    final File canonical = temp.getCanonicalFile();
    return SystemInfoRt.isWindows && canonical.getAbsolutePath().contains(" ") ? temp.getAbsoluteFile() : canonical;
  }


  public static String getTempDirectory() {
    if (ourCanonicalTempPathCache == null) {
      ourCanonicalTempPathCache = calcCanonicalTempPath();
    }
    return ourCanonicalTempPathCache;
  }


  private static String calcCanonicalTempPath() {
    final File file = new File(System.getProperty("java.io.tmpdir"));
    try {
      final String canonical = file.getCanonicalPath();
      if (!SystemInfoRt.isWindows || !canonical.contains(" ")) {
        return canonical;
      }
    }
    catch (IOException ignore) {
    }
    return file.getAbsolutePath();
  }

  static void resetCanonicalTempPathCache(final String tempPath) {
    ourCanonicalTempPathCache = tempPath;
  }


  public static File generateRandomTemporaryPath() throws IOException {
    File file = new File(getTempDirectory(), UUID.randomUUID().toString());
    int i = 0;
    while (file.exists() && i < 5) {
      file = new File(getTempDirectory(), UUID.randomUUID().toString());
      ++i;
    }
    if (file.exists()) {
      throw new IOException("Couldn't generate unique random path.");
    }
    return normalizeFile(file);
  }

  /**
   * @deprecated not needed in 'util-rt'; use {@link consulo.ide.impl.idea.openapi.util.io.FileUtil} or {@link File} methods; for removal in IDEA 2020
   */
  @Deprecated
  @SuppressWarnings("ALL")
  public static void setExecutableAttribute(String path, boolean executableFlag) throws IOException {
    try {
      File file = new File(path);
      //noinspection Since15
      if (!file.setExecutable(executableFlag) && file.canExecute() != executableFlag) {
        logger().warn("Can't set executable attribute of '" + path + "' to " + executableFlag);
      }
    }
    catch (LinkageError ignored) {
    }
  }


  public static String loadFile(File file) throws IOException {
    return loadFile(file, null, false);
  }


  public static String loadFile(File file, boolean convertLineSeparators) throws IOException {
    return loadFile(file, null, convertLineSeparators);
  }


  public static String loadFile(File file, String encoding) throws IOException {
    return loadFile(file, encoding, false);
  }


  public static String loadFile(File file, String encoding, boolean convertLineSeparators) throws IOException {
    final String s = new String(loadFileText(file, encoding));
    return convertLineSeparators ? StringUtilRt.convertLineSeparators(s) : s;
  }


  public static char[] loadFileText(File file) throws IOException {
    return loadFileText(file, (String)null);
  }


  public static char[] loadFileText(File file, String encoding) throws IOException {
    InputStream stream = new FileInputStream(file);
    @SuppressWarnings("ImplicitDefaultCharsetUsage") Reader reader = encoding == null ? new InputStreamReader(stream) : new InputStreamReader(stream, encoding);
    try {
      return loadText(reader, (int)file.length());
    }
    finally {
      reader.close();
    }
  }


  public static char[] loadFileText(File file, Charset encoding) throws IOException {
    Reader reader = new InputStreamReader(new FileInputStream(file), encoding);
    try {
      return loadText(reader, (int)file.length());
    }
    finally {
      reader.close();
    }
  }


  public static char[] loadText(Reader reader, int length) throws IOException {
    char[] chars = new char[length];
    int count = 0;
    while (count < chars.length) {
      int n = reader.read(chars, count, chars.length - count);
      if (n <= 0) break;
      count += n;
    }
    if (count == chars.length) {
      return chars;
    }
    else {
      char[] newChars = new char[count];
      System.arraycopy(chars, 0, newChars, 0, count);
      return newChars;
    }
  }


  public static List<String> loadLines(File file) throws IOException {
    return loadLines(file.getPath());
  }


  public static List<String> loadLines(File file, String encoding) throws IOException {
    return loadLines(file.getPath(), encoding);
  }


  public static List<String> loadLines(String path) throws IOException {
    return loadLines(path, null);
  }


  public static List<String> loadLines(String path, String encoding) throws IOException {
    InputStream stream = new FileInputStream(path);
    try {
      @SuppressWarnings("ImplicitDefaultCharsetUsage") BufferedReader reader = new BufferedReader(encoding == null ? new InputStreamReader(stream) : new InputStreamReader(stream, encoding));
      try {
        return loadLines(reader);
      }
      finally {
        reader.close();
      }
    }
    finally {
      stream.close();
    }
  }


  public static List<String> loadLines(BufferedReader reader) throws IOException {
    List<String> lines = new ArrayList<String>();
    String line;
    while ((line = reader.readLine()) != null) {
      lines.add(line);
    }
    return lines;
  }


  public static byte[] loadBytes(InputStream stream) throws IOException {
    ByteArrayOutputStream buffer = new ByteArrayOutputStream();
    copy(stream, buffer);
    return buffer.toByteArray();
  }

  public static byte[] loadBytes(InputStream stream, int length) throws IOException {
    if (length == 0) {
      return ArrayUtilRt.EMPTY_BYTE_ARRAY;
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

  /**
   * Get parent for the file. The method correctly
   * processes "." and ".." in file names. The name
   * remains relative if was relative before.
   *
   * @param file a file to analyze
   * @return files's parent, or {@code null} if the file has no parent.
   */

  public static File getParentFile(File file) {
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

  /**
   * <b>IMPORTANT</b>: the method is not symlinks- or junction-aware when invoked on Java 6 or earlier.
   *
   * @param file file or directory to delete
   * @return true if the file did not exist or was successfully deleted
   */
  public static boolean delete(File file) {
    if (NIOReflect.IS_AVAILABLE) {
      try {
        deleteRecursivelyNIO(NIOReflect.toPath(file));
        return true;
      }
      catch (IOException e) {
        return false;
      }
      catch (Exception e) {
        logger().info(e);
        return false;
      }
    }
    else {
      return deleteRecursively(file);
    }
  }

  static void deleteRecursivelyNIO(Object path) throws IOException {
    try {
      NIOReflect.deleteRecursively(path);
    }
    catch (InvocationTargetException e) {
      Throwable cause = e.getCause();
      if (cause instanceof IOException) {
        throw (IOException)cause;
      }
    }
    catch (Exception e) {
      logger().info(e);
    }
  }

  private static boolean deleteRecursively(File file) {
    File[] files = file.listFiles();
    if (files != null) {
      for (File child : files) {
        if (!deleteRecursively(child)) return false;
      }
    }

    return deleteFile(file);
  }

  public interface RepeatableIOOperation<T, E extends Throwable> {

    T execute(boolean lastAttempt) throws E;
  }


  public static <T, E extends Throwable> T doIOOperation(RepeatableIOOperation<T, E> ioTask) throws E {
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

  protected static boolean deleteFile(final File file) {
    Boolean result = doIOOperation(new RepeatableIOOperation<Boolean, RuntimeException>() {
      public Boolean execute(boolean lastAttempt) {
        if (file.delete() || !file.exists()) {
          return Boolean.TRUE;
        }
        else if (lastAttempt) {
          return Boolean.FALSE;
        }
        else {
          return null;
        }
      }
    });
    return Boolean.TRUE.equals(result);
  }

  public static boolean ensureCanCreateFile(File file) {
    if (file.exists()) return file.canWrite();
    if (!createIfNotExists(file)) return false;
    return delete(file);
  }

  public static boolean createIfNotExists(File file) {
    if (file.exists()) return true;
    try {
      if (!createParentDirs(file)) return false;

      OutputStream s = new FileOutputStream(file);
      s.close();
      return true;
    }
    catch (IOException e) {
      logger().info(e);
      return false;
    }
  }

  public static boolean createParentDirs(File file) {
    File parentPath = file.getParentFile();
    return parentPath == null || createDirectory(parentPath);
  }

  public static boolean createDirectory(File path) {
    return path.isDirectory() || path.mkdirs();
  }

  public static void copy(File fromFile, File toFile) throws IOException {
    if (!ensureCanCreateFile(toFile)) {
      return;
    }

    FileOutputStream fos = new FileOutputStream(toFile);
    try {
      FileInputStream fis = new FileInputStream(fromFile);
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

    long timeStamp = fromFile.lastModified();
    if (timeStamp < 0) {
      logger().warn("Invalid timestamp " + timeStamp + " of '" + fromFile + "'");
    }
    else if (!toFile.setLastModified(timeStamp)) {
      logger().warn("Unable to set timestamp " + timeStamp + " to '" + toFile + "'");
    }
  }

  public static void copy(InputStream inputStream, OutputStream outputStream) throws IOException {
    if (USE_FILE_CHANNELS && inputStream instanceof FileInputStream && outputStream instanceof FileOutputStream) {
      final FileChannel fromChannel = ((FileInputStream)inputStream).getChannel();
      try {
        final FileChannel toChannel = ((FileOutputStream)outputStream).getChannel();
        try {
          fromChannel.transferTo(0, Long.MAX_VALUE, toChannel);
        }
        finally {
          toChannel.close();
        }
      }
      finally {
        fromChannel.close();
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


  public static byte[] getThreadLocalBuffer() {
    return BUFFER.get();
  }


  private interface CharComparingStrategy {
    CharComparingStrategy IDENTITY = new CharComparingStrategy() {
      public boolean charsEqual(char ch1, char ch2) {
        return ch1 == ch2;
      }
    };
    CharComparingStrategy CASE_INSENSITIVE = new CharComparingStrategy() {
      public boolean charsEqual(char ch1, char ch2) {
        return StringUtilRt.charsEqualIgnoreCase(ch1, ch2);
      }
    };

    boolean charsEqual(char ch1, char ch2);
  }

  private static LoggerRt logger() {
    return LoggerRt.getInstance("#consulo.ide.impl.idea.openapi.util.io.FileUtilRt");
  }
}