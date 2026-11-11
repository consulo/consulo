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
package consulo.util.io;

import consulo.util.io.internal.OSInfo;
import consulo.util.lang.BitUtil;
import org.intellij.lang.annotations.MagicConstant;

import org.jspecify.annotations.Nullable;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.DosFileAttributes;

/**
 * @version 11.1
 * @see FileSystemUtil#getAttributes(String)
 */
public final class FileAttributes {
  public enum Type {
    FILE,
    DIRECTORY,
    SPECIAL
  }

  public static final byte SYM_LINK = 0x01;
  public static final byte HIDDEN = 0x02;
  public static final byte READ_ONLY = 0x04;

  @MagicConstant(flags = {SYM_LINK, HIDDEN, READ_ONLY})
  public @interface Flags {
  }

  public static final FileAttributes BROKEN_SYMLINK = new FileAttributes(null, SYM_LINK, 0, 0);

  /**
   * {@code null} means unknown type - typically broken symlink.
   */
  public final @Nullable Type type;

  @Flags
  public final byte flags;

  /**
   * In bytes, 0 for special files.<br/>
   * For symlinks - length of a link target.
   */
  public final long length;

  /**
   * In milliseconds (actual resolution depends on a file system and may be less accurate).<br/>
   * For symlinks - timestamp of a link target.
   */
  public final long lastModified;

  public FileAttributes(boolean directory, boolean special, boolean symlink, boolean hidden, long length, long lastModified, boolean writable) {
    this(type(directory, special), flags(symlink, hidden, !writable), length, lastModified);
  }

  private FileAttributes(@Nullable Type type, @Flags byte flags, long length, long lastModified) {
    this.type = type;
    this.flags = flags;
    this.length = length;
    this.lastModified = lastModified;
  }

  private static Type type(boolean isDirectory, boolean isSpecial) {
    return isDirectory ? Type.DIRECTORY : isSpecial ? Type.SPECIAL : Type.FILE;
  }

  @Flags
  private static byte flags(boolean isSymlink, boolean isHidden, boolean isReadOnly) {
    @Flags byte flags = 0;
    if (isSymlink) flags |= SYM_LINK;
    if (isHidden) flags |= HIDDEN;
    if (isReadOnly) flags |= READ_ONLY;
    return flags;
  }

  public boolean isFile() {
    return type == Type.FILE;
  }

  public boolean isDirectory() {
    return type == Type.DIRECTORY;
  }

  public boolean isSpecial() {
    return type == Type.SPECIAL;
  }

  public boolean isSymLink() {
    return BitUtil.isSet(flags, SYM_LINK);
  }

  public boolean isHidden() {
    return BitUtil.isSet(flags, HIDDEN);
  }

  public boolean isWritable() {
    return !BitUtil.isSet(flags, READ_ONLY);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    FileAttributes that = (FileAttributes)o;
    if (flags != that.flags) return false;
    if (lastModified != that.lastModified) return false;
    if (length != that.length) return false;
    if (type != that.type) return false;

    return true;
  }

  @Override
  public int hashCode() {
    int result = type != null ? type.hashCode() : 0;
    result = 31 * result + flags;
    result = 31 * result + (int)(length ^ (length >>> 32));
    result = 31 * result + (int)(lastModified ^ (lastModified >>> 32));
    return result;
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();

    sb.append("[type:");
    if (type == Type.FILE) sb.append('f');
    else if (type == Type.DIRECTORY) sb.append('d');
    else if (type == Type.SPECIAL) sb.append('!');
    else sb.append('-');

    if (BitUtil.isSet(flags, SYM_LINK)) sb.append('l');
    if (BitUtil.isSet(flags, HIDDEN)) sb.append('.');

    if (BitUtil.isSet(flags, READ_ONLY)) sb.append(" ro");

    sb.append(" length:").append(length);

    sb.append(" modified:").append(lastModified);

    sb.append(']');
    return sb.toString();
  }

  public static FileAttributes createFrom(byte fileAttributesType, byte flags, long length, long lastModified) {
    Type type = fileAttributesType == -1 ? null : Type.values()[fileAttributesType];
    return new FileAttributes(type, flags, length, lastModified);
  }

  public static FileAttributes fromNio(Path path, BasicFileAttributes attrs) {
    boolean isDirectory = attrs.isDirectory();
    boolean isOther = attrs.isOther();
    Path parent = path.getParent();

    // reparse points are directories (not special files):
    boolean isSpecial = isOther && !(OSInfo.isWindows && isDirectory);
    boolean isSymbolicLink = attrs.isSymbolicLink()
                             // marking reparse points as symlinks (except roots):
                             || (OSInfo.isWindows && isOther && isDirectory && parent != null);

    if (isSymbolicLink) {
      try {
        attrs = Files.readAttributes(path, BasicFileAttributes.class);
        isDirectory = attrs.isDirectory();
      }
      catch (IOException e) {
        return BROKEN_SYMLINK;
      }
    }

    boolean isHidden = false;
    boolean isWritable = false;
    // TODO KN: We should look at the OS of a Path, not at the OS of the IDE.
    if (OSInfo.isWindows) {
      isHidden = parent != null && ((DosFileAttributes)attrs).isHidden();
      isWritable = isDirectory || !((DosFileAttributes)attrs).isReadOnly();
    }
    else {
      try {
        isWritable = isDirectory || Files.isWritable(path);
      }
      catch (SecurityException ignored) {
      }
    }

    long lastModified = attrs.lastModifiedTime().toMillis();

    return new FileAttributes(isDirectory, isSpecial, isSymbolicLink, isHidden, attrs.size(), lastModified, isWritable);
  }
}
