// Copyright 2000-2021 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.builtinWebServer.webServer;

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ExtensionAPI;
import consulo.virtualFileSystem.VirtualFile;
import org.jspecify.annotations.Nullable;

@ExtensionAPI(ComponentScope.PROJECT)
public abstract class WebServerRootsProvider {
    public abstract @Nullable PathInfo resolve(String path, PathQuery pathQuery);

    public abstract @Nullable PathInfo getPathInfo(VirtualFile file);

    public boolean isClearCacheOnFileContentChanged(VirtualFile file) {
        return false;
    }
}
