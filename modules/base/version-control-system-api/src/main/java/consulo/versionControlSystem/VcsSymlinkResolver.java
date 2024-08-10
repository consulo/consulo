// Copyright 2000-2022 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.versionControlSystem;

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ExtensionAPI;
import consulo.component.extension.ExtensionPointName;
import consulo.virtualFileSystem.VirtualFile;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

@ExtensionAPI(ComponentScope.PROJECT)
public interface VcsSymlinkResolver {
    ExtensionPointName<VcsSymlinkResolver> EP_NAME = new ExtensionPointName<>(VcsSymlinkResolver.class);

    boolean isEnabled();

    @Nullable
    VirtualFile resolveSymlink(@Nonnull VirtualFile file);
}
