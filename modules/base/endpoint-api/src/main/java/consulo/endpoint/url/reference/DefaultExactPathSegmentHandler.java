// Copyright 2000-2025 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.endpoint.url.reference;

import consulo.endpoint.url.UrlPath;

import java.util.ArrayList;
import java.util.List;

final class DefaultExactPathSegmentHandler implements PathSegmentHandler {
    static final DefaultExactPathSegmentHandler INSTANCE = new DefaultExactPathSegmentHandler();

    private DefaultExactPathSegmentHandler() {
    }

    @Override
    public String render(UrlPath.PathSegment segment) {
        return UrlPath.FULL_PATH_VARIABLE_PRESENTATION.patternMatch(segment);
    }

    @Override
    public List<UrlPath.PathSegment> getExactPrefix(UrlPath path) {
        List<UrlPath.PathSegment> result = new ArrayList<>();
        for (UrlPath.PathSegment segment : path.getSegments()) {
            if (!(segment instanceof UrlPath.PathSegment.Exact)) {
                break;
            }
            result.add(segment);
        }
        while (!result.isEmpty() && result.get(result.size() - 1).isEmpty()) {
            result.remove(result.size() - 1);
        }
        return result;
    }
}
