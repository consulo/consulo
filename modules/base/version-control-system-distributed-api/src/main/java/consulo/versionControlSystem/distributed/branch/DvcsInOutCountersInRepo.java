// Copyright 2000-2025 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
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
package consulo.versionControlSystem.distributed.branch;

import org.jspecify.annotations.Nullable;

/**
 * Incoming and outgoing commit counters of one branch in one repository. A null counter means it is unknown; an incoming
 * counter of 0 means the remote has commits which are not fetched yet.
 */
public record DvcsInOutCountersInRepo(@Nullable Integer incoming, @Nullable Integer outgoing) {
    public boolean hasIncoming() {
        return incoming != null;
    }

    public boolean hasOutgoing() {
        return outgoing != null;
    }

    public boolean hasUnfetched() {
        return incoming != null && incoming == 0;
    }
}
