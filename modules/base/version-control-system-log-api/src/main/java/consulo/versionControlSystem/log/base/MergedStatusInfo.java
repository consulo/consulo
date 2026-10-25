// Copyright 2000-2017 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
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
package consulo.versionControlSystem.log.base;

import consulo.util.collection.SmartList;

import java.util.List;

public final class MergedStatusInfo<S> {
    private final S myStatusInfo;
    private final List<S> myMergedStatusInfos;

    public MergedStatusInfo(S statusInfo) {
        this(statusInfo, List.of());
    }

    public MergedStatusInfo(S statusInfo, List<S> infos) {
        myStatusInfo = statusInfo;
        myMergedStatusInfos = new SmartList<>(infos);
    }

    public S getStatusInfo() {
        return myStatusInfo;
    }

    public List<S> getMergedStatusInfos() {
        return myMergedStatusInfos;
    }

    @Override
    public String toString() {
        return "MergedStatusInfo{" +
            "myStatusInfo=" + myStatusInfo +
            ", myMergedStatusInfos=" + myMergedStatusInfos +
            '}';
    }
}
