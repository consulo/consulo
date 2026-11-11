// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.virtualFileSystem.internal;

import consulo.util.io.FileAttributes;
import org.jspecify.annotations.Nullable;

public interface VirtualFileWithAttributes {
    @Nullable FileAttributes getAttributes();
}
