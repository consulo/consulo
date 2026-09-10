// Copyright 2000-2023 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.language.index.impl.internal.dependencies;

import consulo.virtualFileSystem.VirtualFile;

public interface IndexingRequestToken {
    FileIndexingStamp getFileIndexingStamp(VirtualFile file);
}
