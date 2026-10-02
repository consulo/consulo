// Copyright 2000-2025 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.endpoint.impl.internal.url.reference;

import consulo.annotation.access.RequiredReadAction;
import consulo.application.Application;
import consulo.endpoint.impl.internal.util.ReferenceResolveUtil;
import consulo.endpoint.internal.EndpointStringUtil;
import consulo.endpoint.localize.EndpointLocalize;
import consulo.endpoint.url.UrlPath;
import consulo.endpoint.url.UrlPathModelUtil;
import consulo.endpoint.url.UrlResolveRequest;
import consulo.endpoint.url.UrlResolverManager;
import consulo.endpoint.url.UrlTargetInfo;
import consulo.endpoint.url.reference.UrlPathContext;
import consulo.endpoint.url.reference.UrlPathReference;
import consulo.endpoint.url.reference.UrlPathReferenceTarget;
import consulo.language.pom.PomTargetPsiElement;
import consulo.language.psi.NavigatablePsiElement;
import consulo.language.psi.PsiElement;
import consulo.logging.Logger;
import consulo.project.Project;
import consulo.util.lang.lazy.LazyValue;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

public final class UrlPathReferenceUnifiedPomTarget implements UrlPathReferenceTarget {
    private static final Logger LOG = Logger.getInstance(UrlPathReferenceUnifiedPomTarget.class);

    private final UrlPathContext myContext;
    private final boolean myIsAtEnd;
    private final Project myProject;
    private final UrlResolverManager myUrlResolver;
    private final LazyValue<Set<UrlTargetInfo>> myResolvedTargets;
    private final LazyValue<List<UrlPath>> myPathValues;

    @RequiredReadAction
    public UrlPathReferenceUnifiedPomTarget(UrlPathContext context, boolean isAtEnd, Project project) {
        myContext = context;
        myIsAtEnd = isAtEnd;
        myProject = project;

        if (Application.get().isUnitTestMode() && context.getSelfPaths().isEmpty()) {
            LOG.error("UrlPathReferenceUnifiedPomTarget for empty contextProvider should not be created");
        }

        myUrlResolver = UrlResolverManager.getInstance(project);
        myResolvedTargets = ReferenceResolveUtil.lazySynchronousResolve(
            EndpointLocalize.microservicesResolvingReference(),
            this::computeResolvedTargets
        );
        myPathValues = LazyValue.atomicNotNull(() -> {
            List<UrlPath> paths = new ArrayList<>();
            for (UrlResolveRequest request : myContext.getResolveRequests()) {
                paths.add(request.getPath());
            }
            return paths;
        });
    }

    @RequiredReadAction
    public UrlPathReferenceUnifiedPomTarget(UrlPathContext context, Project project) {
        this(context, true, project);
    }

    private Set<UrlTargetInfo> computeResolvedTargets() {
        UrlPathContext urlPathContext =
            myIsAtEnd ? myContext : myContext.subContext(new UrlPath(List.of(UrlPath.PathSegment.Undefined.INSTANCE)));
        Iterable<UrlTargetInfo> resolved = () -> asStream(urlPathContext.getResolveRequests())
            .flatMap(request -> asStream(myUrlResolver.resolve(request)))
            .iterator();
        return UrlPathModelUtil.filterBestUrlPathMatches(resolved);
    }

    private static <T> Stream<T> asStream(Iterable<T> iterable) {
        return StreamSupport.stream(iterable.spliterator(), false);
    }

    @Override
    public UrlPathContext getContext() {
        return myContext;
    }

    @Override
    public Set<UrlTargetInfo> getResolvedTargets() {
        return myResolvedTargets.get();
    }

    public List<UrlPath> getPathValues() {
        return myPathValues.get();
    }

    public <T> Stream<T> mapVariants(BiFunction<UrlResolveRequest, Iterable<UrlTargetInfo>, T> handler) {
        return asStream(myContext.getResolveRequests()).map(request -> handler.apply(request, myUrlResolver.getVariants(request)));
    }

    @Override
    @RequiredReadAction
    public @Nullable NavigatablePsiElement getNavigatablePsiElement() {
        Set<PsiElement> elements = new HashSet<>();
        for (UrlTargetInfo target : getResolvedTargets()) {
            @Nullable PsiElement element = target.resolveToPsiElement();
            if (element != null) {
                elements.add(element);
            }
        }
        if (elements.size() != 1) {
            return null;
        }
        PsiElement nav = elements.iterator().next();
        if (nav instanceof NavigatablePsiElement navigatablePsiElement) {
            return navigatablePsiElement;
        }
        Application application = Application.get();
        if (application.isInternal() || application.isUnitTestMode()) {
            LOG.error("non-navigatable navigation element: " + nav + " resolvedTargets = " + getResolvedTargets());
        }
        return null;
    }

