// Copyright 2000-2025 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.endpoint.url.reference;

import consulo.annotation.access.RequiredReadAction;
import consulo.document.util.TextRange;
import consulo.endpoint.url.UrlPath;
import consulo.endpoint.url.parameter.QueryParameterNameReference;
import consulo.language.psi.ElementManipulators;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiLanguageInjectionHost;
import consulo.language.psi.PsiReference;
import consulo.language.psi.util.PartiallyKnownString;
import consulo.language.psi.util.StringEntry;
import consulo.util.collection.SmartList;
import consulo.util.io.URLUtil;
import consulo.util.lang.Pair;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Function;

public final class UrlPathReferenceInjector<S> {
    private final UrlPksParser myUrlParser;
    private final Function<S, @Nullable PartiallyKnownString> mySplitRetrieval;

    private Function<S, UrlPathContext> myDefaultRootContextProvider = it -> UrlPathContext.emptyRoot();

    private PathSegmentHandler myPathSegmentHandler = DefaultExactPathSegmentHandler.INSTANCE;

    private @Nullable Consumer<UrlSegmentReference> myNavigationHandler;

    private Function<StringEntry, @Nullable Pair<PsiElement, TextRange>> myAlignToHost = StringEntry::getRangeAlignedToHost;

    private UrlPathReferenceInjector(UrlPksParser urlParser, Function<S, @Nullable PartiallyKnownString> splitRetrieval) {
        myUrlParser = urlParser;
        mySplitRetrieval = splitRetrieval;
    }

    public UrlPksParser getUrlParser() {
        return myUrlParser;
    }

    public Function<S, UrlPathContext> getDefaultRootContextProvider() {
        return myDefaultRootContextProvider;
    }

    public void setDefaultRootContextProvider(Function<S, UrlPathContext> defaultRootContextProvider) {
        myDefaultRootContextProvider = defaultRootContextProvider;
    }

    public Function<StringEntry, @Nullable Pair<PsiElement, TextRange>> getAlignToHost() {
        return myAlignToHost;
    }

    public void setAlignToHost(Function<StringEntry, @Nullable Pair<PsiElement, TextRange>> alignToHost) {
        myAlignToHost = alignToHost;
    }

    public UrlPathReferenceInjector<S> withDefaultRootContextProviderFactory(Function<S, UrlPathContext> factory) {
        myDefaultRootContextProvider = factory;
        return this;
    }

    public UrlPathReferenceInjector<S> withSchemesSupport(List<String> schemes) {
        UrlPathContext provider = UrlPathContext.supportingSchemes(schemes);
        myDefaultRootContextProvider = it -> provider;
        return this;
    }

    public UrlPathReferenceInjector<S> withCustomHostAligner(Function<StringEntry, @Nullable Pair<PsiElement, TextRange>> converter) {
        myAlignToHost = converter;
        return this;
    }

    public UrlPathReferenceInjector<S> withPathSegmentHandler(PathSegmentHandler handler) {
        myPathSegmentHandler = handler;
        return this;
    }

    public UrlPathReferenceInjector<S> withCustomNavigationHandler(Consumer<UrlSegmentReference> navigate) {
        myNavigationHandler = navigate;
        return this;
    }

    public static <S> UrlPathReferenceInjector<S> forPartialStringFrom(
        UrlPksParser urlParser,
        Function<S, @Nullable PartiallyKnownString> retrievalFun
    ) {
        return new UrlPathReferenceInjector<>(urlParser, retrievalFun);
    }

    public static <S> UrlPathReferenceInjector<S> forPartialStringFrom(Function<S, @Nullable PartiallyKnownString> retrievalFun) {
        return forPartialStringFrom(new UrlPksParser(), retrievalFun);
    }

    @RequiredReadAction
    public static boolean hasConsistentFullUrl(PsiReference[] reference) {
        @Nullable PsiReference schemeReference = singleOrNull(reference, SchemeReference.class);
        if (schemeReference == null || schemeReference.getRangeInElement().isEmpty()) {
            return false;
        }
        TextRange schemeRange = schemeReference.getRangeInElement();
        @Nullable PsiReference authReference = singleOrNull(reference, AuthorityReference.class);
        if (authReference == null || authReference.getRangeInElement().isEmpty()) {
            return false;
        }
        TextRange authRange = authReference.getRangeInElement();
        TextRange baseRange = schemeRange.union(authRange);
        for (PsiReference psiReference : reference) {
            if (psiReference instanceof UrlPathReference && psiReference.getRangeInElement().intersectsStrict(baseRange)) {
                return false;
            }
        }
        return true;
    }

