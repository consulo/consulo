// Copyright 2000-2023 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
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

import consulo.versionControlSystem.change.Change;
import consulo.util.lang.lazy.LazyValue;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.BiFunction;

final class VcsChangesLazilyParsedDetailsMergedChange extends MergedChange {
    private final LazyValue<List<Change>> mySourceChanges;

    VcsChangesLazilyParsedDetailsMergedChange(Change change,
                                              MergedStatusInfo<VcsFileStatusInfo> statusInfo,
                                              BiFunction<List<VcsFileStatusInfo>, Integer, List<Change>> parser) {
        super(change);
        mySourceChanges = LazyValue.notNull(() -> {
            List<Change> sourceChanges = new ArrayList<>();
            for (int parent = 0; parent < statusInfo.getMergedStatusInfos().size(); parent++) {
                List<VcsFileStatusInfo> statusInfos = Collections.singletonList(statusInfo.getMergedStatusInfos().get(parent));
                sourceChanges.addAll(parser.apply(statusInfos, parent));
            }
            return sourceChanges;
        });
    }

    @Override
    public List<Change> getSourceChanges() {
        return mySourceChanges.get();
    }
}
