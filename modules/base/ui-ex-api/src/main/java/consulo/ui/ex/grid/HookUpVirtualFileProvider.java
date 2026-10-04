// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.ui.ex.grid;

import consulo.virtualFileSystem.VirtualFile;
import org.jspecify.annotations.Nullable;

/**
 * A data source which shows the content of a file.
 */
public interface HookUpVirtualFileProvider {
    @Nullable
    VirtualFile getVirtualFile();
}
