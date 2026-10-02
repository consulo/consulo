// Copyright 2000-2025 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.endpoint.impl.internal.url.reference;

import consulo.annotation.access.RequiredReadAction;
import consulo.endpoint.url.Authority;
import consulo.endpoint.url.UrlPath;
import consulo.endpoint.url.UrlResolveRequest;
import consulo.endpoint.url.UrlResolverManager;
import consulo.endpoint.url.UrlTargetInfo;
import consulo.endpoint.url.reference.PathSegmentHandler;
import consulo.endpoint.url.reference.UrlPathReference;
import consulo.language.editor.completion.lookup.LookupElement;
import consulo.project.Project;
import consulo.util.collection.JBIterable;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

public final class UrlTargetInfoFakeElementUtil {
    private UrlTargetInfoFakeElementUtil() {
    }

    private static boolean compatibleMethod(UrlTargetInfo variant, @Nullable String method) {
        if (method == null) {
            return true;
        }
        if (variant.getMethods().isEmpty()) {
            return true;
        }
        return variant.getMethods().contains(method);
    }

    private static @Nullable LookupElement mkLookup(
        UrlPathReference reference,
        UrlResolveRequest context,
        List<UrlPath.PathSegment> exactPrefix,
        List<UrlPath.PathSegment> pathToComplete,
        UrlTargetInfo variant,
        boolean hasSomethingNext
    ) {
        PathSegmentHandler pathSegmentHandler = reference.getPathSegmentHandler();
        return pathSegmentHandler.createLookupElement(
            context,
            exactPrefix,
            pathToComplete,
            variant,
            reference.getShouldHaveSlashBefore() ? "/" : "",
            reference.isAtEnd() && hasSomethingNext ? "/" : "",
            hasSomethingNext
        );
    }

    @RequiredReadAction
    public static Iterator<LookupElement> getVariantsIterator(UrlPathReference reference) {
        Set<List<UrlPath.PathSegment>> knownPrefixes = new HashSet<>();

        if (!(reference.getUnifiedPomTarget() instanceof UrlPathReferenceUnifiedPomTarget unifiedPomTarget)) {
            return Collections.emptyIterator();
        }

        Stream<JBIterable<LookupElement>> variantsPerRequest = unifiedPomTarget.mapVariants(
            (context, variants) -> JBIterable.from(variants).flatMap(variant -> variantLookups(reference, knownPrefixes, context, variant))
        );
        Iterable<JBIterable<LookupElement>> requests = variantsPerRequest::iterator;
        return JBIterable.from(requests).flatMap(it -> it).iterator();
    }

    @RequiredReadAction
    private static List<LookupElement> variantLookups(
        UrlPathReference reference,
        Set<List<UrlPath.PathSegment>> knownPrefixes,
        UrlResolveRequest context,
        UrlTargetInfo variant
    ) {
        if (!compatibleMethod(variant, context.getMethod())) {
            return List.of();
        }

        UrlPath pathToMatch;
        if (!reference.getValue().isEmpty()) {
            List<UrlPath.PathSegment> segments = context.getPath().getSegments();
            pathToMatch = new UrlPath(segments.subList(0, Math.max(segments.size() - 1, 0)));
        }
        else {
            pathToMatch = context.getPath();
        }

        if (!pathToMatch.canBePrefixFor(variant.getPath())) {
            return List.of();
        }
        UrlPath pathToComplete = reference.remainingPath(pathToMatch, variant.getPath());
        List<UrlPath.PathSegment> exactPrefix = reference.getPathSegmentHandler().getExactPrefix(pathToComplete);

        knownPrefixes.add(exactPrefix);
        List<List<UrlPath.PathSegment>> prefixes = new ArrayList<>();
        if (reference.isAtEnd()) {
            for (int i = 0; i < exactPrefix.size(); i++) {
                List<UrlPath.PathSegment> prefix = exactPrefix.subList(0, i);
                if (knownPrefixes.add(prefix)) {
                    prefixes.add(prefix);
                }
            }
        }
        else if (!exactPrefix.isEmpty()) {
            prefixes.add(exactPrefix.subList(0, 1));
        }

        List<LookupElement> results = new ArrayList<>(prefixes.size() + 1);
        for (List<UrlPath.PathSegment> prefix : prefixes) {
            @Nullable LookupElement lookup = mkLookup(reference, context, prefix, pathToComplete.getSegments(), variant, true);
            if (lookup != null) {
                results.add(lookup);
            }
        }
        if (reference.isAtEnd()) {
            @Nullable LookupElement lookup = mkLookup(
                reference,
                context,
                exactPrefix,
                pathToComplete.getSegments(),
                variant,
                exactPrefix.size() != pathToComplete.getSegments().size()
            );
            if (lookup != null) {
                results.add(lookup);
            }
        }
        return results;
    }

    @RequiredReadAction
    public static List<AuthorityPomTarget> getAvailableAuthorities(Project project, @Nullable String schema) {
        List<AuthorityPomTarget> result = new ArrayList<>();
        for (Authority.Exact authority : UrlResolverManager.getInstance(project).getAuthorityHints(schema)) {
            result.add(new AuthorityPomTarget(authority.getText()));
        }
        return result;
    }
}
