// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.mcpServer.skill;

import consulo.project.Project;
import org.jspecify.annotations.Nullable;

import java.nio.file.Path;

/**
 * The normalized agent working directory and the open IDE project at that directory, if any.
 */
public final class SkillWorkspaceContext {
    private final Path myWorkingDirectory;
    private final @Nullable Project myProject;

    private SkillWorkspaceContext(Path workingDirectory, @Nullable Project project) {
        myWorkingDirectory = workingDirectory;
        myProject = project;
    }

    public static SkillWorkspaceContext create(Path workingDirectory, @Nullable Project project) {
        Path normalizedPath = workingDirectory.toAbsolutePath().normalize();
        Project openProject = project != null && project.isOpen() && !project.isDisposed() && !project.isDefault() ? project : null;
        String basePath = openProject == null ? null : openProject.getBasePath();
        Path projectPath = basePath == null ? null : Path.of(basePath).toAbsolutePath().normalize();
        return new SkillWorkspaceContext(normalizedPath, normalizedPath.equals(projectPath) ? openProject : null);
    }

    public Path getWorkingDirectory() {
        return myWorkingDirectory;
    }

    /**
     * An open IDE project whose normalized base path equals {@link #getWorkingDirectory()}.
     */
    public @Nullable Project getProject() {
        return myProject;
    }
}
