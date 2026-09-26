// Copyright 2000-2021 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.builtinWebServer.impl.webServer;

import consulo.annotation.component.ExtensionImpl;
import consulo.application.ReadAction;
import consulo.builtinWebServer.webServer.FileResolver;
import consulo.builtinWebServer.webServer.PathInfo;
import consulo.builtinWebServer.webServer.PathQuery;
import consulo.builtinWebServer.webServer.WebServerPathToFileManager;
import consulo.builtinWebServer.webServer.WebServerRootsProvider;
import consulo.module.content.ProjectFileIndex;
import consulo.project.Project;
import consulo.virtualFileSystem.VirtualFile;
import jakarta.inject.Inject;
import org.jspecify.annotations.Nullable;

@ExtensionImpl(id = "default")
public final class DefaultWebServerRootsProvider extends WebServerRootsProvider {
    private final Project myProject;
    private final WebServerPathToFileManager myPathToFileManager;

    @Inject
    public DefaultWebServerRootsProvider(Project project, WebServerPathToFileManager pathToFileManager) {
        myProject = project;
        myPathToFileManager = pathToFileManager;
    }

    @Override
    public @Nullable PathInfo resolve(String path, PathQuery pathQuery) {
        if (pathQuery.isUseVfs()) {
            int slashIndex = path.indexOf('/');
            String oldestParent = slashIndex > 0 ? path.substring(0, slashIndex) : null;
            if (oldestParent == null && !path.isEmpty() && path.indexOf('.') < 0) {
                oldestParent = path;
            }

            if (oldestParent != null) {
                WebServerPathToFileManagerImpl pathToFileManager = (WebServerPathToFileManagerImpl) myPathToFileManager;
                for (SuitableRoot suitableRoot : pathToFileManager.getParentToSuitableRoot().getUnchecked(oldestParent)) {
                    VirtualFile root = suitableRoot.file();
                    if (!root.isValid()) {
                        continue;
                    }

                    VirtualFile file = root.findFileByRelativePath(path);
                    if (file != null) {
                        return new PathInfo(null, file, root, suitableRoot.moduleQualifier());
                    }
                }
            }
            return null;
        }

        FileResolver resolver = myPathToFileManager.getResolver(path);
        for (VirtualFile root : RootProvider.getAllRoots(myProject)) {
            if (!root.isValid()) {
                continue;
            }

            PathInfo pathInfo = resolver.resolve(path, root, pathQuery);
            if (pathInfo != null) {
                return pathInfo;
            }
        }
        return null;
    }

    @Override
    public @Nullable PathInfo getPathInfo(VirtualFile file) {
        return ReadAction.compute(() -> {
            ProjectFileIndex fileIndex = ProjectFileIndex.getInstance(myProject);
            if (!fileIndex.isInContent(file) && !fileIndex.isExcluded(file)) {
                return null;
            }

            VirtualFile root = fileIndex.getSourceRootForFile(file);
            boolean isRootNameOptionalInPath;
            if (root == null) {
                isRootNameOptionalInPath = false;
                root = fileIndex.getContentRootForFile(file, false);
                if (root == null) {
                    return null;
                }
            }
            else {
                isRootNameOptionalInPath = true;
            }

            return new PathInfo(null, file, root, null, false, isRootNameOptionalInPath);
        });
    }
}
