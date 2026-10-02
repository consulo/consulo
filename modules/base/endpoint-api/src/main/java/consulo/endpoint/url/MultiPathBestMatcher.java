// Copyright 2000-2025 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.endpoint.url;

import org.jspecify.annotations.Nullable;

import java.util.HashSet;
import java.util.Set;

public final class MultiPathBestMatcher {
    private final Set<UrlTargetInfo> myBestResolveMatches = new HashSet<>();
    private int myLongestMatchLength = 1;

    public MultiPathBestMatcher addBestMatching(Iterable<? extends UrlTargetInfo> all) {
        return addBestMatching(all, null);
    }

    public MultiPathBestMatcher addBestMatching(Iterable<? extends UrlTargetInfo> all, @Nullable UrlPath original) {
        for (UrlTargetInfo urlTargetInfo : all) {
            int exactCount = original != null ? original.commonLength(urlTargetInfo.getPath()) : countExact(urlTargetInfo.getPath());
            if (exactCount > myLongestMatchLength) {
                myLongestMatchLength = exactCount;
                myBestResolveMatches.clear();
                myBestResolveMatches.add(urlTargetInfo);
            }
            else if (exactCount == myLongestMatchLength) {
                myBestResolveMatches.add(urlTargetInfo);
            }
        }
        return this;
    }

    private static int countExact(UrlPath path) {
        int count = 0;
        for (UrlPath.PathSegment segment : path.getSegments()) {
            if (segment instanceof UrlPath.PathSegment.Exact) {
                count++;
            }
        }
        return count;
    }

    public Set<UrlTargetInfo> getResult() {
        return myBestResolveMatches;
    }
}
