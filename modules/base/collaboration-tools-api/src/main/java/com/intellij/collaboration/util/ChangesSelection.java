// Copyright 2000-2023 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package com.intellij.collaboration.util;

import com.intellij.collaboration.ui.codereview.diff.DiffLineLocation;
import org.jetbrains.annotations.ApiStatus;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.util.List;
import java.util.Objects;

@ApiStatus.Experimental
public sealed interface ChangesSelection permits ChangesSelection.Precise, ChangesSelection.Fuzzy {
    @Nonnull
    List<RefComparisonChange> getChanges();

    int getSelectedIdx();

    default @Nullable RefComparisonChange getSelectedChange() {
        int idx = getSelectedIdx();
        List<RefComparisonChange> changes = getChanges();
        return idx >= 0 && idx < changes.size() ? changes.get(idx) : null;
    }

    static boolean equalChanges(@Nullable ChangesSelection self, @Nullable Object other) {
        if (self == null && other != null) {
            return false;
        }
        if (self != null && other == null) {
            return false;
        }
        if (other == self) {
            return true;
        }
        if (self == null) {
            return false;
        }

        ChangesSelection otherSelection = (ChangesSelection) other;

        if (!self.getChanges().equals(otherSelection.getChanges())) {
            return false;
        }
        return self.getSelectedIdx() == otherSelection.getSelectedIdx();
    }

    /**
     * Single change selected from {@link #getChanges()}
     */
    final class Precise implements ChangesSelection {
        private final @Nonnull List<RefComparisonChange> changes;
        private final int selectedIdx;
        private final @Nullable DiffLineLocation location;

        public Precise(@Nonnull List<RefComparisonChange> changes, int selectedIdx, @Nullable DiffLineLocation location) {
            this.changes = changes;
            this.selectedIdx = selectedIdx;
            this.location = location;
        }

        public Precise(@Nonnull List<RefComparisonChange> changes, int selectedIdx) {
            this(changes, selectedIdx, null);
        }

        public Precise(@Nonnull List<RefComparisonChange> changes) {
            this(changes, 0, null);
        }

        public Precise(
            @Nonnull List<RefComparisonChange> changes,
            @Nonnull RefComparisonChange change,
            @Nullable DiffLineLocation location
        ) {
            this(changes, indexOf(changes, change), location);
        }

        public Precise(@Nonnull List<RefComparisonChange> changes, @Nonnull RefComparisonChange change) {
            this(changes, change, null);
        }

        private static int indexOf(List<RefComparisonChange> changes, RefComparisonChange change) {
            for (int i = 0; i < changes.size(); i++) {
                if (changes.get(i).equals(change)) {
                    return i;
                }
            }
            return -1;
        }

        @Override
        public @Nonnull List<RefComparisonChange> getChanges() {
            return changes;
        }

        @Override
        public int getSelectedIdx() {
            return selectedIdx;
        }

        public @Nullable DiffLineLocation getLocation() {
            return location;
        }

        public @Nonnull Precise withLocation(@Nonnull DiffLineLocation location) {
            return new Precise(changes, selectedIdx, location);
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (!(o instanceof Precise that)) {
                return false;
            }
            return selectedIdx == that.selectedIdx &&
                changes.equals(that.changes) &&
                Objects.equals(location, that.location);
        }

        @Override
        public int hashCode() {
            return Objects.hash(changes, selectedIdx, location);
        }

        @Override
        public String toString() {
            return "Precise(changes=" + changes + ", selectedIdx=" + selectedIdx + ", location=" + location + ")";
        }
    }

    /**
     * Changes selected by a certain group (like directory)
     */
    final class Fuzzy implements ChangesSelection {
        private final @Nonnull List<RefComparisonChange> changes;
        private final int selectedIdx;

        public Fuzzy(@Nonnull List<RefComparisonChange> changes, int selectedIdx) {
            this.changes = changes;
            this.selectedIdx = selectedIdx;
        }

        public Fuzzy(@Nonnull List<RefComparisonChange> changes) {
            this(changes, 0);
        }

        @Override
        public @Nonnull List<RefComparisonChange> getChanges() {
            return changes;
        }

        @Override
        public int getSelectedIdx() {
            return selectedIdx;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (!(o instanceof Fuzzy that)) {
                return false;
            }
            return selectedIdx == that.selectedIdx && changes.equals(that.changes);
        }

        @Override
        public int hashCode() {
            return Objects.hash(changes, selectedIdx);
        }

        @Override
        public String toString() {
            return "Fuzzy(changes=" + changes + ", selectedIdx=" + selectedIdx + ")";
        }
    }
}
