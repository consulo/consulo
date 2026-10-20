// Copyright 2000-2024 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.versionControlSystem.impl.internal.change.ignore;

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.TopicImpl;
import consulo.logging.Logger;
import consulo.project.Project;
import consulo.project.ProjectCoreUtil;
import consulo.versionControlSystem.AbstractVcs;
import consulo.versionControlSystem.ProjectLevelVcsManager;
import consulo.versionControlSystem.VcsMappingListener;
import consulo.versionControlSystem.util.VcsImplUtil;
import consulo.versionControlSystem.util.VcsUtil;
import consulo.virtualFileSystem.VirtualFile;
import jakarta.inject.Inject;
import org.jspecify.annotations.Nullable;

@TopicImpl(ComponentScope.PROJECT)
public class VcsIgnoreFilesChecker implements VcsMappingListener {
    private static final Logger LOG = Logger.getInstance(VcsIgnoreFilesChecker.class);

    private final Project myProject;

    @Inject
    public VcsIgnoreFilesChecker(Project project) {
        myProject = project;
    }

    @Override
    public void directoryMappingChanged() {
        ProjectLevelVcsManager.getInstance(myProject).runAfterInitialization(
            () -> myProject.getApplication().executeOnPooledThread(() -> generateVcsIgnoreFileInRootIfNeeded(myProject))
        );
    }

    /**
     * Propose to manage (generate) ignore file in project VCS root directory.
     */
    private static void generateVcsIgnoreFileInRootIfNeeded(Project project) {
        if (project.isDisposed()) {
            return;
        }
        VirtualFile projectFile = getProjectConfigDirOrProjectFile(project);
        if (projectFile == null) {
            return;
        }
        VirtualFile projectFileVcsRoot = VcsUtil.getVcsRootFor(project, projectFile);
        if (projectFileVcsRoot == null) {
            return;
        }
        AbstractVcs vcs = VcsUtil.getVcsFor(project, projectFileVcsRoot);
        if (vcs == null) {
            return;
        }
        LOG.debug("Propose manage VCS ignore in " + projectFileVcsRoot.getPath() + " for vcs " + vcs.getName());
        VcsImplUtil.proposeUpdateIgnoreFile(project, vcs, projectFileVcsRoot);
    }

    private static @Nullable VirtualFile getProjectConfigDirOrProjectFile(Project project) {
        return ProjectCoreUtil.isDirectoryBased(project) ? ProjectCoreUtil.getDirectoryStoreFile(project) : project.getProjectFile();
    }
}
