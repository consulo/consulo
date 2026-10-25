// Copyright 2000-2024 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.versionControlSystem.change;

import consulo.versionControlSystem.FilePath;
import consulo.virtualFileSystem.status.FileStatus;
import org.jspecify.annotations.Nullable;

public interface PresentableChange {
    FilePath getFilePath();

    FileStatus getFileStatus();

    default @Nullable Object getTag() {
        return null;
    }
}
