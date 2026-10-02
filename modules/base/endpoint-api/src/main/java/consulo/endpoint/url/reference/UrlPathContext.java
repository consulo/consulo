// Copyright 2000-2025 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.endpoint.url.reference;

import consulo.application.Application;
import consulo.endpoint.internal.EndpointStringUtil;
import consulo.endpoint.internal.LazyChain;
import consulo.endpoint.url.Authority;
import consulo.endpoint.url.UrlPath;
import consulo.endpoint.url.UrlResolveRequest;
import consulo.endpoint.url.UrlTargetInfo;
import consulo.logging.Logger;
import consulo.util.collection.ContainerUtil;
import consulo.util.lang.lazy.LazyValue;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.StringJoiner;
import java.util.TreeSet;
import java.util.function.Supplier;
import java.util.function.UnaryOperator;

public final class UrlPathContext {
    private static final Logger LOG = Logger.getInstance(UrlPathContext.class);

    private final LazyChain.Immediate<UrlPathContextData> myImmediate;
    private final @Nullable LazyChain<UrlPathContextData> myDelayed;

    private UrlPathContext(LazyChain.Immediate<UrlPathContextData> immediate, @Nullable LazyChain<UrlPathContextData> delayed) {
        myImmediate = immediate;
        myDelayed = delayed;
    }

    private UrlPathContext(UrlPathContextData data) {
        this(new LazyChain.Immediate<>(data), null);
    }

    public UrlPathContext(Iterable<UrlResolveRequest> contexts) {
        this(createData(contexts));
    }

    public UrlPathContext(UrlTargetInfo target) {
        this(createData(target));
    }

    private static UrlPathContextData createData(Iterable<UrlResolveRequest> contexts) {
        Set<String> schemes = new LinkedHashSet<>();
        Set<String> authorities = new LinkedHashSet<>();
        Set<String> methods = new LinkedHashSet<>();
        List<UrlPath> paths = new ArrayList<>();
        Set<UrlPath> seenPaths = new LinkedHashSet<>();
        for (UrlResolveRequest request : contexts) {
            if (request.getSchemeHint() != null) {
                schemes.add(request.getSchemeHint());
            }
        }
        for (UrlResolveRequest request : contexts) {
            if (request.getAuthorityHint() != null) {
                authorities.add(request.getAuthorityHint());
            }
        }
        for (UrlResolveRequest request : contexts) {
            if (request.getMethod() != null) {
                methods.add(request.getMethod());
            }
        }
        for (UrlResolveRequest request : contexts) {
            if (seenPaths.add(request.getPath())) {
                paths.add(request.getPath());
            }
        }
        return new UrlPathContextData(new Info(schemes, authorities, methods, Set.of(), false), paths, null);
    }

    private static UrlPathContextData createData(UrlTargetInfo target) {
        Set<String> authorities = new LinkedHashSet<>();
        for (Authority authority : target.getAuthorities()) {
            if (authority instanceof Authority.Exact exact) {
                authorities.add(exact.getText());
            }
        }
        Info info = new Info(new LinkedHashSet<>(target.getSchemes()), authorities, target.getMethods(), target.getContentTypes(), false);
        return new UrlPathContextData(info, List.of(target.getPath()), null);
    }

    private LazyChain<UrlPathContextData> getGenuineChain() {
        return myDelayed != null ? myDelayed : myImmediate;
    }

    public @Nullable UrlPathContext getParent() {
        @Nullable UrlPathContextData parent = myImmediate.getValue().getParent();
        return parent != null ? parent.getContext() : null;
    }

    private static final class Info {
        private final Set<String> mySchemes;
        private final Set<String> myAuthorities;
        private final Set<String> myMethods;
        private final Set<String> myContentTypes;
        private final boolean myIsDeclaration;
        private final @Nullable Supplier<@Nullable UrlPathContext> myFullUrlComputation;

        private Info(Set<String> schemes, Set<String> authorities, Set<String> methods, Set<String> contentTypes, boolean isDeclaration) {
            this(schemes, authorities, methods, contentTypes, isDeclaration, null);
        }

        private Info(
            Set<String> schemes,
            Set<String> authorities,
            Set<String> methods,
            Set<String> contentTypes,
            boolean isDeclaration,
            @Nullable Supplier<@Nullable UrlPathContext> fullUrlComputation
        ) {
            mySchemes = schemes;
            myAuthorities = authorities;
            myMethods = methods;
            myContentTypes = contentTypes;
            myIsDeclaration = isDeclaration;
            myFullUrlComputation = fullUrlComputation;

            Application application = Application.get();
            if (application.isUnitTestMode() || application.isInternal()) {
                boolean noneBlank = true;
                for (String scheme : schemes) {
                    if (EndpointStringUtil.isBlank(scheme)) {
                        noneBlank = false;
                        break;
                    }
                }
                LOG.assertTrue(noneBlank, "blank scheme looks like an error");
            }
        }

