/*
 * Copyright 2013-2026 consulo.io
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
package consulo.execution.configuration;

import consulo.content.platform.PlatformTarget;
import consulo.content.platform.PlatformTargetManager;
import consulo.execution.configuration.fragment.FragmentStateHolder;
import consulo.execution.configuration.fragment.PlatformTargetFragmentState;
import consulo.platform.Platform;
import consulo.process.ExecutionException;
import consulo.project.localize.ProjectLocalize;
import org.jspecify.annotations.Nullable;

/**
 * A run profile which can run on another machine. The chosen machine is a {@link PlatformTarget} id, stored with the
 * profile; null runs it locally.
 */
public interface PlatformAwareRunProfile extends RunProfile, FragmentStateHolder {
    default @Nullable String getPlatformTargetId() {
        return getFragmentState(PlatformTargetFragmentState.class).getTargetId();
    }

    default void setPlatformTargetId(@Nullable String targetId) {
        getFragmentState(PlatformTargetFragmentState.class).setTargetId(targetId);
    }

    /**
     * The platform to run on. The execution manager connects the target before a run starts; a target that is still
     * not reachable here fails the run.
     */
    default Platform resolvePlatform(PlatformTargetManager manager) throws ExecutionException {
        String id = getPlatformTargetId();
        PlatformTarget target = manager.findTarget(id);
        if (target == null) {
            throw new ExecutionException(ProjectLocalize.errorPlatformTargetMissing(id).get());
        }
        Platform platform = target.getPlatform();
        if (platform != null) {
            return platform;
        }
        target.connect();
        throw new ExecutionException(ProjectLocalize.errorPlatformTargetConnecting(target.getDisplayName()).get());
    }
}
