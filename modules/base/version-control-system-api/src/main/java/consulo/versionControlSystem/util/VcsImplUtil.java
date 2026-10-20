// Copyright 2000-2025 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.versionControlSystem.util;

import consulo.logging.Logger;
import consulo.project.Project;
import consulo.versionControlSystem.AbstractVcs;
import consulo.versionControlSystem.VcsKey;
import consulo.versionControlSystem.change.ChangeListManager;
import consulo.versionControlSystem.change.IgnoredFileContentProvider;
import consulo.versionControlSystem.change.IgnoredFileGenerator;
import consulo.virtualFileSystem.LocalFileSystem;
import consulo.virtualFileSystem.VirtualFile;
import consulo.virtualFileSystem.status.FileStatus;
import org.jspecify.annotations.Nullable;

import java.util.function.Supplier;

/**
 * <p>{@link VcsUtil} extension that needs access to the {@code intellij.platform.vcs.impl} module.</p>
 */
public final class VcsImplUtil {
    private static final Logger LOG = Logger.getInstance(VcsImplUtil.class);

    public static @Nullable IgnoredFileContentProvider findIgnoredFileContentProvider(AbstractVcs vcs) {
        return findIgnoredFileContentProvider(vcs.getProject(), vcs.getKeyInstanceMethod());
    }

    public static @Nullable IgnoredFileContentProvider findIgnoredFileContentProvider(Project project, VcsKey vcsKey) {
        IgnoredFileContentProvider ignoreContentProvider =
            project.getExtensionPoint(IgnoredFileContentProvider.class).findFirstSafe(provider -> provider.getSupportedVcs().equals(vcsKey));

        if (ignoreContentProvider == null) {
            LOG.debug("Cannot get ignore content provider for vcs " + vcsKey.getName());
            return null;
        }
        return ignoreContentProvider;
    }

    public static void proposeUpdateIgnoreFile(Project project, AbstractVcs vcs, VirtualFile ignoreFileRoot) {
        generateIgnoreFile(project, vcs, ignoreFileRoot, true);
    }

    public static void generateIgnoreFileIfNeeded(Project project, AbstractVcs vcs, VirtualFile ignoreFileRoot) {
        generateIgnoreFile(project, vcs, ignoreFileRoot, false);
    }

    private static void generateIgnoreFile(Project project, AbstractVcs vcs, VirtualFile ignoreFileRoot, boolean notify) {
        IgnoredFileGenerator ignoredFileGenerator = project.getInstance(IgnoredFileGenerator.class);
        ignoredFileGenerator.generateFile(ignoreFileRoot, vcs, notify);
    }

    private static boolean isFileSharedInVcs(Project project, ChangeListManager changeListManager, String filePath) {
        VirtualFile file = LocalFileSystem.getInstance().findFileByPath(filePath);
        if (file == null) {
            return false;
        }
        FileStatus fileStatus = changeListManager.getStatus(file);
        return VcsUtil.isFileUnderVcs(project, filePath) && (fileStatus != FileStatus.UNKNOWN && fileStatus != FileStatus.IGNORED);
    }

    public static boolean isProjectSharedInVcs(Project project) {
        return project.getApplication().runReadAction((Supplier<Boolean>) () -> {
            if (project.isDisposed()) {
                return false;
            }
            String projectFilePath = project.getProjectFilePath();
            ChangeListManager changeListManager = ChangeListManager.getInstance(project);
            return !changeListManager.isInUpdate()
                && (projectFilePath != null && isFileSharedInVcs(project, changeListManager, projectFilePath));
        });
    }

    private VcsImplUtil() {
    }
}
