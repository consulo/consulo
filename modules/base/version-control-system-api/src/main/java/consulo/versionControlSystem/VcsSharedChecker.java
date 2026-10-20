// Copyright 2000-2025 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.versionControlSystem;

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ExtensionAPI;
import consulo.project.Project;

import java.nio.file.Path;

@ExtensionAPI(ComponentScope.APPLICATION)
public interface VcsSharedChecker {
    VcsKey getSupportedVcs();

    /**
     * Check if a {@code path} is shared under {@link #getSupportedVcs()} VCS.
     * In compare to {@link consulo.versionControlSystem.util.VcsImplUtil#isProjectSharedInVcs}, execute specific "native" check
     * in the corresponding VCS.
     *
     * @param path The path (e.g., directory) to check
     */
    boolean isPathSharedInVcs(Project project, Path path) throws VcsException;
}
