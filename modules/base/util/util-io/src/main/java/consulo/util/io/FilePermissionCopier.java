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
package consulo.util.io;

import consulo.util.io.internal.OSInfo;
import jakarta.annotation.Nonnull;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.PosixFileAttributes;
import java.nio.file.attribute.PosixFilePermission;
import java.util.EnumSet;
import java.util.Set;

/**
 * @author VISTALL
 * @since 26-Feb-22
 */
public interface FilePermissionCopier {
  static FilePermissionCopier DISABLED = (source, target, execOnly) -> true;

  static FilePermissionCopier BY_NIO2 = (source, target, execOnly) -> {
    if (!OSInfo.isUnix) return false;

    PosixFilePermission[] execPermissions = {PosixFilePermission.OWNER_EXECUTE, PosixFilePermission.GROUP_EXECUTE, PosixFilePermission.OTHERS_EXECUTE};

    Path sourcePath = Paths.get(source), targetPath = Paths.get(target);
    Set<PosixFilePermission> sourcePermissions = Files.readAttributes(sourcePath, PosixFileAttributes.class).permissions();
    Set<PosixFilePermission> targetPermissions = Files.readAttributes(targetPath, PosixFileAttributes.class).permissions();
    Set<PosixFilePermission> newPermissions;
    if (execOnly) {
      newPermissions = EnumSet.copyOf(targetPermissions);
      for (PosixFilePermission permission : execPermissions) {
        if (sourcePermissions.contains(permission)) {
          newPermissions.add(permission);
        }
        else {
          newPermissions.remove(permission);
        }
      }
    }
    else {
      newPermissions = sourcePermissions;
    }
    Files.setAttribute(targetPath, "posix:permissions", newPermissions);
    return true;
  };

  boolean clonePermissions(@Nonnull String source, @Nonnull String target, boolean execOnly) throws IOException;
}
