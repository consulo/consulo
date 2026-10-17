/*
 * Copyright 2000-2017 JetBrains s.r.o.
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

import consulo.ui.annotation.RequiredUIAccess;
import consulo.util.dataholder.Key;
import consulo.versionControlSystem.distributed.repository.Repository;
import org.jspecify.annotations.Nullable;

import java.util.Collection;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;

public interface VcsPushUi {
    Key<VcsPushUi> VCS_PUSH_DIALOG = Key.create("VcsPushDialog");

    /**
     * Runs {@link PrePushHandler pre-push handlers} under a modal progress,
     * if they succeed, starts the given background activity, and closes the push dialog.
     */
    @RequiredUIAccess
    void executeAfterRunningPrePushHandlers(Supplier<CompletableFuture<?>> activity);

    /**
     * Runs {@link PrePushHandler pre-push handlers} under a modal progress,
     * and after that starts push in a background task.
     */
    @RequiredUIAccess
    void push(boolean forcePush);

    /**
     * Returns push specifications (what is being pushed, where from and where to) collected from the push dialog,
     * grouped per PushSupports, which means in fact per-VCS.
     * <br/><br/>
     * Although no type specification is given, it is guaranteed that in the returned Map
     * each PushSupport is the one which corresponds to its RepoPushSpecs (all of which are of course of same types).
     */
    Map<PushSupport<Repository, PushSource, PushTarget>, Collection<PushInfo>> getSelectedPushSpecs();

    /**
     * Checks if push is available right now for selected repositories and their targets.
     * <br/><br/>
     * E.g. push is not allowed, when a target is being edited, or when a repository without any remotes is selected.
     */
    @RequiredUIAccess
    boolean canPush();

    @RequiredUIAccess
    default boolean hasWarnings() {
        return false;
    }

    /**
     * Returns special push options, usually selected by user at the bottom of the push dialog.
     */
    @Nullable
    VcsPushOptionValue getAdditionalOptionValue(PushSupport support);
}
