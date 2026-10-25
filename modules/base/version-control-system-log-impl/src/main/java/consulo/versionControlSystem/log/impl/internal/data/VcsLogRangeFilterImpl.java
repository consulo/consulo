// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.versionControlSystem.log.impl.internal.data;

import consulo.versionControlSystem.log.VcsLogRangeFilter;
import consulo.versionControlSystem.log.util.VcsLogUtil;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.stream.Collectors;

public class VcsLogRangeFilterImpl implements VcsLogRangeFilter {
    private final List<RefRange> myRanges;

    public VcsLogRangeFilterImpl(List<RefRange> ranges) {
        myRanges = List.copyOf(ranges);
    }

    @Override
    public List<RefRange> getRanges() {
        return myRanges;
    }

    @Override
    public Collection<String> getTextPresentation() {
        return myRanges.stream().map(range -> range.exclusiveRef() + ".." + range.inclusiveRef()).toList();
    }

    @Override
    public String getDisplayText() {
        return myRanges.stream()
            .map(range -> VcsLogUtil.getShortHash(range.exclusiveRef()) + ".." + VcsLogUtil.getShortHash(range.inclusiveRef()))
            .collect(Collectors.joining(", "));
    }

    @Override
    public String toString() {
        return getDisplayText();
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (other == null || getClass() != other.getClass()) {
            return false;
        }
        return new HashSet<>(myRanges).equals(new HashSet<>(((VcsLogRangeFilterImpl) other).myRanges));
    }

    @Override
    public int hashCode() {
        return new HashSet<>(myRanges).hashCode();
    }
}