    private static @Nullable PsiReference singleOrNull(PsiReference[] references, Class<? extends PsiReference> type) {
        @Nullable PsiReference single = null;
        for (PsiReference reference : references) {
            if (type.isInstance(reference)) {
                if (single != null) {
                    return null;
                }
                single = reference;
            }
        }
        return single;
    }

    @RequiredReadAction
    public PsiReference[] buildFullUrlReference(S uElement, PsiElement host) {
        @Nullable PartiallyKnownString pkwString = mySplitRetrieval.apply(uElement);
        if (pkwString == null) {
            return PsiReference.EMPTY_ARRAY;
        }
        TextRange hostTextRange = ElementManipulators.getValueTextRange(host);

        UrlPksParser.ParsedPksUrl parsedUrl = myUrlParser.parseFullUrl(pkwString);

        @Nullable PartiallyKnownString scheme = parsedUrl.getScheme();
        if (scheme == null) {
            return PsiReference.EMPTY_ARRAY;
        }
        return UrlPathContextUtil.forbidExpensiveUrlContext(() -> {
            List<PsiReference> result = new SmartList<>();
            @Nullable TextRange rangeInHost = scheme.getRangeInHost(host);
            if (rangeInHost != null && host instanceof PsiLanguageInjectionHost injectionHost) {
                UrlPathContext contextRoot = myDefaultRootContextProvider.apply(uElement);
                result.add(new SchemeReference(scheme.getValueIfKnown(), contextRoot.getSchemes(), injectionHost, rangeInHost));
            }

            @Nullable PartiallyKnownString authority = parsedUrl.getAuthority();
            if (authority != null) {
                @Nullable AuthorityReference authorityReference =
                    createAuthorityReference(host, hostTextRange, scheme, authority, parsedUrl);

                if (authorityReference != null) {
                    result.add(authorityReference);
                }

                UrlPathContext rootContextFromParsedBaseUrl =
                    UrlPathContextUtil.applyFromParsed(myDefaultRootContextProvider.apply(uElement), parsedUrl, true, true, false);

                List<PsiReference> urlPathReferences;
                if (!parsedUrl.getSlashesSplit().isEmpty()) {
                    urlPathReferences =
                        buildReferencesForGivenSplit(parsedUrl.getSlashesSplit(), rootContextFromParsedBaseUrl, uElement, host);
                }
                else if (!result.isEmpty()) {
                    TextRange lastRef = result.get(result.size() - 1).getRangeInElement();
                    urlPathReferences = List.of(
                        new UrlPathReference(
                            rootContextFromParsedBaseUrl,
                            host,
                            new TextRange(lastRef.getEndOffset(), lastRef.getEndOffset()),
                            true,
                            false,
                            myPathSegmentHandler,
                            myNavigationHandler
                        )
                    );
                }
                else {
                    urlPathReferences = List.of();
                }

                result.addAll(urlPathReferences);
                result.addAll(buildQueryParamReferences(parsedUrl.getQueryParameters(), urlPathReferences, uElement, host));
            }
            return result.toArray(PsiReference.EMPTY_ARRAY);
        });
    }

    @RequiredReadAction
    private @Nullable AuthorityReference createAuthorityReference(
        PsiElement host,
        TextRange hostTextRange,
        PartiallyKnownString scheme,
        PartiallyKnownString authority,
        UrlPksParser.ParsedPksUrl parsedUrl
    ) {
        if (!(host instanceof PsiLanguageInjectionHost injectionHost)) {
            return null;
        }
        @Nullable TextRange authorityRangeInHost = authority.getRangeInHost(host);
        if (authorityRangeInHost != null) {
            @Nullable TextRange intersection = hostTextRange.intersection(authorityRangeInHost);
            return new AuthorityReference(
                authority.getValueIfKnown(),
                injectionHost,
                intersection != null ? intersection : hostTextRange,
                myNavigationHandler
            );
        }
        if (!isNullOrEmpty(scheme.getValueIfKnown())
            && isNullOrEmpty(authority.getValueIfKnown())
            && parsedUrl.getSlashesSplit().isEmpty()) {
            return new AuthorityReference(
                authority.getValueIfKnown(),
                injectionHost,
                new TextRange(hostTextRange.getEndOffset(), hostTextRange.getEndOffset()),
                myNavigationHandler
            );
        }
        return null;
    }

