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

import consulo.versionControlSystem.change.Change;
import org.jspecify.annotations.Nullable;

public final class VcsFileStatusInfoMerger extends VcsStatusMerger<VcsFileStatusInfo, CharSequence> {
    @Override
    protected VcsFileStatusInfo createStatus(Change.Type type, CharSequence path, @Nullable CharSequence secondPath) {
        return new VcsFileStatusInfo(type, path, secondPath);
    }

    @Override
    protected CharSequence getFirstPath(VcsFileStatusInfo info) {
        return info.getFirst();
    }

    @Override
    protected @Nullable CharSequence getSecondPath(VcsFileStatusInfo info) {
        return info.getSecond();
    }

    @Override
    protected Change.Type getType(VcsFileStatusInfo info) {
        return info.getType();
    }
}
