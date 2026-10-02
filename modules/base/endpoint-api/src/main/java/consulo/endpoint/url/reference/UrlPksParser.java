// Copyright 2000-2025 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.endpoint.url.reference;

import consulo.annotation.access.RequiredReadAction;
import consulo.endpoint.internal.EndpointStringUtil;
import consulo.endpoint.url.UrlPath;
import consulo.language.psi.util.PartiallyKnownString;
import consulo.language.psi.util.SplitEscaper;
import consulo.util.lang.Pair;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.BiFunction;
import java.util.function.Function;

/**
 * A parser for extracting URL from a {@link PartiallyKnownString} and mapping URL parts to corresponding {@link consulo.language.psi.PsiElement}s.
 * <p>
 * Is configurable via fields to support framework-specific URL parts like Path Variables and Placeholders.
 */
public final class UrlPksParser {
    private static final String SCHEME_SEPARATOR = "://";
    private static final String QUERY_SEPARATOR = "?";
    private static final String QUERY_PARAMS_SEPARATOR = "&";

    private BiFunction<CharSequence, String, SplitEscaper> mySplitEscaper;
    private Function<String, UrlPath.@Nullable PathSegment> myCustomPathSegmentExtractor;
    private boolean myParseQueryParameters;
    private boolean myShouldHaveScheme = true;

    public UrlPksParser() {
        this((input, pattern) -> SplitEscaper.ACCEPT_ALL);
    }

    public UrlPksParser(BiFunction<CharSequence, String, SplitEscaper> splitEscaper) {
        this(splitEscaper, UrlExtractorUtil::extractSegmentLikeSpring);
    }

    public UrlPksParser(
        BiFunction<CharSequence, String, SplitEscaper> splitEscaper,
        Function<String, UrlPath.@Nullable PathSegment> customPathSegmentExtractor
    ) {
        this(splitEscaper, customPathSegmentExtractor, true);
    }

    public UrlPksParser(
        BiFunction<CharSequence, String, SplitEscaper> splitEscaper,
        Function<String, UrlPath.@Nullable PathSegment> customPathSegmentExtractor,
        boolean parseQueryParameters
    ) {
        mySplitEscaper = splitEscaper;
        myCustomPathSegmentExtractor = customPathSegmentExtractor;
        myParseQueryParameters = parseQueryParameters;
    }

    public BiFunction<CharSequence, String, SplitEscaper> getSplitEscaper() {
        return mySplitEscaper;
    }

    public void setSplitEscaper(BiFunction<CharSequence, String, SplitEscaper> splitEscaper) {
        mySplitEscaper = splitEscaper;
    }

    public Function<String, UrlPath.@Nullable PathSegment> getCustomPathSegmentExtractor() {
        return myCustomPathSegmentExtractor;
    }

    public void setCustomPathSegmentExtractor(Function<String, UrlPath.@Nullable PathSegment> customPathSegmentExtractor) {
        myCustomPathSegmentExtractor = customPathSegmentExtractor;
    }

    public boolean getParseQueryParameters() {
        return myParseQueryParameters;
    }

    public void setParseQueryParameters(boolean parseQueryParameters) {
        myParseQueryParameters = parseQueryParameters;
    }

    /**
     * setting to {@code false} makes parser able to handle input without a scheme like {@code "localhost/some/path"}.
     * NOTE: in that case incomplete strings like {@code "loc"} could be treated as both host and scheme
     * which could lead to an incompatible scheme in produced results,
     * please check that {@link ParsedPksUrl#getScheme} {@code !=} {@link ParsedPksUrl#getAuthority} in that case before further processing
     */
    public boolean getShouldHaveScheme() {
        return myShouldHaveScheme;
    }

    public void setShouldHaveScheme(boolean shouldHaveScheme) {
        myShouldHaveScheme = shouldHaveScheme;
    }

    public static final class ParsedPksUrl {
        private final @Nullable PartiallyKnownString myScheme;
        private final @Nullable PartiallyKnownString myAuthority;
        private final List<PartiallyKnownString> mySlashesSplit;
        private final UrlPath myUrlPath;
        private final List<QueryParameter> myQueryParameters;

