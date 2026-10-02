// Copyright 2000-2025 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.endpoint.url.reference;

import consulo.document.util.TextRange;
import consulo.endpoint.url.PlaceholderSplitEscaper;
import consulo.endpoint.url.UrlConversionConstants;
import consulo.endpoint.url.UrlPath.PathSegment;
import consulo.endpoint.url.UrlSpecialSegmentMarker;
import consulo.language.psi.util.SplitEscaper;
import consulo.util.collection.SmartList;
import consulo.util.lang.Pair;
import org.jspecify.annotations.Nullable;

import java.util.List;

public final class UrlExtractorUtil {
    private UrlExtractorUtil() {
    }

    public static SplitEscaper springLikePropertySplitEscaper(CharSequence input, String pattern) {
        return PlaceholderSplitEscaper.create(
            List.of(UrlConversionConstants.SPRING_LIKE_PLACEHOLDER_BRACES, UrlConversionConstants.SPRING_LIKE_PATH_VARIABLE_BRACES),
            input,
            pattern
        );
    }

    public static PathSegment extractSegmentLikeSpring(String segmentStr) {
        @Nullable PathSegment result = extractPlaceholderAsUndefined(segmentStr, UrlConversionConstants.SPRING_LIKE_PLACEHOLDER_BRACES);
        if (result != null) {
            return result;
        }
        result = extractPathVariable(segmentStr, UrlConversionConstants.SPRING_LIKE_PATH_VARIABLE_BRACES);
        if (result != null) {
            return result;
        }
        result = extractAnyPathVariable(segmentStr, "*");
        if (result != null) {
            return result;
        }
        result = extractPlaceholder(segmentStr, "**");
        if (result != null) {
            return result;
        }
        return new PathSegment.Exact(segmentStr);
    }

    public static @Nullable PathSegment extractAnyPathVariable(String segmentStr, String anySequenceSymbol) {
        return !segmentStr.equals(anySequenceSymbol) ? null : new PathSegment.Variable(null);
    }

    public static @Nullable PathSegment extractPlaceholder(String segmentStr, String anySequenceSymbol) {
        return !segmentStr.equals(anySequenceSymbol) ? null : PathSegment.Undefined.INSTANCE;
    }

    public static @Nullable PathSegment extractPlaceholderAsUndefined(String segmentStr, UrlSpecialSegmentMarker wrap) {
        return wrap.matches(segmentStr) ? PathSegment.Undefined.INSTANCE : null;
    }

    public static @Nullable PathSegment extractPathVariable(String segmentStr, UrlSpecialSegmentMarker pathVariableWrap) {
        List<Pair<TextRange, UrlSpecialSegmentMarker.ExtractionInfo>> allVariables = pathVariableWrap.extractAll(segmentStr);
        if (allVariables.isEmpty()) {
            return null;
        }
        if (allVariables.size() == 1) {
            Pair<TextRange, UrlSpecialSegmentMarker.ExtractionInfo> single = allVariables.get(0);
            TextRange range = single.getFirst();
            UrlSpecialSegmentMarker.ExtractionInfo second = single.getSecond();
            if (range.getStartOffset() == 0 && range.getEndOffset() == segmentStr.length()) {
                return new PathSegment.Variable(second.getValue(), getOrNull(second.getRegexGroups(), 2));
            }
        }

        List<PathSegment> resultSegments = new SmartList<>();
        int prevEnd = 0;
        for (Pair<TextRange, UrlSpecialSegmentMarker.ExtractionInfo> pair : allVariables) {
            TextRange varRange = pair.getFirst();
            UrlSpecialSegmentMarker.ExtractionInfo v = pair.getSecond();
            if (prevEnd != varRange.getStartOffset()) {
                resultSegments.add(new PathSegment.Exact(segmentStr.substring(prevEnd, varRange.getStartOffset())));
            }
            resultSegments.add(new PathSegment.Variable(v.getValue(), getOrNull(v.getRegexGroups(), 2)));
            prevEnd = varRange.getEndOffset();
        }
        if (prevEnd != segmentStr.length()) {
            resultSegments.add(new PathSegment.Exact(segmentStr.substring(prevEnd, segmentStr.length())));
        }
        return new PathSegment.Composite(resultSegments);
    }

    private static @Nullable String getOrNull(List<@Nullable String> list, int index) {
        return index >= 0 && index < list.size() ? list.get(index) : null;
    }
}
