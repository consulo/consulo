/*
 * Copyright 2000-2017 JetBrains s.r.o.
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
package consulo.document.internal;

import consulo.document.Document;
import consulo.document.RangeMarker;
import consulo.document.util.TextRange;
import jakarta.annotation.Nonnull;

import java.util.Collections;
import java.util.List;
import java.util.function.Predicate;

public interface DocumentEx extends Document {
    default void setStripTrailingSpacesEnabled(boolean isEnabled) {
    }

    @Nonnull
    LineIterator createLineIterator();

    void setModificationStamp(long modificationStamp);

    default void addEditReadOnlyListener(@Nonnull EditReadOnlyListener listener) {
    }

    default void removeEditReadOnlyListener(@Nonnull EditReadOnlyListener listener) {
    }

    void replaceText(@Nonnull CharSequence chars, long newModificationStamp);

    default void suppressGuardedExceptions() {
    }

    default void unSuppressGuardedExceptions() {
    }

    default boolean isInEventsHandling() {
        return false;
    }

    default void clearLineModificationFlags() {
    }

    boolean removeRangeMarker(@Nonnull RangeMarkerEx rangeMarker);

    void registerRangeMarker(@Nonnull RangeMarkerEx rangeMarker, int start, int end, boolean greedyToLeft, boolean greedyToRight, int layer);

    @Nonnull
    default List<RangeMarker> getGuardedBlocks() {
        return Collections.emptyList();
    }

    /**
     * Get all range markers
     * and hand them to the {@code processor} in their {@link RangeMarker#getStartOffset()} order
     */
    boolean processRangeMarkers(@Nonnull Predicate<? super RangeMarker> processor);

    /**
     * Get range markers which {@link TextRange#intersects(int, int)} the specified range
     * and hand them to the {@code processor} in their {@link RangeMarker#getStartOffset()} order
     */
    boolean processRangeMarkersOverlappingWith(int start, int end, @Nonnull Predicate<? super RangeMarker> processor);

    /**
     * @return modification stamp. Guaranteed to be strictly increasing on each change unlike the {@link #getModificationStamp()} which can change arbitrarily.
     */
    default int getModificationSequence() {
        return 0;
    }

    default boolean setAcceptSlashR(boolean accept) {
        return false;
    }
}