        ParsedPksUrl(
            @Nullable PartiallyKnownString scheme,
            @Nullable PartiallyKnownString authority,
            List<PartiallyKnownString> slashesSplit,
            UrlPath urlPath
        ) {
            this(scheme, authority, slashesSplit, urlPath, List.of());
        }

        ParsedPksUrl(
            @Nullable PartiallyKnownString scheme,
            @Nullable PartiallyKnownString authority,
            List<PartiallyKnownString> slashesSplit,
            UrlPath urlPath,
            List<QueryParameter> queryParameters
        ) {
            myScheme = scheme;
            myAuthority = authority;
            mySlashesSplit = slashesSplit;
            myUrlPath = urlPath;
            myQueryParameters = queryParameters;
        }

        public @Nullable PartiallyKnownString getScheme() {
            return myScheme;
        }

        public @Nullable PartiallyKnownString getAuthority() {
            return myAuthority;
        }

        public List<PartiallyKnownString> getSlashesSplit() {
            return mySlashesSplit;
        }

        public UrlPath getUrlPath() {
            return myUrlPath;
        }

        public List<QueryParameter> getQueryParameters() {
            return myQueryParameters;
        }

        @Override
        public boolean equals(@Nullable Object o) {
            if (this == o) {
                return true;
            }
            if (!(o instanceof ParsedPksUrl that)) {
                return false;
            }
            return Objects.equals(myScheme, that.myScheme)
                && Objects.equals(myAuthority, that.myAuthority)
                && mySlashesSplit.equals(that.mySlashesSplit)
                && myUrlPath.equals(that.myUrlPath)
                && myQueryParameters.equals(that.myQueryParameters);
        }

        @Override
        public int hashCode() {
            int result = Objects.hashCode(myScheme);
            result = 31 * result + Objects.hashCode(myAuthority);
            result = 31 * result + mySlashesSplit.hashCode();
            result = 31 * result + myUrlPath.hashCode();
            result = 31 * result + myQueryParameters.hashCode();
            return result;
        }

        @Override
        public String toString() {
            return "ParsedPksUrl(scheme=" + myScheme +
                ", authority=" + myAuthority +
                ", slashesSplit=" + mySlashesSplit +
                ", urlPath=" + myUrlPath +
                ", queryParameters=" + myQueryParameters + ")";
        }
    }

    public static final class ParsedPksUrlPath {
        private final List<PartiallyKnownString> mySlashesSplit;
        private final UrlPath myUrlPath;
        private final List<QueryParameter> myQueryParameters;

        ParsedPksUrlPath(List<PartiallyKnownString> slashesSplit, UrlPath urlPath) {
            this(slashesSplit, urlPath, List.of());
        }

        ParsedPksUrlPath(List<PartiallyKnownString> slashesSplit, UrlPath urlPath, List<QueryParameter> queryParameters) {
            mySlashesSplit = slashesSplit;
            myUrlPath = urlPath;
            myQueryParameters = queryParameters;
        }

        public List<PartiallyKnownString> getSlashesSplit() {
            return mySlashesSplit;
        }

        public UrlPath getUrlPath() {
            return myUrlPath;
        }

        public List<QueryParameter> getQueryParameters() {
            return myQueryParameters;
        }

        @Override
        public boolean equals(@Nullable Object o) {
            if (this == o) {
                return true;
            }
            if (!(o instanceof ParsedPksUrlPath that)) {
                return false;
            }
            return mySlashesSplit.equals(that.mySlashesSplit)
                && myUrlPath.equals(that.myUrlPath)
                && myQueryParameters.equals(that.myQueryParameters);
        }

        @Override
        public int hashCode() {
            int result = mySlashesSplit.hashCode();
            result = 31 * result + myUrlPath.hashCode();
            result = 31 * result + myQueryParameters.hashCode();
            return result;
        }

