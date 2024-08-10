// Copyright 2000-2022 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.versionControlSystem.impl.internal;

import consulo.annotation.component.ExtensionImpl;
import consulo.application.util.registry.Registry;
import consulo.application.util.registry.RegistryValue;
import consulo.project.Project;
import consulo.versionControlSystem.ProjectLevelVcsManager;
import consulo.versionControlSystem.VcsSymlinkResolver;
import consulo.virtualFileSystem.VirtualFile;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import jakarta.inject.Inject;

@ExtensionImpl(order = "last")
public class DefaultVcsSymlinkResolver implements VcsSymlinkResolver {
    private enum Mode {
        FORCE_TARGET,
        PREFER_TARGET,
        FALLBACK_TARGET,
        DISABLED
    }

    private final Project myProject;
    private final Mode myMode;

    @Inject
    public DefaultVcsSymlinkResolver(@Nonnull Project project) {
        myProject = project;
        RegistryValue value = Registry.get("vcs.resolve.symlinks.for.vcs.operations");
        myMode = switch (value.asString()) {
            case "force_target" -> Mode.FORCE_TARGET;
            case "prefer_target" -> Mode.PREFER_TARGET;
            case "fallback_target" -> Mode.FALLBACK_TARGET;
            default -> Mode.DISABLED;
        };
    }

    @Override
    public boolean isEnabled() {
        return myMode != Mode.DISABLED;
    }

    @Override
    @Nullable
    public VirtualFile resolveSymlink(@Nonnull VirtualFile file) {
        if (myMode == Mode.DISABLED) return file;

        VirtualFile canonicalFile = file.getCanonicalFile();
        if (canonicalFile == null || file.equals(canonicalFile)) {
            return null;
        }

        if (myMode == Mode.FORCE_TARGET) {
            return canonicalFile;
        }

        if (myMode == Mode.PREFER_TARGET) {
            if (ProjectLevelVcsManager.getInstance(myProject).getVcsFor(canonicalFile) != null) {
                return canonicalFile;
            }
        }
        else if (myMode == Mode.FALLBACK_TARGET) {
            if (ProjectLevelVcsManager.getInstance(myProject).getVcsFor(file) == null &&
                ProjectLevelVcsManager.getInstance(myProject).getVcsFor(canonicalFile) != null) {
                return canonicalFile;
            }
        }
        return null;
    }
}