        private Info withSchemes(Set<String> schemes) {
            return new Info(schemes, myAuthorities, myMethods, myContentTypes, myIsDeclaration, myFullUrlComputation);
        }

        private Info withAuthorities(Set<String> authorities) {
            return new Info(mySchemes, authorities, myMethods, myContentTypes, myIsDeclaration, myFullUrlComputation);
        }

        private Info withMethods(Set<String> methods) {
            return new Info(mySchemes, myAuthorities, methods, myContentTypes, myIsDeclaration, myFullUrlComputation);
        }

        private Info withContentTypes(Set<String> contentTypes) {
            return new Info(mySchemes, myAuthorities, myMethods, contentTypes, myIsDeclaration, myFullUrlComputation);
        }

        private Info withDeclaration(boolean isDeclaration) {
            return new Info(mySchemes, myAuthorities, myMethods, myContentTypes, isDeclaration, myFullUrlComputation);
        }

        private Info withFullUrlComputation(@Nullable Supplier<@Nullable UrlPathContext> fullUrlComputation) {
            return new Info(mySchemes, myAuthorities, myMethods, myContentTypes, myIsDeclaration, fullUrlComputation);
        }

        private static Collection<? extends @Nullable String> orNull(Collection<String> collection) {
            if (collection.isEmpty()) {
                return Collections.<@Nullable String>singletonList(null);
            }
            return collection;
        }

        private List<UrlResolveRequest> toUrlResolveRequestsStubs(List<UrlPath> paths) {
            List<UrlResolveRequest> result = new ArrayList<>();
            for (@Nullable String scheme : orNull(mySchemes)) {
                for (@Nullable String authority : orNull(myAuthorities)) {
                    for (@Nullable String method : orNull(myMethods)) {
                        for (UrlPath path : paths) {
                            result.add(new UrlResolveRequest(scheme, authority, path, method));
                        }
                    }
                }
            }
            return result;
        }

        @Override
        public boolean equals(@Nullable Object o) {
            if (this == o) {
                return true;
            }
            if (!(o instanceof Info info)) {
                return false;
            }
            return myIsDeclaration == info.myIsDeclaration
                && mySchemes.equals(info.mySchemes)
                && myAuthorities.equals(info.myAuthorities)
                && myMethods.equals(info.myMethods)
                && myContentTypes.equals(info.myContentTypes)
                && Objects.equals(myFullUrlComputation, info.myFullUrlComputation);
        }

        @Override
        public int hashCode() {
            int result = mySchemes.hashCode();
            result = 31 * result + myAuthorities.hashCode();
            result = 31 * result + myMethods.hashCode();
            result = 31 * result + myContentTypes.hashCode();
            result = 31 * result + Boolean.hashCode(myIsDeclaration);
            result = 31 * result + Objects.hashCode(myFullUrlComputation);
            return result;
        }

        @Override
        public String toString() {
            return "Info(schemes=" + mySchemes +
                ", authorities=" + myAuthorities +
                ", methods=" + myMethods +
                ", contentTypes=" + myContentTypes +
                ", isDeclaration=" + myIsDeclaration + ")";
        }
    }

    private static final class UrlPathContextData {
        private final Info myInfo;
        private final List<UrlPath> myPaths;
        private final @Nullable UrlPathContextData myParent;
        private final LazyValue<UrlPathContext> myContext;

        private UrlPathContextData(Info info, List<UrlPath> paths, @Nullable UrlPathContextData parent) {
            myInfo = info;
            myPaths = paths;
            myParent = parent;
            myContext = LazyValue.notNull(() -> new UrlPathContext(this));

            Application application = Application.get();
            if (application.isUnitTestMode() || application.isInternal()) {
                LOG.assertTrue(!paths.isEmpty(), "paths list should contain at least UrlPath.EMPTY");
            }
        }

        private Info getInfo() {
            return myInfo;
        }

        private List<UrlPath> getPaths() {
            return myPaths;
        }

        private @Nullable UrlPathContextData getParent() {
            return myParent;
        }

        private UrlPathContext getContext() {
            return myContext.get();
        }

        private boolean isEmpty() {
            boolean singleEmpty = myPaths.size() == 1 && UrlPath.EMPTY.equals(myPaths.get(0));
            return singleEmpty && (myParent == null || myParent.isEmpty());
        }