        @Override
        public String toString() {
            return "ParsedPksUrlPath(slashesSplit=" + mySlashesSplit +
                ", urlPath=" + myUrlPath +
                ", queryParameters=" + myQueryParameters + ")";
        }
    }

    public static final class QueryParameter {
        private final PartiallyKnownString myName;
        private final @Nullable PartiallyKnownString myValue;

        public QueryParameter(PartiallyKnownString name, @Nullable PartiallyKnownString value) {
            myName = name;
            myValue = value;
        }

        public PartiallyKnownString getName() {
            return myName;
        }

        public @Nullable PartiallyKnownString getValue() {
            return myValue;
        }

        @Override
        public boolean equals(@Nullable Object o) {
            if (this == o) {
                return true;
            }
            if (!(o instanceof QueryParameter that)) {
                return false;
            }
            return myName.equals(that.myName) && Objects.equals(myValue, that.myValue);
        }

        @Override
        public int hashCode() {
            return 31 * myName.hashCode() + Objects.hashCode(myValue);
        }

        @Override
        public String toString() {
            return "QueryParameter(name=" + myName + ", value=" + myValue + ")";
        }
    }

    @RequiredReadAction
    public ParsedPksUrl parseFullUrl(PartiallyKnownString pkwString) {
        int schemeSeparatorIndex = pkwString.findIndexOfInKnown(SCHEME_SEPARATOR);
        int firstSlashIndex = pkwString.findIndexOfInKnown("/");
        PartiallyKnownString scheme;
        PartiallyKnownString remaining;
        if (schemeSeparatorIndex == -1 && firstSlashIndex == -1) {
            if (myShouldHaveScheme) {
                scheme = EndpointStringUtil.isBlank(pkwString.getConcatenationOfKnown()) ? PartiallyKnownString.EMPTY : pkwString;
                remaining = PartiallyKnownString.EMPTY;
            }
            else {
                scheme = pkwString;
                remaining = pkwString;
            }
        }
        else if (schemeSeparatorIndex >= 0) {
            Pair<PartiallyKnownString, PartiallyKnownString> split =
                pkwString.splitAtInKnown(schemeSeparatorIndex + SCHEME_SEPARATOR.length());
            scheme = split.getFirst();
            remaining = split.getSecond();
        }
        else {
            Pair<PartiallyKnownString, PartiallyKnownString> split = pkwString.splitAtInKnown(firstSlashIndex + 1);
            @Nullable String secondValue = split.getSecond().getValueIfKnown();
            if (myShouldHaveScheme && secondValue != null && secondValue.isEmpty()) {
                scheme = split.getFirst();
                remaining = split.getSecond();
            }
            else {
                scheme = PartiallyKnownString.EMPTY;
                remaining = pkwString;
            }
        }

        List<PartiallyKnownString> slashesSplit = splitUrlPath(remaining);

        if (!myParseQueryParameters) {
            if (myShouldHaveScheme && isNullOrEmpty(scheme.getValueIfKnown())) {
                return new ParsedPksUrl(scheme, null, slashesSplit, parseUrlPath(slashesSplit));
            }
            else {
                List<PartiallyKnownString> slashesSplitTail = tailOrEmpty(slashesSplit);
                return new ParsedPksUrl(scheme, firstOrNull(slashesSplit), slashesSplitTail, parseUrlPath(slashesSplitTail));
            }
        }

        Pair<List<PartiallyKnownString>, @Nullable PartiallyKnownString> queryPart = extractQueryPart(slashesSplit);
        List<PartiallyKnownString> pathSlashesSplit = queryPart.getFirst();
        List<QueryParameter> queryParameters = parseQueryParameters(queryPart.getSecond());

        if (myShouldHaveScheme && isNullOrEmpty(scheme.getValueIfKnown())) {
            return new ParsedPksUrl(scheme, null, pathSlashesSplit, parseUrlPath(pathSlashesSplit), queryParameters);
        }
        else {
            List<PartiallyKnownString> pathSlashesSplitTail = tailOrEmpty(pathSlashesSplit);
            return new ParsedPksUrl(
                scheme,
                firstOrNull(pathSlashesSplit),
                pathSlashesSplitTail,
                parseUrlPath(pathSlashesSplitTail),
                queryParameters
            );
        }
    }