    private static boolean isNullOrEmpty(@Nullable String value) {
        return value == null || value.isEmpty();
    }

    @RequiredReadAction
    public MappedReferences buildReferences(S source) {
        @Nullable PartiallyKnownString pkwString = mySplitRetrieval.apply(source);
        if (pkwString == null) {
            return MappedReferences.EMPTY;
        }
        UrlPksParser.ParsedPksUrlPath parsedUrl = myUrlParser.parseUrlPath(pkwString);

        return new MappedReferences() {
            private @Nullable UrlPathContext myCurrentRootContextProvider = null;

            private UrlPathContext rootContext() {
                @Nullable UrlPathContext current = myCurrentRootContextProvider;
                return current != null ? current : myDefaultRootContextProvider.apply(source);
            }

            @Override
            public MappedReferences withRootContextProvider(UrlPathContext rooContextProvider) {
                myCurrentRootContextProvider = rooContextProvider;
                return this;
            }

            @Override
            @RequiredReadAction
            public PsiReference[] forPsiElement(PsiElement host) {
                return UrlPathContextUtil.forbidExpensiveUrlContext(() -> {
                    List<PsiReference> urlPathReferences;
                    if (!parsedUrl.getSlashesSplit().isEmpty()) {
                        urlPathReferences = buildReferencesForGivenSplit(parsedUrl.getSlashesSplit(), rootContext(), source, host);
                    }
                    else {
                        urlPathReferences = List.of();
                    }

                    List<PsiReference> result = new SmartList<>();
                    result.addAll(urlPathReferences);
                    result.addAll(buildQueryParamReferences(parsedUrl.getQueryParameters(), urlPathReferences, source, host));

                    return result.toArray(PsiReference.EMPTY_ARRAY);
                });
            }
        };
    }

    @RequiredReadAction
    public boolean hasCompleteScheme(S uElement) {
        @Nullable PartiallyKnownString pks = mySplitRetrieval.apply(uElement);
        if (pks == null) {
            return false;
        }
        UrlPksParser.ParsedPksUrl parsed = myUrlParser.parseFullUrl(pks);
        @Nullable PartiallyKnownString scheme = parsed.getScheme();
        @Nullable String schemeValue = scheme != null ? scheme.getValueIfKnown() : null;
        if (schemeValue == null) {
            return false;
        }
        return schemeValue.endsWith("://");
    }

    @RequiredReadAction
    public PsiReference[] buildAbsoluteOrRelativeReferences(S uElement, PsiElement host) {
        PsiReference[] fullReferences = buildFullUrlReference(uElement, host);
        if (hasConsistentFullUrl(fullReferences) || hasCompleteScheme(uElement)) {
            return fullReferences;
        }

        PsiReference[] contextReferences = buildReferences(uElement).forPsiElement(host);
        PsiReference[] result = new PsiReference[fullReferences.length + contextReferences.length];
        System.arraycopy(fullReferences, 0, result, 0, fullReferences.length);
        System.arraycopy(contextReferences, 0, result, fullReferences.length, contextReferences.length);
        return result;
    }

    @RequiredReadAction
    private List<PsiReference> buildQueryParamReferences(
        List<UrlPksParser.QueryParameter> queryParameters,
        List<PsiReference> urlPathReferences,
        S source,
        PsiElement host
    ) {
        if (queryParameters.isEmpty() || !(host instanceof PsiLanguageInjectionHost injectionHost)) {
            return List.of();
        }

        List<PsiReference> result = new SmartList<>();
        @Nullable PsiReference lastReference = urlPathReferences.isEmpty() ? null : urlPathReferences.get(urlPathReferences.size() - 1);
        UrlPathContext urlPathContext = lastReference instanceof UrlPathReference urlPathReference
            ? urlPathReference.getContext()
            : myDefaultRootContextProvider.apply(source);

        for (UrlPksParser.QueryParameter queryParameter : queryParameters) {
            @Nullable TextRange nameRangeInHost = null;
            for (StringEntry segment : queryParameter.getName().getSegments()) {
                if (!Objects.equals(segment.getHost(), host)) {
                    continue;
                }
                @Nullable Pair<PsiElement, TextRange> aligned = myAlignToHost.apply(segment);
                if (aligned != null) {
                    nameRangeInHost = aligned.getSecond();
                    break;
                }
            }
            if (nameRangeInHost != null) {
                result.add(new QueryParameterNameReference(urlPathContext, injectionHost, nameRangeInHost));
            }
        }

        return result;
    }

