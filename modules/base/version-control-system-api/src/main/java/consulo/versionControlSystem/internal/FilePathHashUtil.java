// Copyright 2000-2023 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.versionControlSystem.internal;

import consulo.util.lang.StringUtil;
import jakarta.annotation.Nonnull;

public class FilePathHashUtil {
  public static int pathHashCode(boolean caseSensitive, @Nonnull String path) {
    return pathHashCode(caseSensitive, path, 0, path.length(), 0);
  }

  public static int pathHashCode(boolean caseSensitive, @Nonnull String path, int offset1, int offset2, int prefixHash) {
    if (caseSensitive) {
      return StringUtil.stringHashCode(path, offset1, offset2, prefixHash);
    }
    else {
      return StringUtil.stringHashCodeInsensitive(path, offset1, offset2, prefixHash);
    }
  }
}
