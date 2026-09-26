// Copyright 2000-2024 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.builtinWebServer.webServer;

import consulo.virtualFileSystem.VirtualFile;
import org.jspecify.annotations.Nullable;

public interface FileResolver {
    @Nullable PathInfo resolve(String path, VirtualFile root, @Nullable String moduleName, boolean isLibrary, PathQuery pathQuery);

    default @Nullable PathInfo resolve(String path, VirtualFile root, PathQuery pathQuery) {
        return resolve(path, root, null, false, pathQuery);
    }
}
