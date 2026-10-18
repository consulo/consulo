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
import consulo.versionControlSystem.review.diff.DiffLineLocation;
import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.Objects;

/**
 * Single change selected from {@link #getChanges()}
 */
public final class PreciseChangesSelection implements ChangesSelection {
    private final List<RefComparisonChange> myChanges;
    private final int mySelectedIdx;
    private final @Nullable DiffLineLocation myLocation;

    public PreciseChangesSelection(List<RefComparisonChange> changes) {
        this(changes, 0, null);
    }

    public PreciseChangesSelection(List<RefComparisonChange> changes, int selectedIdx) {
        this(changes, selectedIdx, null);
    }

    public PreciseChangesSelection(List<RefComparisonChange> changes, int selectedIdx, @Nullable DiffLineLocation location) {
        myChanges = changes;
        mySelectedIdx = selectedIdx;
        myLocation = location;
    }

    public PreciseChangesSelection(List<RefComparisonChange> changes, RefComparisonChange change) {
        this(changes, change, null);
    }

    public PreciseChangesSelection(List<RefComparisonChange> changes, RefComparisonChange change, @Nullable DiffLineLocation location) {
        this(changes, changes.indexOf(change), location);
    }

    @Override
    public List<RefComparisonChange> getChanges() {
        return myChanges;
    }

    @Override
    public int getSelectedIdx() {
        return mySelectedIdx;
    }

    public @Nullable DiffLineLocation getLocation() {
        return myLocation;
    }

    public PreciseChangesSelection withLocation(DiffLineLocation location) {
        return new PreciseChangesSelection(myChanges, mySelectedIdx, location);
    }

    @Override
    public boolean equals(@Nullable Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof PreciseChangesSelection that)) {
            return false;
        }
        return mySelectedIdx == that.mySelectedIdx && myChanges.equals(that.myChanges) && Objects.equals(myLocation, that.myLocation);
    }

    @Override
    public int hashCode() {
        return Objects.hash(myChanges, mySelectedIdx, myLocation);
    }

    @Override
    public String toString() {
        return "Precise(changes=" + myChanges + ", selectedIdx=" + mySelectedIdx + ", location=" + myLocation + ")";
    }
}
