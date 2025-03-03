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
package consulo.ide.impl.idea.dvcs.push;

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ServiceAPI;
import consulo.annotation.component.ServiceImpl;
import consulo.component.persist.PersistentStateComponent;
import consulo.component.persist.State;
import consulo.component.persist.Storage;
import consulo.component.persist.StoragePathMacros;
import consulo.util.collection.ContainerUtil;
import consulo.util.xml.serializer.annotation.AbstractCollection;
import consulo.util.xml.serializer.annotation.Attribute;
import consulo.util.xml.serializer.annotation.Tag;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import jakarta.inject.Singleton;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Singleton
@State(name = "Push.Settings", storages = {@Storage(file = StoragePathMacros.WORKSPACE_FILE)})
@ServiceAPI(ComponentScope.PROJECT)
@ServiceImpl
public class PushSettings implements PersistentStateComponent<PushSettings.State> {
    private State myState = new State();

    public static class State {
        @Tag("excluded-roots")
        @AbstractCollection(surroundWithTag = false, elementTag = "path")
        public Set<String> EXCLUDED_ROOTS = new HashSet<>();
        @AbstractCollection(surroundWithTag = false)
        @Tag("force-push-targets")
        public List<ForcePushTargetInfo> FORCE_PUSH_TARGETS = new ArrayList<>();
    }

    @Nullable
    @Override
    public State getState() {
        return myState;
    }

    @Override
    public void loadState(State state) {
        myState = state;
    }

    @Nonnull
    public Set<String> getExcludedRepoRoots() {
        return myState.EXCLUDED_ROOTS;
    }

    public void saveExcludedRepoRoots(@Nonnull Set<String> roots) {
        myState.EXCLUDED_ROOTS = roots;
    }


    public boolean containsForcePushTarget(@Nonnull final String remote, @Nonnull final String branch) {
        return ContainerUtil.exists(
            myState.FORCE_PUSH_TARGETS,
            info -> info.targetRemoteName.equals(remote) && info.targetBranchName.equals(branch)
        );
    }

    public void addForcePushTarget(@Nonnull String targetRemote, @Nonnull String targetBranch) {
        List<ForcePushTargetInfo> targets = myState.FORCE_PUSH_TARGETS;
        if (!containsForcePushTarget(targetRemote, targetBranch)) {
            targets.add(new ForcePushTargetInfo(targetRemote, targetBranch));
            myState.FORCE_PUSH_TARGETS = targets;
        }
    }

    @Tag("force-push-target")
    private static class ForcePushTargetInfo {
        @Attribute(value = "remote-path")
        public String targetRemoteName;
        @Attribute(value = "branch")
        public String targetBranchName;

        @SuppressWarnings("unused")
        ForcePushTargetInfo() {
            this("", "");
        }

        ForcePushTargetInfo(@Nonnull String targetRemote, @Nonnull String targetBranch) {
            targetRemoteName = targetRemote;
            targetBranchName = targetBranch;
        }
    }
}

