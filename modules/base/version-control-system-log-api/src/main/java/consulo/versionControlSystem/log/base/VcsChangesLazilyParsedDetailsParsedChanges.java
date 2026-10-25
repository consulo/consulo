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

import java.util.Collection;
import java.util.List;

public class VcsChangesLazilyParsedDetailsParsedChanges implements VcsChangesLazilyParsedDetailsChanges {
    private final Collection<Change> myMergedChanges;
    private final List<? extends Collection<Change>> myChanges;

    VcsChangesLazilyParsedDetailsParsedChanges(Collection<Change> mergedChanges, List<? extends Collection<Change>> changes) {
        myMergedChanges = mergedChanges;
        myChanges = changes;
    }

    @Override
    public Collection<Change> getMergedChanges() {
        return myMergedChanges;
    }

    @Override
    public Collection<Change> getChanges(int parent) {
        return myChanges.get(parent);
    }

    @Override
    public int size() {
        int size = 0;
        for (Collection<Change> changesToParent : myChanges) {
            size += changesToParent.size();
        }
        return size;
    }
}
