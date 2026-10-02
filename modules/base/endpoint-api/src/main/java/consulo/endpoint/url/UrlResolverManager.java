// Copyright 2000-2025 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.endpoint.url;

import consulo.annotation.access.RequiredReadAction;
import consulo.application.Application;
import consulo.logging.Logger;
import consulo.project.Project;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

public final class UrlResolverManager {
    private static final Logger LOG = Logger.getInstance(UrlResolverManager.class);

    private final List<UrlResolver> myAll;

    private final List<UrlResolver> myMeaningfulResolvers;

    public UrlResolverManager(Project project) {
        List<UrlResolver> all = new ArrayList<>();
        project.getExtensionPoint(UrlResolverFactory.class).forEach(factory -> {
            UrlResolver resolver = factory.forProject();
            if (resolver != null) {
                all.add(resolver);
            }
        });
        myAll = all;
        List<UrlResolver> meaningfulResolvers = new ArrayList<>();
        for (UrlResolver resolver : myAll) {
            if (resolver.getClass().getAnnotation(HelperUrlResolver.class) == null) {
                meaningfulResolvers.add(resolver);
            }
        }
        myMeaningfulResolvers = meaningfulResolvers;
    }

    public List<UrlResolver> getMeaningfulResolvers() {
        return myMeaningfulResolvers;
    }

    /**
     * @param schema schema string with {@code ://} in the end, like {@code http://} or {@code wss://}.
     * If not specified, then all authorities should be returned.
     */
    public List<Authority.Exact> getAuthorityHints(@Nullable String schema) {
        Set<Authority.Exact> result = new LinkedHashSet<>();
        for (UrlResolver resolver : myAll) {
            result.addAll(resolver.getAuthorityHints(schema));
        }
        return new ArrayList<>(result);
    }

    public List<String> getSupportedSchemes() {
        Set<String> result = new LinkedHashSet<>();
        for (UrlResolver resolver : myAll) {
            result.addAll(resolver.getSupportedSchemes());
        }
        return new ArrayList<>(result);
    }

    private <T> Iterable<T> allIterable(UrlResolveRequest request, Function<UrlResolver, Stream<T>> call) {
        @Nullable String schemeHintValue = request.getSchemeHint();
        List<String> schemeHint = schemeHintValue != null ? List.of(schemeHintValue) : List.of();
        return () -> myAll.stream()
            .flatMap(it -> UrlPathModelUtil.compatibleSchemes(it.getSupportedSchemes(), schemeHint) ? call.apply(it) : Stream.<T>empty())
            .iterator();
    }

    private static <T> Stream<T> asStream(Iterable<T> iterable) {
        return StreamSupport.stream(iterable.spliterator(), false);
    }

    public Iterable<UrlTargetInfo> resolve(
        UrlResolveRequest request,
        BiFunction<UrlResolver, UrlResolveRequest, Iterable<UrlTargetInfo>> action
    ) {
        return allIterable(request, it -> asStream(action.apply(it, request)));
    }

    public Iterable<UrlTargetInfo> resolve(UrlResolveRequest request) {
        return allIterable(request, urlResolver -> {
            Stream<UrlTargetInfo> seq = asStream(urlResolver.resolve(request));
            Application application = Application.get();
            if (!UrlPath.EMPTY.equals(request.getPath()) && (application.isUnitTestMode() || application.isInternal())) {
                return seq.peek(result -> {
                    if (result.getPath() == request.getPath()) {
                        LOG.error(
                            "urlResolver of class " + urlResolver.getClass() + " returned the UrlPath(" + result.getPath() + ") " +
                                "identical to the requested it is suspicious, because UrlResolvers are expected to return UrlPath " +
                                "built from sources, not the requested one"
                        );
                    }
                });
            }
            return seq;
        });
    }

    public Iterable<UrlTargetInfo> getVariants(UrlResolveRequest request) {
        return allIterable(request, resolver -> {
            Set<UrlTargetInfoDistinctKey> seen = new HashSet<>();
            return asStream(resolver.getVariants()).filter(it -> seen.add(new UrlTargetInfoDistinctKey(it)));
        });
    }

    @RequiredReadAction
    public static UrlResolverManager getInstance(Project project) {
        Application.get().assertReadAccessAllowed();

        return new UrlResolverManager(project);
    }

    private static final class UrlTargetInfoDistinctKey {
        private final UrlTargetInfo myTargetInfo;

        private UrlTargetInfoDistinctKey(UrlTargetInfo targetInfo) {
            myTargetInfo = targetInfo;
        }

        @Override
        public boolean equals(@Nullable Object other) {
            if (this == other) {
                return true;
            }
            if (other == null || getClass() != other.getClass()) {
                return false;
            }

            UrlTargetInfoDistinctKey that = (UrlTargetInfoDistinctKey) other;

            if (!myTargetInfo.getMethods().equals(that.myTargetInfo.getMethods())) {
                return false;
            }
            if (!myTargetInfo.getPath().equals(that.myTargetInfo.getPath())) {
                return false;
            }

            return true;
        }

        @Override
        public int hashCode() {
            return Objects.hashCode(myTargetInfo.getPath()) + 31 * Objects.hashCode(myTargetInfo.getMethods());
        }
    }
}
