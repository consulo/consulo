// Copyright 2000-2025 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.endpoint.url.reference;

import consulo.annotation.access.RequiredReadAction;
import consulo.endpoint.url.FrameworkUrlPathSpecification;
import consulo.endpoint.url.UrlPath;
import consulo.endpoint.url.UrlPathModelUtil;
import consulo.endpoint.url.UrlResolverManager;
import consulo.endpoint.url.UrlTargetInfo;
import consulo.language.psi.util.PartiallyKnownString;
import consulo.project.Project;
import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.Set;
import java.util.function.Supplier;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

public final class UrlPathContextUtil {
    static final ThreadLocal<Boolean> CAN_BUILD_URL_CONTEXT = ThreadLocal.withInitial(() -> true);

    private UrlPathContextUtil() {
    }

    /**
     * @see consulo.endpoint.impl.internal.url.reference.UrlPathReferenceUnifiedPomTarget#getResolvedTargets
     */
    @RequiredReadAction
    public static Set<UrlTargetInfo> resolveTargets(UrlPathContext context, Project project) {
        UrlResolverManager urlResolver = UrlResolverManager.getInstance(project);

        Iterable<UrlTargetInfo> resolved = () -> StreamSupport.stream(context.getResolveRequests().spliterator(), false)
            .flatMap(request -> asStream(urlResolver.resolve(request)))
            .iterator();
        return UrlPathModelUtil.filterBestUrlPathMatches(resolved);
    }

    private static <T> Stream<T> asStream(Iterable<T> iterable) {
        return StreamSupport.stream(iterable.spliterator(), false);
    }

    public static UrlPathContext applyFromParsed(UrlPathContext context, UrlPksParser.ParsedPksUrl parsedUrl) {
        return applyFromParsed(context, parsedUrl, true);
    }

    public static UrlPathContext applyFromParsed(UrlPathContext context, UrlPksParser.ParsedPksUrl parsedUrl, boolean scheme) {
        return applyFromParsed(context, parsedUrl, scheme, true);
    }

    public static UrlPathContext applyFromParsed(
        UrlPathContext context,
        UrlPksParser.ParsedPksUrl parsedUrl,
        boolean scheme,
        boolean auth
    ) {
        return applyFromParsed(context, parsedUrl, scheme, auth, true);
    }

    public static UrlPathContext applyFromParsed(
        UrlPathContext context,
        UrlPksParser.ParsedPksUrl parsedUrl,
        boolean scheme,
        boolean auth,
        boolean path
    ) {
        UrlPathContext result = context;
        @Nullable PartiallyKnownString authority = parsedUrl.getAuthority();
        @Nullable String authorityKnown = authority != null ? authority.getValueIfKnown() : null;
        @Nullable String authorityValue = authorityKnown != null && !authorityKnown.isEmpty() ? authorityKnown : null;
        @Nullable PartiallyKnownString parsedScheme = parsedUrl.getScheme();
        @Nullable String schemeKnown = parsedScheme != null ? parsedScheme.getValueIfKnown() : null;
        @Nullable String schemeValue =
            schemeKnown != null && !schemeKnown.isEmpty() && !schemeKnown.equals(authorityValue) ? schemeKnown : null;
        UrlPath urlPath = parsedUrl.getUrlPath();

        if (scheme && schemeValue != null) {
            result = result.withSchemes(Set.of(schemeValue));
        }
        if (auth && authorityValue != null) {
            result = result.withAuthorities(Set.of(authorityValue));
        }
        if (path) {
            result = result.subContext(urlPath);
        }

        return result;
    }

    @RequiredReadAction
    public static UrlPathContext configureFromStringHeuristically(
        UrlPathContext context,
        @Nullable String url,
        FrameworkUrlPathSpecification specification
    ) {
        if (url == null) {
            return context;
        }
        PartiallyKnownString pks = new PartiallyKnownString(url);
        UrlPathContext pathContext = context;
        UrlPksParser.ParsedPksUrl fullUrl = specification.getParser().parseFullUrl(pks);
        @Nullable PartiallyKnownString authority = fullUrl.getAuthority();
        @Nullable String authorityValue = authority != null ? authority.getValueIfKnown() : null;
        if (authorityValue != null && !authorityValue.isEmpty()) {
            pathContext = applyFromParsed(pathContext, fullUrl);
        }
        else {
            pathContext = pathContext.subContext(specification.getParser().parseUrlPath(pks).getUrlPath());
        }
        return pathContext;
    }

    public static <T> T forbidExpensiveUrlContext(Supplier<T> call) {
        Boolean prev = CAN_BUILD_URL_CONTEXT.get();
        try {
            CAN_BUILD_URL_CONTEXT.set(false);
            return call.get();
        }
        finally {
            CAN_BUILD_URL_CONTEXT.set(prev);
        }
    }

    public static UrlPath chopLeadingEmptyBlock(UrlPath urlPath) {
        List<UrlPath.PathSegment> segments = urlPath.getSegments();
        if (urlPath != UrlPath.EMPTY && !segments.isEmpty() && segments.get(0).isEmpty()) {
            List<UrlPath.PathSegment> newSegments = chopLeadingEmptyBlock(segments);
            return newSegments.isEmpty() ? UrlPath.EMPTY : new UrlPath(newSegments);
        }
        else {
            return urlPath;
        }
    }

    public static UrlPath chopTrailingEmptyBlock(UrlPath urlPath) {
        List<UrlPath.PathSegment> segments = urlPath.getSegments();
        if (urlPath != UrlPath.EMPTY && hasEmptyTrailingBlock(segments)) {
            List<UrlPath.PathSegment> newSegments = chopTrailingEmptyBlock(segments);
            return newSegments.isEmpty() ? UrlPath.EMPTY : new UrlPath(newSegments);
        }
        else {
            return urlPath;
        }
    }

    static List<UrlPath.PathSegment> chopLeadingEmptyBlock(List<UrlPath.PathSegment> segments) {
        return !segments.isEmpty() && segments.get(0).isEmpty() ? segments.subList(1, segments.size()) : segments;
    }

    static List<UrlPath.PathSegment> chopTrailingEmptyBlock(List<UrlPath.PathSegment> segments) {
        return hasEmptyTrailingBlock(segments) ? segments.subList(0, segments.size() - 1) : segments;
    }

    private static boolean hasEmptyTrailingBlock(List<UrlPath.PathSegment> segments) {
        return segments.size() > 1 && segments.get(segments.size() - 1).isEmpty();
    }
}