        @Override
        public boolean equals(@Nullable Object o) {
            if (this == o) {
                return true;
            }
            if (!(o instanceof UrlPathContextData that)) {
                return false;
            }
            return myInfo.equals(that.myInfo) && myPaths.equals(that.myPaths) && Objects.equals(myParent, that.myParent);
        }

        @Override
        public int hashCode() {
            int result = myInfo.hashCode();
            result = 31 * result + myPaths.hashCode();
            result = 31 * result + Objects.hashCode(myParent);
            return result;
        }

        @Override
        public String toString() {
            return "UrlPathContextData(info=" + myInfo + ", paths=" + myPaths + ", parent=" + myParent + ")";
        }
    }

    private boolean isEvaluated() {
        return myDelayed == null;
    }

    /**
     * Delays expensive computation until it is really necessary (for instance on {@link UrlPathReference} resolve).
     * @see #getFullyEvaluated
     */
    public UrlPathContext applyOnResolve(UnaryOperator<UrlPathContext> trans) {
        return new UrlPathContext(myImmediate, getGenuineChain().chainLazy(it -> {
            LOG.assertTrue(UrlPathContextUtil.CAN_BUILD_URL_CONTEXT.get(), "expensive transformations shouldn't be called there");
            return trans.apply(it.getContext()).getGenuineChain().getValue();
        }));
    }

    /**
     * Provides full url information for generation from {@link UrlPathContext}
     * <p>
     * This method is a workaround for HttpClient.
     * Right now, we can't provide full url information from a resolved version of {@link UrlPathContext}
     */
    public UrlPathContext computeFullUrlInfo() {
        LOG.assertTrue(UrlPathContextUtil.CAN_BUILD_URL_CONTEXT.get(), "expensive transformations shouldn't be called there");
        @Nullable Supplier<@Nullable UrlPathContext> fullUrlComputation = getInfo().myFullUrlComputation;
        @Nullable UrlPathContext result = fullUrlComputation != null ? fullUrlComputation.get() : null;
        return result != null ? result : this;
    }

    private UrlPathContext update(UnaryOperator<UrlPathContextData> transformation) {
        return new UrlPathContext(myImmediate.chain(transformation), myDelayed != null ? myDelayed.chain(transformation) : null);
    }

    /**
     * Computes all delayed by {@link #applyOnResolve} computations. Could be expensive. Please avoid calling it during highlighting or on EDT.
     * @see UrlPathContextUtil#forbidExpensiveUrlContext
     */
    public UrlPathContext getFullyEvaluated() {
        LOG.assertTrue(UrlPathContextUtil.CAN_BUILD_URL_CONTEXT.get(), "expensive transformations shouldn't be called there");
        return isEvaluated() ? this : getGenuineChain().getValue().getContext();
    }

    @Override
    public String toString() {
        UrlPathContextData data = myImmediate.getValue();
        List<List<UrlPath>> pathsBlocks = new ArrayList<>();
        pathsBlocks.add(data.getPaths());
        for (@Nullable UrlPathContextData parent = data.getParent(); parent != null; parent = parent.getParent()) {
            pathsBlocks.add(parent.getPaths());
        }
        StringJoiner blocksJoiner = new StringJoiner(" <- ");
        for (List<UrlPath> block : pathsBlocks) {
            Set<String> sorted = new TreeSet<>();
            for (UrlPath path : block) {
                sorted.add(path.toStringWithStars());
            }
            StringJoiner blockJoiner = new StringJoiner(", ", "[", "]");
            for (String value : sorted) {
                blockJoiner.add(value);
            }
            blocksJoiner.add(blockJoiner.toString());
        }
        return "UrlPathContext(" + blocksJoiner + ", " + data.getInfo() + ")";
    }

    public UrlPathContext subContext(UrlPath subPath) {
        return update(data -> new UrlPathContextData(data.getInfo(), List.of(subPath), data));
    }

    public UrlPathContext subContexts(List<UrlPath> subContexts) {
        if (subContexts.isEmpty()) {
            return this;
        }
        return update(data -> new UrlPathContextData(data.getInfo(), subContexts, data));
    }

    public UrlPathContext withMethod(@Nullable String httpMethod) {
        Set<String> methods = httpMethod != null ? Set.of(httpMethod) : Set.of();
        return update(data -> new UrlPathContextData(data.getInfo().withMethods(methods), data.getPaths(), data.getParent()));
    }

    public UrlPathContext withMethods(Collection<String> httpMethods) {
        Set<String> methods = new LinkedHashSet<>(httpMethods);
        return update(data -> new UrlPathContextData(data.getInfo().withMethods(methods), data.getPaths(), data.getParent()));
    }

