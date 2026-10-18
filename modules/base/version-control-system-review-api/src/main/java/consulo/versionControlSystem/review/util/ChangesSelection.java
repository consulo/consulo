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
package consulo.versionControlSystem.review.util;

import consulo.versionControlSystem.change.RefComparisonChange;
import org.jspecify.annotations.Nullable;

import java.util.List;

public sealed interface ChangesSelection permits PreciseChangesSelection, FuzzyChangesSelection {
    List<RefComparisonChange> getChanges();

    int getSelectedIdx();

    default @Nullable RefComparisonChange getSelectedChange() {
        List<RefComparisonChange> changes = getChanges();
        int idx = getSelectedIdx();
        return idx >= 0 && idx < changes.size() ? changes.get(idx) : null;
    }

    static boolean equalChanges(@Nullable ChangesSelection selection, @Nullable Object other) {
        if (selection == null && other != null) {
            return false;
        }
        if (selection != null && other == null) {
            return false;
        }
        if (other == selection) {
            return true;
        }
        if (selection == null || other == null) {
            return false; // for null safety
        }
        ChangesSelection otherSelection = (ChangesSelection) other;
        if (!selection.getChanges().equals(otherSelection.getChanges())) {
            return false;
        }
        return selection.getSelectedIdx() == otherSelection.getSelectedIdx();
    }
}
