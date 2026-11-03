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
package consulo.versionControlSystem.distributed.push;

import consulo.localize.LocalizeValue;
import consulo.versionControlSystem.distributed.localize.DistributedVcsLocalize;

/**
 * Kind of force push, offered by {@link PushSupport#getForcePushTypes()}.
 * Each type is shown as separate force push action in push dialog, and passed to
 * {@link Pusher#push(java.util.Map, VcsPushOptionValue, ForcePushType, java.util.Map)}.
 */
public interface ForcePushType {
    /**
     * Plain force push, used by vcs which do not provide own types
     */
    ForcePushType DEFAULT = DistributedVcsLocalize::actionForcePush;

    LocalizeValue getActionText();

    /**
     * @return additional info, shown as tooltip of force push action. For example which options will be used
     */
    default LocalizeValue getDescription() {
        return LocalizeValue.empty();
    }
}