    private static boolean isNullOrEmpty(@Nullable String value) {
        return value == null || value.isEmpty();
    }

    private static <T> List<T> tailOrEmpty(List<T> list) {
        return list.isEmpty() ? List.of() : list.subList(1, list.size());
    }

    private static <T> @Nullable T firstOrNull(List<T> list) {
        return list.isEmpty() ? null : list.get(0);
    }

    @RequiredReadAction
    private Pair<List<PartiallyKnownString>, @Nullable PartiallyKnownString> extractQueryPart(List<PartiallyKnownString> slashesSplit) {
        if (slashesSplit.isEmpty()) {
            return Pair.create(slashesSplit, null);
        }
        PartiallyKnownString lastNode = slashesSplit.get(slashesSplit.size() - 1);
        int qp = lastNode.findIndexOfInKnown(QUERY_SEPARATOR);
        if (qp == -1) {
            return Pair.create(slashesSplit, null);
        }
        Pair<PartiallyKnownString, PartiallyKnownString> split = lastNode.splitAtInKnown(qp);

        List<PartiallyKnownString> updated = new ArrayList<>(slashesSplit);
        updated.set(updated.size() - 1, split.getFirst());
        return Pair.create(updated, split.getSecond());
    }

    @RequiredReadAction
    private List<QueryParameter> parseQueryParameters(@Nullable PartiallyKnownString queryString) {
        if (queryString == null) {
            return List.of();
        }
        PartiallyKnownString parameters = queryString.splitAtInKnown(QUERY_SEPARATOR.length()).getSecond();
        List<QueryParameter> result = new ArrayList<>();
        for (PartiallyKnownString pair : parameters.split(QUERY_PARAMS_SEPARATOR)) {
            List<PartiallyKnownString> nameAndValue = pair.split("=");
            result.add(new QueryParameter(nameAndValue.get(0), nameAndValue.size() > 1 ? nameAndValue.get(1) : null));
        }
        return result;
    }

    @RequiredReadAction
    public ParsedPksUrlPath parseUrlPath(PartiallyKnownString sourceString) {
        List<PartiallyKnownString> slashesSplit = splitUrlPath(sourceString);

        if (!myParseQueryParameters) {
            return new ParsedPksUrlPath(slashesSplit, parseUrlPath(slashesSplit));
        }

        Pair<List<PartiallyKnownString>, @Nullable PartiallyKnownString> queryPart = extractQueryPart(slashesSplit);
        List<PartiallyKnownString> pathSlashesSplit = queryPart.getFirst();
        List<QueryParameter> queryParameters = parseQueryParameters(queryPart.getSecond());

        return new ParsedPksUrlPath(pathSlashesSplit, parseUrlPath(pathSlashesSplit), queryParameters);
    }

    @RequiredReadAction
    public List<PartiallyKnownString> splitUrlPath(PartiallyKnownString sourceString) {
        return sourceString.split("/", mySplitEscaper);
    }

    private UrlPath parseUrlPath(List<PartiallyKnownString> slashesSplit) {
        List<UrlPath.PathSegment> segments = new ArrayList<>(slashesSplit.size());
        for (PartiallyKnownString pks : slashesSplit) {
            segments.add(pksPathSegment(pks));
        }
        return new UrlPath(segments);
    }

    public UrlPath.PathSegment pksPathSegment(PartiallyKnownString pks) {
        @Nullable String value = pks.getValueIfKnown();
        if (value == null) {
            return UrlPath.PathSegment.Undefined.INSTANCE;
        }
        UrlPath.@Nullable PathSegment segment = myCustomPathSegmentExtractor.apply(value);
        return segment != null ? segment : new UrlPath.PathSegment.Exact(value);
    }
}
