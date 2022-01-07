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
package consulo.vfs.impl.mediator;

import com.intellij.openapi.util.SystemInfo;
import com.intellij.openapi.util.io.FileAttributes;
import com.intellij.openapi.util.io.FileSystemUtil;
import com.intellij.util.SystemProperties;
import com.sun.jna.Library;
import com.sun.jna.Memory;
import com.sun.jna.Native;
import com.sun.jna.Pointer;
import consulo.logging.Logger;

import javax.annotation.Nonnull;
import java.io.File;
import java.io.IOException;
import java.util.Locale;

// thanks to SVNKit for the idea of platform-specific offsets
class JnaUnixMediatorImpl implements FileSystemUtil.Mediator {
  private static final Logger LOG = Logger.getInstance(JnaUnixMediatorImpl.class);

  static final String COARSE_TIMESTAMP = "idea.io.coarse.ts";

  @SuppressWarnings({"OctalInteger", "SpellCheckingInspection"})
  private interface LibC extends Library {
    int S_MASK = 0177777;
    int S_IFMT = 0170000;
    int S_IFLNK = 0120000;  // symbolic link
    int S_IFREG = 0100000;  // regular file
    int S_IFDIR = 0040000;  // directory
    int PERM_MASK = 0777;
    int EXECUTE_MASK = 0111;
    int WRITE_MASK = 0222;
    int W_OK = 2;           // write permission flag for access(2)

    int getuid();

    int getgid();

    int lstat(String path, Pointer stat);

    int stat(String path, Pointer stat);

    int __lxstat64(int ver, String path, Pointer stat);

    int __xstat64(int ver, String path, Pointer stat);

    int chmod(String path, int mode);

    int access(String path, int mode);
  }

  private static final int[] LINUX_32 = {16, 44, 72, 24, 28};
  private static final int[] LINUX_64 = {24, 48, 88, 28, 32};
  private static final int[] LNX_PPC32 = {16, 48, 80, 24, 28};
  private static final int[] LNX_PPC64 = LINUX_64;
  private static final int[] LINUX_ARM = LNX_PPC32;
  private static final int[] BSD_32 = {8, 48, 32, 12, 16};
  private static final int[] BSD_64 = {8, 72, 40, 12, 16};
  private static final int[] SUN_OS_32 = {20, 48, 64, 28, 32};
  private static final int[] SUN_OS_64 = {16, 40, 64, 24, 28};

  private static final int STAT_VER = 1;
  private static final int OFF_MODE = 0;
  private static final int OFF_SIZE = 1;
  private static final int OFF_TIME = 2;
  private static final int OFF_UID = 3;
  private static final int OFF_GID = 4;

  private final LibC myLibC;
  private final int[] myOffsets;
  private final int myUid;
  private final int myGid;
  private final boolean myCoarseTs = SystemProperties.getBooleanProperty(COARSE_TIMESTAMP, false);

  JnaUnixMediatorImpl() throws Exception {
    if (SystemInfo.isLinux) {
      if ("arm".equals(SystemInfo.OS_ARCH)) {
        if (SystemInfo.is32Bit) {
          myOffsets = LINUX_ARM;
        }
        else {
          throw new IllegalStateException("AArch64 architecture is not supported");
        }
      }
      else if ("ppc".equals(SystemInfo.OS_ARCH)) {
        myOffsets = SystemInfo.is32Bit ? LNX_PPC32 : LNX_PPC64;
      }
      else {
        myOffsets = SystemInfo.is32Bit ? LINUX_32 : LINUX_64;
      }
    }
    else if (SystemInfo.isMac | SystemInfo.isFreeBSD) {
      myOffsets = SystemInfo.is32Bit ? BSD_32 : BSD_64;
    }
    else if (SystemInfo.isSolaris) {
      myOffsets = SystemInfo.is32Bit ? SUN_OS_32 : SUN_OS_64;
    }
    else {
      throw new IllegalStateException("Unsupported OS/arch: " + SystemInfo.OS_NAME + "/" + SystemInfo.OS_ARCH);
    }

    myLibC = (LibC)Native.loadLibrary("c", LibC.class);
    myUid = myLibC.getuid();
    myGid = myLibC.getgid();
  }

