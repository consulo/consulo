/*
 * Copyright 2000-2012 JetBrains s.r.o.
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
package consulo.virtualFileSystem;

import consulo.annotation.DeprecationInfo;
import consulo.util.io.URLUtil;
import consulo.util.lang.lazy.LazyValue;
import consulo.virtualFileSystem.archive.ArchiveFileSystem;

import javax.annotation.Nonnull;
import java.util.function.Supplier;

/**
 * @author yole
 */
public class StandardFileSystems {
  public static final String FILE_PROTOCOL = URLUtil.FILE_PROTOCOL;
  public static final String FILE_PROTOCOL_PREFIX = FILE_PROTOCOL + URLUtil.SCHEME_SEPARATOR;

  @Deprecated
  @DeprecationInfo("platform don't known about jar file system")
  public static String JAR_PROTOCOL = "jar";
  @Deprecated
  @DeprecationInfo("platform don't known about jar file system")
  public static String JAR_PROTOCOL_PREFIX = "jar://";

  public static final String ZIP_PROTOCOL = "zip";
  public static final String ZIP_PROTOCOL_PREFIX = ZIP_PROTOCOL + URLUtil.SCHEME_SEPARATOR;

  @Deprecated
  public static String JAR_SEPARATOR = "!/";
  @Deprecated
  @DeprecationInfo("use com.intellij.util.io.URLUtil#HTTP_PROTOCOL")
  public static final String HTTP_PROTOCOL = URLUtil.HTTP_PROTOCOL;

  private static final Supplier<VirtualFileSystem> ourLocal = LazyValue.notNull(() -> {
    return VirtualFileManager.getInstance().getFileSystem(FILE_PROTOCOL);
  });

  private static final Supplier<VirtualFileSystem> ourZip = LazyValue.notNull(() -> {
    return VirtualFileManager.getInstance().getFileSystem(ZIP_PROTOCOL);
  });

  @Nonnull
  public static VirtualFileSystem local() {
    return ourLocal.get();
  }

  /**
   * @deprecated use JarArchiveFileType.INSTANCE.getFileSystem()
   */
  @Deprecated
  @DeprecationInfo("use JarArchiveFileType.INSTANCE.getFileSystem()")
  public static ArchiveFileSystem jar() {
    return zip();
  }

  @Nonnull
  public static ArchiveFileSystem zip() {
    return (ArchiveFileSystem)ourZip.get();
  }
}