    @RequiredReadAction
    public UrlPath toUrlPath(S source) {
        @Nullable PartiallyKnownString sourceString = mySplitRetrieval.apply(source);
        if (sourceString == null) {
            return UrlPath.EMPTY;
        }
        int schemePosition = sourceString.findIndexOfInKnown(URLUtil.SCHEME_SEPARATOR);
        if (schemePosition != -1) {
            return myUrlParser.parseFullUrl(sourceString).getUrlPath();
        }
        else {
            return myUrlParser.parseUrlPath(sourceString).getUrlPath();
        }
    }

    @RequiredReadAction
    private List<PsiReference> buildReferencesForGivenSplit(
        List<PartiallyKnownString> slashesSplit,
        @Nullable UrlPathContext rootContextProvider,
        S source,
        PsiElement host
    ) {
        Map<PsiElement, List<Pair<TextRange, UrlPath>>> hostsUrlsMap = mapHostRangesToPathFirTree(this, slashesSplit);
        @Nullable List<Pair<TextRange, UrlPath>> currentHostUrls = hostsUrlsMap.get(host);
        if (currentHostUrls == null || currentHostUrls.isEmpty()) {
            return List.of();
        }
        int maxSegments = -1;
        for (Pair<TextRange, UrlPath> pair : currentHostUrls) {
            maxSegments = Math.max(maxSegments, pair.getSecond().getSegments().size());
        }
        TextRange valueTextRange = ElementManipulators.getValueTextRange(host);
        List<PsiReference> result = new ArrayList<>(currentHostUrls.size());
        for (Pair<TextRange, UrlPath> pair : currentHostUrls) {
            TextRange range = pair.getFirst();
            UrlPath paths = pair.getSecond();
            UrlPathContext contextProvider =
                rootContextProvider != null ? rootContextProvider : myDefaultRootContextProvider.apply(source);
            @Nullable TextRange intersection = valueTextRange.intersection(range);
            result.add(new UrlPathReference(
                contextProvider.subContext(paths),
                host,
                intersection != null ? intersection : valueTextRange,
                paths.getSegments().size() == maxSegments,
                false,
                myPathSegmentHandler,
                myNavigationHandler
            ));
        }
        return result;
    }

    @RequiredReadAction
    private static Map<PsiElement, List<Pair<TextRange, UrlPath>>> mapHostRangesToPathFirTree(
        UrlPathReferenceInjector<?> injector,
        List<PartiallyKnownString> slashesSplit
    ) {
        List<UrlPath.PathSegment> pathSegments = new ArrayList<>(slashesSplit.size());
        for (PartiallyKnownString pks : slashesSplit) {
            pathSegments.add(injector.getUrlParser().pksPathSegment(pks));
        }
        Map<PsiElement, List<Pair<TextRange, UrlPath>>> result = new LinkedHashMap<>();
        for (int i = 0; i < pathSegments.size(); i++) {
            List<UrlPath.PathSegment> path = pathSegments.subList(0, i + 1);
            for (Map.Entry<PsiElement, TextRange> entry : pksRangeInHostForFirTree(injector, slashesSplit.get(i)).entrySet()) {
                result.computeIfAbsent(entry.getKey(), k -> new ArrayList<>()).add(Pair.create(entry.getValue(), new UrlPath(path)));
            }
        }
        return result;
    }

    @RequiredReadAction
    private static Map<PsiElement, TextRange> pksRangeInHostForFirTree(UrlPathReferenceInjector<?> injector, PartiallyKnownString pks) {
        Map<PsiElement, List<TextRange>> grouped = new LinkedHashMap<>();
        for (StringEntry segment : pks.getSegments()) {
            @Nullable Pair<PsiElement, TextRange> aligned = injector.getAlignToHost().apply(segment);
            if (aligned != null) {
                grouped.computeIfAbsent(aligned.getFirst(), k -> new ArrayList<>()).add(aligned.getSecond());
            }
        }
        Map<PsiElement, TextRange> result = new LinkedHashMap<>();
        for (Map.Entry<PsiElement, List<TextRange>> entry : grouped.entrySet()) {
            PsiElement host = entry.getKey();
            TextRange valueTextRange = ElementManipulators.getValueTextRange(host);
            TextRange unitedRange = entry.getValue().get(0);
            @Nullable TextRange asRangeInHost = valueTextRange.intersection(unitedRange);

            if (asRangeInHost != null) {
                result.put(host, asRangeInHost);
            }
        }
        return result;
    }
}
