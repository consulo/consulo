// Copyright 2000-2023 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.virtualFileSystem.internal;

import consulo.virtualFileSystem.fileType.FileType;
import org.jspecify.annotations.Nullable;

/**
 * A way to opt-out from file type detection logic
 */
public interface VirtualFileWithAssignedFileType {
    @Nullable FileType getAssignedFileType();
}
