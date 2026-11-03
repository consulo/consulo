/*
 * Copyright 2000-2014 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package consulo.versionControlSystem.distributed.push;

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ExtensionAPI;
import consulo.localize.LocalizeValue;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.versionControlSystem.AbstractVcs;
import consulo.versionControlSystem.distributed.repository.Repository;
import consulo.versionControlSystem.distributed.repository.RepositoryManager;
import org.jspecify.annotations.Nullable;

import java.util.Collection;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Base class to provide vcs-specific info
 */
@ExtensionAPI(ComponentScope.PROJECT)
public abstract class PushSupport<Repo extends Repository, Source extends PushSource, Target extends PushTarget> {
    public abstract AbstractVcs getVcs();

    public abstract Pusher<Repo, Source, Target> getPusher();

    public abstract OutgoingCommitsProvider<Repo, Source, Target> getOutgoingCommitsProvider();

    public boolean canBePushed(Repo repository, Source source, Target target) {
        return true;
    }

    /**
     * @return Default push destination
     */
    public abstract @Nullable Target getDefaultTarget(Repo repository);

    /**
     * @return Push destination for source
     */
    public @Nullable Target getDefaultTarget(Repo repository, Source source) {
        return null;
    }

    /**
     * @return current source(branch) for repository
     */
    public abstract @Nullable Source getSource(Repo repository);

    /**
     * @return RepositoryManager for vcs
     */
    public abstract RepositoryManager<Repo> getRepositoryManager();

    @RequiredUIAccess
    public @Nullable VcsPushOptionsPanel createOptionsPanel() {
        return null;
    }

    @RequiredUIAccess
    public abstract PushTargetPanel<Target> createTargetPanel(Repo repository, Source source, @Nullable Target defaultTarget);

    public boolean shouldRequestIncomingChangesForNotCheckedRepositories() {
        return true;
    }

    /**
     * Returns true if force push is allowed now in the selected repository.
     * <p/>
     * Force push might depend on the branch user is pushing to.
     */
    public abstract boolean isForcePushAllowed(Repo repo, Target target);

    public abstract boolean isSilentForcePushAllowed(Target target);

    public LocalizeValue getForcePushConfigurablePath() {
        return LocalizeValue.empty();
    }

    public abstract void saveSilentForcePushTarget(Target target);

    /**
     * @return force push types, which will be shown as separate actions in push dialog (empty list - no force push actions).
     * Used only when repositories of single vcs are selected, otherwise {@link ForcePushType#DEFAULT} is used
     */
    public List<ForcePushType> getForcePushTypes() {
        return List.of(ForcePushType.DEFAULT);
    }

    /**
     * Called after force push confirmation, before push started. Can ask additional info from user.
     *
     * @return future with type which will be passed to {@link Pusher} (can be same as {@code type}, or type with additional info),
     * or with null if push must be cancelled
     */
    @RequiredUIAccess
    public CompletableFuture<@Nullable ForcePushType> beforeForcePush(ForcePushType type, Collection<PushInfo> pushInfos) {
        return CompletableFuture.completedFuture(type);
    }

    public boolean mayChangeTargetsSync() {
        return false;
    }
}
