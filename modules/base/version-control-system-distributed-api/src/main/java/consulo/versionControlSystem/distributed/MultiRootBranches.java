// Copyright 2000-2025 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.versionControlSystem.distributed;

import consulo.versionControlSystem.distributed.branch.DvcsSyncSettings;
import consulo.versionControlSystem.distributed.repository.Repository;
import org.jspecify.annotations.Nullable;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.function.Function;

public final class MultiRootBranches {
    private MultiRootBranches() {
    }

    public static <T> @Nullable String getCommonName(Collection<T> repositories, Function<T, @Nullable String> nameSupplier) {
        Set<@Nullable String> names = new LinkedHashSet<>();
        for (T repository : repositories) {
            names.add(nameSupplier.apply(repository));
        }
        return names.size() == 1 ? names.iterator().next() : null;
    }

    /**
     * For the given repositories, returns the name of the current branch if it is the same for all repositories
     * (AKA "all repositories are on the same branch"), and {@code null} if they are on different branches,
     * in particular, if any of repositories is in the detached HEAD.
     */
    public static @Nullable String getCommonCurrentBranch(Collection<? extends Repository> repositories) {
        return getCommonName(repositories, Repository::getCurrentBranchName);
    }

    public static boolean diverged(Collection<? extends Repository> repositories) {
        return getCommonCurrentBranch(repositories) == null;
    }

    public static boolean isSyncOptionEnabled(DvcsSyncSettings settings) {
        return settings.getSyncSetting() == DvcsSyncSettings.Value.SYNC;
    }
}
