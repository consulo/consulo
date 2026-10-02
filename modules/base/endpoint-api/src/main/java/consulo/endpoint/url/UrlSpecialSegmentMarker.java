// Copyright 2000-2025 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.endpoint.url;

import consulo.document.util.PlaceholderTextRanges;
import consulo.document.util.TextRange;
import consulo.util.lang.Pair;
import consulo.util.lang.StringUtil;
import org.jspecify.annotations.Nullable;

import java.util.AbstractList;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class UrlSpecialSegmentMarker {
    private final String myPrefix;
    private final String mySuffix;
    private final @Nullable Pattern myPattern;

    public UrlSpecialSegmentMarker(String prefix, String suffix) {
        this(prefix, suffix, null);
    }

    public UrlSpecialSegmentMarker(String prefix, String suffix, @Nullable Pattern pattern) {
        myPrefix = prefix;
        mySuffix = suffix;
        myPattern = pattern;
    }

    public String getPrefix() {
        return myPrefix;
    }

    public String getSuffix() {
        return mySuffix;
    }

    public boolean matches(CharSequence segment) {
        return StringUtil.startsWith(segment, myPrefix) && StringUtil.endsWith(segment, mySuffix);
    }

    private @Nullable ExtractionInfo extractValue(CharSequence segmentStr) {
        if (!matches(segmentStr)) {
            return null;
        }
        String body = segmentStr.subSequence(myPrefix.length(), segmentStr.length() - mySuffix.length()).toString();
        if (myPattern != null) {
            Matcher matcher = myPattern.matcher(body);
            if (matcher.find()) {
                return new ExtractionInfo(matcher.group(1), matcher);
            }
            return null;
        }
        return new ExtractionInfo(body, null);
    }

    public static final class ExtractionInfo {
        private final String myValue;
        private final @Nullable Matcher myMatcher;
        private final List<@Nullable String> myRegexGroups;

        public ExtractionInfo(String value, @Nullable Matcher matcher) {
            myValue = value;
            myMatcher = matcher;
            myRegexGroups = matcher != null ? new AbstractList<>() {
                @Override
                public int size() {
                    return matcher.groupCount() + 1;
                }

                @Override
                public @Nullable String get(int index) {
                    return matcher.group(index);
                }
            } : List.of();
        }

        public String getValue() {
            return myValue;
        }

        public List<@Nullable String> getRegexGroups() {
            return myRegexGroups;
        }

        @Override
        public boolean equals(@Nullable Object o) {
            if (this == o) {
                return true;
            }
            if (!(o instanceof ExtractionInfo that)) {
                return false;
            }
            return myValue.equals(that.myValue) && Objects.equals(myMatcher, that.myMatcher);
        }

        @Override
        public int hashCode() {
            return 31 * myValue.hashCode() + Objects.hashCode(myMatcher);
        }

        @Override
        public String toString() {
            return "ExtractionInfo(value=" + myValue + ", matcher=" + myMatcher + ")";
        }
    }

    public List<Pair<TextRange, ExtractionInfo>> extractAll(CharSequence segmentStr) {
        List<Pair<TextRange, ExtractionInfo>> result = new ArrayList<>();
        for (TextRange range : PlaceholderTextRanges.getPlaceholderRanges(segmentStr.toString(), myPrefix, mySuffix, true, true)) {
            ExtractionInfo info = extractValue(range.subSequence(segmentStr));
            if (info != null) {
                result.add(Pair.create(range, info));
            }
        }
        return result;
    }
}
