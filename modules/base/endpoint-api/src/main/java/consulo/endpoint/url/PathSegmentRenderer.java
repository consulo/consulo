// Copyright 2000-2025 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.endpoint.url;

public interface PathSegmentRenderer {
    default String visitExact(UrlPath.PathSegment.Exact exact) {
        return exact.getValue();
    }

    default String visitVariable(UrlPath.PathSegment.Variable variable) {
        return UrlPath.STAR;
    }

    default String visitUndefined() {
        return UrlPath.UNKNOWN_URL_PATH_SEGMENT_PRESENTATION;
    }

    default String visitComposite(UrlPath.PathSegment.Composite composite) {
        StringBuilder builder = new StringBuilder();
        for (UrlPath.PathSegment segment : composite.getSegments()) {
            builder.append(patternMatch(segment));
        }
        return builder.toString();
    }

    default String patternMatch(UrlPath.PathSegment segment) {
        if (segment instanceof UrlPath.PathSegment.Exact exact) {
            return visitExact(exact);
        }
        if (segment instanceof UrlPath.PathSegment.Variable variable) {
            return visitVariable(variable);
        }
        if (segment instanceof UrlPath.PathSegment.Composite composite) {
            return visitComposite(composite);
        }
        return visitUndefined();
    }
}
