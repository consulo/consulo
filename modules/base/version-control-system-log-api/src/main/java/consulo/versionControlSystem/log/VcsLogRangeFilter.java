// Copyright 2000-2022 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.versionControlSystem.log;

import java.util.Collection;
import java.util.List;

/**
 * Filters the log by a range or several ranges of commits.
 * <p>
 * A range represents a set of commits which are reachable from the end of the range, but not reachable from the start of the range.
 * A continuous ancestry list of commits is one simple case of the range. Another example is a difference between branches.
 */
public interface VcsLogRangeFilter extends VcsLogFilter {
    /**
     * The range between commits (or references pointing to the commits), represented by this filter.
     */
    List<RefRange> getRanges();

    /**
     * Text presentation for the filter (to display in filter popup).
     */
    Collection<String> getTextPresentation();

    /**
     * Represents a range between two references pointing to the commits.
     * <p>
     * The references are given as Strings and may be either names of {@link VcsRef}s or commit hashes.
     *
     * @param exclusiveRef the reference not containing commits of the range, which means that commits are not reachable from this ref.
     * @param inclusiveRef the reference containing commits of the range, which means that commits are reachable from this ref.
     */
    record RefRange(String exclusiveRef, String inclusiveRef) {
    }
}
