// Copyright 2000-2025 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.versionControlSystem.util;

import consulo.logging.Logger;
import consulo.project.Project;
import consulo.versionControlSystem.AbstractVcs;
import consulo.versionControlSystem.VcsKey;
import consulo.versionControlSystem.change.IgnoredFileContentProvider;
import org.jspecify.annotations.Nullable;

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

    private VcsImplUtil() {
    }
}
