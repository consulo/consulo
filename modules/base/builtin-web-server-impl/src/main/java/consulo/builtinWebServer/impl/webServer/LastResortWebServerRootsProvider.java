// Copyright 2000-2024 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.builtinWebServer.impl.webServer;

import consulo.annotation.component.ExtensionImpl;
import consulo.builtinWebServer.webServer.PathInfo;
import consulo.builtinWebServer.webServer.PathQuery;
import consulo.builtinWebServer.webServer.WebServerRootsProvider;
import consulo.project.Project;
import consulo.virtualFileSystem.VirtualFile;
import consulo.virtualFileSystem.util.VirtualFileUtil;
import jakarta.inject.Inject;
import org.jspecify.annotations.Nullable;

@ExtensionImpl(id = "lastResort", order = "last")
public final class LastResortWebServerRootsProvider extends WebServerRootsProvider {
    private final Project myProject;

    @Inject
    public LastResortWebServerRootsProvider(Project project) {
        myProject = project;
    }

    @Override
    public @Nullable PathInfo resolve(String path, PathQuery pathQuery) {
        VirtualFile baseDir = myProject.getBaseDir();
        if (baseDir == null) {
            return null;
        }

        VirtualFile file = baseDir.findFileByRelativePath(path);
        return file == null ? null : new PathInfo(null, file, baseDir);
    }

    @Override
    public @Nullable PathInfo getPathInfo(VirtualFile file) {
        VirtualFile baseDir = myProject.getBaseDir();
        if (baseDir == null || !VirtualFileUtil.isAncestor(baseDir, file, false)) {
            return null;
        }
        return new PathInfo(null, file, baseDir);
    }
}