    @Override
    @RequiredReadAction
    public void navigate(boolean requestFocus) {
        @Nullable NavigatablePsiElement element = getNavigatablePsiElement();
        if (element != null) {
            element.navigate(requestFocus);
        }
    }

    @Override
    public @Nullable Object setName(String newName) {
        return null;
    }

    @Override
    @RequiredReadAction
    public boolean canNavigate() {
        @Nullable NavigatablePsiElement element = getNavigatablePsiElement();
        return element != null && element.canNavigate();
    }

    private UrlPath.@Nullable PathSegment getLastPathSegment() {
        List<UrlPath> pathValues = getPathValues();
        if (pathValues.isEmpty()) {
            return null;
        }
        List<UrlPath.PathSegment> segments = pathValues.get(0).getSegments();
        return segments.isEmpty() ? null : segments.get(segments.size() - 1);
    }

    @Override
    public String getName() {
        UrlPath.@Nullable PathSegment lastPathSegment = getLastPathSegment();
        return UrlPath.FULL_PATH_VARIABLE_PRESENTATION.patternMatch(
            lastPathSegment != null ? lastPathSegment : UrlPath.PathSegment.Undefined.INSTANCE
        );
    }

    @Override
    @RequiredReadAction
    public boolean canNavigateToSource() {
        @Nullable NavigatablePsiElement element = getNavigatablePsiElement();
        return element != null && element.canNavigateToSource();
    }

    @Override
    public boolean isValid() {
        return true;
    }

    @Override
    @RequiredReadAction
    public boolean isWritable() {
        if (!(getLastPathSegment() instanceof UrlPath.PathSegment.Exact)) {
            return false;
        }

        for (UrlTargetInfo target : getResolvedTargets()) {
            @Nullable PsiElement element = target.resolveToPsiElement();
            if (element == null) {
                continue;
            }
            PsiElement navigationElement = element.getNavigationElement();
            if (navigationElement == null || !navigationElement.isWritable()) {
                return false;
            }
        }
        return true;
    }

    @Override
    public boolean equals(@Nullable Object other) {
        if (this == other) {
            return true;
        }
        if (other == null || getClass() != other.getClass()) {
            return false;
        }
        UrlPathReferenceUnifiedPomTarget that = (UrlPathReferenceUnifiedPomTarget) other;

        Set<String> aSchemes = collectSchemes(myContext);
        Set<String> bSchemes = collectSchemes(that.myContext);
        if (!UrlPathModelUtil.compatibleSchemes(aSchemes, bSchemes)) {
            return false;
        }

        List<UrlPath> otherPathValues = that.getPathValues();
        for (UrlPath cur : getPathValues()) {
            for (UrlPath it : otherPathValues) {
                if (cur.isCompatibleWith(it)) {
                    return true;
                }
            }
        }
        return false;
    }

    private static Set<String> collectSchemes(UrlPathContext context) {
        Set<String> schemes = new HashSet<>();
        for (UrlResolveRequest request : context.getResolveRequests()) {
            @Nullable String schemeHint = request.getSchemeHint();
            if (schemeHint != null && !EndpointStringUtil.isBlank(schemeHint)) {
                schemes.add(schemeHint);
            }
        }
        return schemes;
    }

    @Override
    public int hashCode() {
        return myContext.getSelfPaths().hashCode();
    }

    @Override
    public String toString() {
        return "URLPathReferenceUnifiedPomTarget(" + myContext.getResolveRequests() + ")";
    }

    @Override
    public PsiElement toElement(UrlPathReference reference) {
        return new UrlTargetInfoFakeElement(myProject, this, reference, myContext.isDeclaration());
    }

    public static @Nullable UrlPathReferenceUnifiedPomTarget getFromPomTargetPsiElement(PsiElement psiElement) {
        if (psiElement instanceof PomTargetPsiElement pomTargetPsiElement
            && pomTargetPsiElement.getTarget() instanceof UrlPathReferenceUnifiedPomTarget target) {
            return target;
        }
        return null;
    }
}