  @Override
  public FileAttributes getAttributes(@Nonnull String path) {
    Memory buffer = new Memory(256);
    int res = SystemInfo.isLinux ? myLibC.__lxstat64(STAT_VER, path, buffer) : myLibC.lstat(path, buffer);
    if (res != 0) return null;

    int mode = getModeFlags(buffer) & LibC.S_MASK;
    boolean isSymlink = (mode & LibC.S_IFMT) == LibC.S_IFLNK;
    if (isSymlink) {
      if (!loadFileStatus(path, buffer)) {
        return FileAttributes.BROKEN_SYMLINK;
      }
      mode = getModeFlags(buffer) & LibC.S_MASK;
    }

    boolean isDirectory = (mode & LibC.S_IFMT) == LibC.S_IFDIR;
    boolean isSpecial = !isDirectory && (mode & LibC.S_IFMT) != LibC.S_IFREG;
    long size = buffer.getLong(myOffsets[OFF_SIZE]);
    long mTime1 = SystemInfo.is32Bit ? buffer.getInt(myOffsets[OFF_TIME]) : buffer.getLong(myOffsets[OFF_TIME]);
    long mTime2 = myCoarseTs ? 0 : SystemInfo.is32Bit ? buffer.getInt(myOffsets[OFF_TIME] + 4) : buffer.getLong(myOffsets[OFF_TIME] + 8);
    long mTime = mTime1 * 1000 + mTime2 / 1000000;

    boolean writable = ownFile(buffer) ? (mode & LibC.WRITE_MASK) != 0 : myLibC.access(path, LibC.W_OK) == 0;

    return new FileAttributes(isDirectory, isSpecial, isSymlink, false, size, mTime, writable);
  }

  private boolean loadFileStatus(@Nonnull String path, Memory buffer) {
    return (SystemInfo.isLinux ? myLibC.__xstat64(STAT_VER, path, buffer) : myLibC.stat(path, buffer)) == 0;
  }

  @Override
  public String resolveSymLink(@Nonnull final String path) throws IOException {
    try {
      return new File(path).getCanonicalPath();
    }
    catch (IOException e) {
      String message = e.getMessage();
      if (message != null && message.toLowerCase(Locale.US).contains("too many levels of symbolic links")) {
        LOG.debug(e);
        return null;
      }
      throw new IOException("Cannot resolve '" + path + "'", e);
    }
  }

  @Override
  public boolean clonePermissions(@Nonnull String source, @Nonnull String target, boolean onlyPermissionsToExecute) {
    Memory buffer = new Memory(256);
    if (!loadFileStatus(source, buffer)) return false;

    int permissions;
    int sourcePermissions = getModeFlags(buffer) & LibC.PERM_MASK;
    if (onlyPermissionsToExecute) {
      if (!loadFileStatus(target, buffer)) return false;
      int targetPermissions = getModeFlags(buffer) & LibC.PERM_MASK;
      permissions = targetPermissions & ~LibC.EXECUTE_MASK | sourcePermissions & LibC.EXECUTE_MASK;
    }
    else {
      permissions = sourcePermissions;
    }
    return myLibC.chmod(target, permissions) == 0;
  }

  private int getModeFlags(Memory buffer) {
    return SystemInfo.isLinux ? buffer.getInt(myOffsets[OFF_MODE]) : buffer.getShort(myOffsets[OFF_MODE]);
  }

  private boolean ownFile(Memory buffer) {
    return buffer.getInt(myOffsets[OFF_UID]) == myUid && buffer.getInt(myOffsets[OFF_GID]) == myGid;
  }
}