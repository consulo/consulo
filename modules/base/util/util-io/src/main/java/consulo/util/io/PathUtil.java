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
package consulo.util.io;

import consulo.util.lang.StringUtil;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.slf4j.LoggerFactory;

import java.nio.charset.Charset;
import java.util.Locale;
import java.util.Set;

/**
 * @author nik
 */
public class PathUtil {
  @Nullable
  public static String getFileExtension(@Nonnull String name) {
    int index = name.lastIndexOf('.');
    if (index < 0) return null;
    return name.substring(index + 1);
  }

  public static String getCanonicalPath(@NonNls String path) {
    return FileUtil.toCanonicalPath(path);
  }

  @Contract("null -> null; !null -> !null")
  public static String toSystemIndependentName(@Nullable String path) {
    return path == null ? null : FileUtil.toSystemIndependentName(path);
  }


  @Contract("null -> null; !null -> !null")
  public static String toSystemDependentName(@Nullable String path) {
    return path == null ? null : FileUtil.toSystemDependentName(path);
  }

  @Nonnull
  public static String makeFileName(@Nonnull String name, @Nullable String extension) {
    return name + (StringUtil.isEmpty(extension) ? "" : "." + extension);
  }

  @Nonnull
  public static String getFileName(@Nonnull String path) {
    if (StringUtil.isEmpty(path)) {
      return "";
    }

    int end = getEnd(path);
    int start = getLastIndexOfPathSeparator(path, end);
    if (isWindowsUNCRoot(path, start)) {
      start = -1;
    }
    return path.substring(start + 1, end);
  }

  private static int getEnd(@Nonnull String path) {
    char c = path.charAt(path.length() - 1);
    return c == '/' || c == '\\' ? path.length() - 1 : path.length();
  }

  @Nonnull
  public static String getParentPath(@Nonnull String path) {
    if (path.length() == 0) return "";
    int end = Math.max(path.lastIndexOf('/'), path.lastIndexOf('\\'));
    if (end == path.length() - 1) {
      end = getLastIndexOfPathSeparator(path, end);
    }
    if (end == -1 || end == 0) {
      return "";
    }
    if (isWindowsUNCRoot(path, end)) {
      return "";
    }
    // parent of '//host' is root
    char prev = path.charAt(end - 1);
    if (prev == '/' || prev == '\\') {
      end--;
    }
    return path.substring(0, end);
  }

  private static int getLastIndexOfPathSeparator(@Nonnull String path, int end) {
    return Math.max(path.lastIndexOf('/', end - 1), path.lastIndexOf('\\', end - 1));
  }

  private static boolean isWindowsUNCRoot(@Nonnull String path, int lastPathSeparatorPosition) {
    return Target.CURRENT == Target.WINDOWS && (path.startsWith("//") || path.startsWith("\\\\")) && getLastIndexOfPathSeparator(path, lastPathSeparatorPosition) == 1;
  }

  @Nonnull
  public static String suggestFileName(@Nonnull String text) {
    return suggestFileName(text, false, false);
  }

  @Nonnull
  public static String suggestFileName(@Nonnull String text, boolean allowDots, boolean allowSpaces) {
    StringBuilder result = new StringBuilder();
    for (int i = 0; i < text.length(); i++) {
      char c = text.charAt(i);
      if (!isValidFileNameChar(c, Target.CURRENT, true) || (!allowDots && c == '.') || (!allowSpaces && Character.isWhitespace(c))) {
        result.append('_');
      }
      else {
        result.append(c);
      }
    }
    return result.toString();
  }

  /**
   * Checks whether a file with the given name can be created on a current platform.
   *
   * @see #isValidFileName(String, Target, boolean, Charset)
   */
  public static boolean isValidFileName(@Nonnull String fileName, boolean strict) {
    return isValidFileName(fileName, Target.CURRENT, strict, FS_CHARSET);
  }

  public static boolean isValidFileName(@Nonnull String fileName) {
    return isValidFileName(fileName, true);
  }

  public enum Target {
    UNIX,
    WINDOWS;
    public static Target CURRENT = OSInfo.isWindows ? WINDOWS : UNIX;
  }

  /**
   * Checks whether a file with the given name can be created on a platform specified by given parameters.
   * <p>
   * Platform restrictions:<br>
   * {@code Platform.UNIX} prohibits empty names, traversals (".", ".."), and names containing '/' or '\' characters.<br>
   * {@code Platform.WINDOWS} prohibits empty names, traversals (".", ".."), reserved names ("CON", "NUL", "COM1" etc.),
   * and names containing any of characters {@code <>:"/\|?*} or control characters (range 0..31)
   * (<a href="https://msdn.microsoft.com/en-us/library/windows/desktop/aa365247(v=vs.85).aspx">more info</a>).
   *
   * @param os     specifies a platform.
   * @param strict prohibits names containing any of characters {@code <>:"/\|?*;} and control characters (range 0..31).
   * @param cs     prohibits names which cannot be encoded by this charset (optional).
   */
  public static boolean isValidFileName(@Nonnull String name, @Nonnull Target os, boolean strict, @Nullable Charset cs) {
    if (name.length() == 0 || name.equals(".") || name.equals("..")) {
      return false;
    }

    for (int i = 0; i < name.length(); i++) {
      if (!isValidFileNameChar(name.charAt(i), os, strict)) {
        return false;
      }
    }

    if (os == Target.WINDOWS && name.length() >= 3 && name.length() <= 4 && WINDOWS_NAMES.contains(name.toUpperCase(Locale.US))) {
      return false;
    }

    if (cs != null && !(cs.canEncode() && cs.newEncoder().canEncode(name))) {
      return false;
    }

    return true;
  }

  private static boolean isValidFileNameChar(char c, Target os, boolean strict) {
    if (c == '/' || c == '\\') return false;
    if ((strict || os == Target.WINDOWS) && (c < 32 || WINDOWS_CHARS.indexOf(c) >= 0)) return false;
    if (strict && c == ';') return false;
    return true;
  }

  private static final String WINDOWS_CHARS = "<>:\"|?*";
  private static final Set<String> WINDOWS_NAMES =
          Set.of("CON", "PRN", "AUX", "NUL", "COM1", "COM2", "COM3", "COM4", "COM5", "COM6", "COM7", "COM8", "COM9", "LPT1", "LPT2", "LPT3", "LPT4", "LPT5", "LPT6", "LPT7", "LPT8", "LPT9");

  private static final Charset FS_CHARSET = fsCharset();

  private static Charset fsCharset() {
    if (!OSInfo.isWindows && !OSInfo.isMac) {
      String property = System.getProperty("sun.jnu.encoding");
      if (property != null) {
        try {
          return Charset.forName(property);
        }
        catch (Exception e) {
          LoggerFactory.getLogger(PathUtil.class).warn("unknown JNU charset: " + property, e);
        }
      }
    }

    return null;
  }

  /**
   * @return true when the path starts with a drive letter followed by colon, e.g., "C:"
   */
  public static boolean startsWithWindowsDrive(@Nonnull String path) {
    return path.length() >= 2 && path.charAt(1) == ':' && isDriveLetter(path.charAt(0));
  }

  public static boolean isDriveLetter(char c) {
    return 'A' <= c && c <= 'Z' || 'a' <= c && c <= 'z';
  }
}