    public UrlPathContext withSchemes(Set<String> schemes) {
        return update(data -> new UrlPathContextData(data.getInfo().withSchemes(schemes), data.getPaths(), data.getParent()));
    }

    public UrlPathContext withAuthorities(Set<String> authorities) {
        return update(data -> new UrlPathContextData(data.getInfo().withAuthorities(authorities), data.getPaths(), data.getParent()));
    }

    public UrlPathContext withDeclarationFlag(boolean isDeclaration) {
        return update(data -> new UrlPathContextData(data.getInfo().withDeclaration(isDeclaration), data.getPaths(), data.getParent()));
    }

    public UrlPathContext withPaths(List<UrlPath> paths) {
        List<UrlPath> distinct = new ArrayList<>(new LinkedHashSet<>(paths));
        return update(data -> new UrlPathContextData(data.getInfo(), distinct, data.getParent()));
    }

    public UrlPathContext withContentTypes(Set<String> contentTypes) {
        return update(data -> new UrlPathContextData(data.getInfo().withContentTypes(contentTypes), data.getPaths(), data.getParent()));
    }

    public UrlPathContext withoutLastAppendedText() {
        return update(data -> {
            @Nullable UrlPathContextData parent = data.getParent();
            return new UrlPathContextData(
                data.getInfo(),
                parent != null ? parent.getPaths() : List.of(UrlPath.EMPTY),
                parent != null ? parent.getParent() : null
            );
        });
    }

    public UrlPathContext withFullUrlComputation(Supplier<@Nullable UrlPathContext> fullUrlComputation) {
        return update(data -> new UrlPathContextData(
            data.getInfo().withFullUrlComputation(fullUrlComputation),
            data.getPaths(),
            data.getParent()
        ));
    }

    public boolean isEmpty() {
        return myImmediate.getValue().isEmpty();
    }

    public List<UrlPath> getSelfPaths() {
        return myImmediate.getValue().getPaths();
    }

    private Info getInfo() {
        return myImmediate.getValue().getInfo();
    }

    public List<String> getAuthorities() {
        return new ArrayList<>(getInfo().myAuthorities);
    }

    public List<String> getSchemes() {
        return new ArrayList<>(getInfo().mySchemes);
    }

    public List<String> getMethods() {
        return new ArrayList<>(getInfo().myMethods);
    }

    public boolean isDeclaration() {
        return getInfo().myIsDeclaration;
    }

    public Set<String> getContentTypes() {
        return getInfo().myContentTypes;
    }

    public Iterable<UrlResolveRequest> getResolveRequests() {
        UrlPathContextData data = getFullyEvaluated().myImmediate.getValue();
        List<List<UrlPath>> pathBlocks = new ArrayList<>();
        for (@Nullable UrlPathContextData current = data; current != null; current = current.getParent()) {
            pathBlocks.add(current.getPaths());
        }
        Collections.reverse(pathBlocks);

        List<UrlPath> paths = pathBlocks.get(0);
        for (int i = 1; i < pathBlocks.size(); i++) {
            List<UrlPath> subs = pathBlocks.get(i);
            List<UrlPath> reduced = new ArrayList<>();
            for (UrlPath root : paths) {
                for (UrlPath subSegments : subs) {
                    reduced.add(new UrlPath(ContainerUtil.concat(
                        UrlPathContextUtil.chopTrailingEmptyBlock(root.getSegments()),
                        UrlPathContextUtil.chopLeadingEmptyBlock(subSegments.getSegments())
                    )));
                }
            }
            paths = reduced;
        }

        return data.getInfo().toUrlResolveRequestsStubs(paths);
    }

    public static UrlPathContext singleContext(@Nullable String scheme, @Nullable String authority, UrlPath urlPath) {
        return new UrlPathContext(List.of(new UrlResolveRequest(scheme, authority, urlPath)));
    }

    public static UrlPathContext supportingSchemes(List<String> schemes) {
        return supportingSchemes(schemes, null);
    }

    public static UrlPathContext supportingSchemes(List<String> schemes, @Nullable String method) {
        if (!schemes.isEmpty()) {
            List<UrlResolveRequest> requests = new ArrayList<>(schemes.size());
            for (String scheme : schemes) {
                requests.add(new UrlResolveRequest(scheme, null, UrlPath.EMPTY, method));
            }
            return new UrlPathContext(requests);
        }
        if (method != null) {
            return emptyRoot().withMethod(method);
        }
        return emptyRoot();
    }

    public static UrlPathContext emptyRoot() {
        return singleContext(null, null, UrlPath.EMPTY);
    }
}
