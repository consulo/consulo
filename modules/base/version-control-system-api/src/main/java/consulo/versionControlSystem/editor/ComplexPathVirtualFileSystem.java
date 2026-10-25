// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.versionControlSystem.editor;

import consulo.logging.Logger;
import consulo.project.Project;
import consulo.project.ProjectManager;
import consulo.virtualFileSystem.BaseVirtualFileSystem;
import consulo.virtualFileSystem.NonPhysicalFileSystem;
import consulo.virtualFileSystem.VirtualFile;
import consulo.virtualFileSystem.VirtualFilePathWrapper;
import org.jspecify.annotations.Nullable;

public abstract class ComplexPathVirtualFileSystem<P extends ComplexPath> extends BaseVirtualFileSystem implements NonPhysicalFileSystem {
    private static final Logger LOG = Logger.getInstance(ComplexPathVirtualFileSystem.class);

    private final ComplexPathSerializer<P> myPathSerializer;

    protected ComplexPathVirtualFileSystem(ComplexPathSerializer<P> pathSerializer) {
        myPathSerializer = pathSerializer;
    }

    protected abstract @Nullable VirtualFile findOrCreateFile(Project project, P path);

    public String getPath(P path) {
        return myPathSerializer.serialize(path);
    }

    private P getComplexPath(String path) {
        return myPathSerializer.deserialize(path);
    }

    private @Nullable P getComplexPathSafe(String path) {
        try {
            return getComplexPath(path);
        }
        catch (Exception e) {
            LOG.warn("Cannot deserialize " + path, e);
            return null;
        }
    }

    @Override
    public @Nullable VirtualFile findFileByPath(String path) {
        P parsedPath = getComplexPathSafe(path);
        if (parsedPath == null) {
            return null;
        }
        Project project = ProjectManager.getInstance().findOpenProjectByHash(parsedPath.getProjectHash());
        if (project == null) {
            return null;
        }
        return findOrCreateFile(project, parsedPath);
    }

    @Override
    public @Nullable VirtualFile refreshAndFindFileByPath(String path) {
        return findFileByPath(path);
    }

    @Override
    public String extractPresentableUrl(String path) {
        return refreshAndFindFileByPath(path) instanceof VirtualFilePathWrapper wrapper ? wrapper.getPresentablePath() : path;
    }

    @Override
    public void refresh(boolean asynchronous) {
    }
}
