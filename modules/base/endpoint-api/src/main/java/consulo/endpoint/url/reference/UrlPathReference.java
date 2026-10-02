// Copyright 2000-2025 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.endpoint.url.reference;

import consulo.annotation.access.RequiredReadAction;
import consulo.application.util.RecursionManager;
import consulo.document.util.TextRange;
import consulo.endpoint.internal.HttpReferenceService;
import consulo.endpoint.url.Authority;
import consulo.endpoint.url.UrlPath;
import consulo.endpoint.url.UrlQueryParameter;
import consulo.endpoint.url.UrlResolveRequest;
import consulo.endpoint.url.UrlTargetInfo;
import consulo.language.psi.NavigatablePsiElement;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiElementResolveResult;
import consulo.language.psi.PsiReferenceBase;
import consulo.language.psi.ResolveResult;
import consulo.project.Project;
import consulo.ui.image.Image;
import consulo.util.lang.lazy.LazyValue;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;

public final class UrlPathReference extends PsiReferenceBase.Poly<PsiElement> implements UrlSegmentReference {
    private final UrlPathContext myContext;
    private final boolean myIsAtEnd;
    private final boolean myShouldHaveSlashBefore;
    private final PathSegmentHandler myPathSegmentHandler;
    private final @Nullable Consumer<UrlSegmentReference> myCustomNavigate;
    private final LazyValue<Optional<UrlPathReferenceTarget>> myUnifiedPomTarget;

    public UrlPathReference(UrlPathContext context, PsiElement host, TextRange range, boolean isAtEnd) {
        this(context, host, range, isAtEnd, false);
    }

    public UrlPathReference(UrlPathContext context, PsiElement host, TextRange range, boolean isAtEnd, boolean shouldHaveSlashBefore) {
        this(context, host, range, isAtEnd, shouldHaveSlashBefore, DefaultExactPathSegmentHandler.INSTANCE);
    }

    public UrlPathReference(
        UrlPathContext context,
        PsiElement host,
        TextRange range,
        boolean isAtEnd,
        boolean shouldHaveSlashBefore,
        PathSegmentHandler pathSegmentHandler
    ) {
        this(context, host, range, isAtEnd, shouldHaveSlashBefore, pathSegmentHandler, null);
    }

    public UrlPathReference(
        UrlPathContext context,
        PsiElement host,
        TextRange range,
        boolean isAtEnd,
        boolean shouldHaveSlashBefore,
        PathSegmentHandler pathSegmentHandler,
        @Nullable Consumer<UrlSegmentReference> customNavigate
    ) {
        super(host, range, false);
        myContext = context;
        myIsAtEnd = isAtEnd;
        myShouldHaveSlashBefore = shouldHaveSlashBefore;
        myPathSegmentHandler = pathSegmentHandler;
        myCustomNavigate = customNavigate;
        myUnifiedPomTarget = LazyValue.atomicNotNull(() -> Optional.ofNullable(createUnifiedPomTarget()));
    }

    public UrlPathContext getContext() {
        return myContext;
    }

    public boolean isAtEnd() {
        return myIsAtEnd;
    }

    public boolean getShouldHaveSlashBefore() {
        return myShouldHaveSlashBefore;
    }

    public PathSegmentHandler getPathSegmentHandler() {
        return myPathSegmentHandler;
    }

    public @Nullable Consumer<UrlSegmentReference> getCustomNavigate() {
        return myCustomNavigate;
    }

    @Override
    public String toString() {
        @Nullable String value = RecursionManager.doPreventingRecursion(this, false, this::getValue);
        String valueIfAvailable = value != null ? value : "<recursive-evaluation>";
        Set<UrlPath> contexts = new LinkedHashSet<>();
        for (UrlResolveRequest request : myContext.getResolveRequests()) {
            contexts.add(request.getPath());
        }
        return "URLPathReference(" + valueIfAvailable + ", " + getRangeInElement() + ", contexts = " + new ArrayList<>(contexts) + ")";
    }

    @RequiredReadAction
    private @Nullable UrlPathReferenceTarget createUnifiedPomTarget() {
        if (!myContext.getResolveRequests().iterator().hasNext()) {
            return null;
        }

        return HttpReferenceService.getInstance().createUrlPathTarget(myContext, myIsAtEnd, getElement().getProject());
    }

    public @Nullable UrlPathReferenceTarget getUnifiedPomTarget() {
        return myUnifiedPomTarget.get().orElse(null);
    }

    @Override
    @RequiredReadAction
    public ResolveResult[] multiResolve(boolean incompleteCode) {
        List<PsiElement> elements = new ArrayList<>(1);
        @Nullable UrlPathReferenceTarget target = getUnifiedPomTarget();
        if (target != null) {
            elements.add(target.toElement(this));
        }
        return PsiElementResolveResult.createResults(elements);
    }

    private int normalizedLength(UrlPath path) {
        List<UrlPath.PathSegment> segments = path.getSegments();
        int sub = 0;
        if (sub < segments.size() && segments.get(sub) instanceof UrlPath.PathSegment.Undefined) {
            sub++;
        }
        if (sub < segments.size() && segments.get(sub).isEmpty()) {
            sub++;
        }
        if (segments.size() > 1) {
            int endingEmpty = 0;
            for (int i = segments.size() - 1; i >= 0 && segments.get(i).isEmpty(); i--) {
                endingEmpty++;
            }
            sub += endingEmpty;
        }
        return segments.size() - sub;
    }

    public UrlPath remainingPath(UrlPath currentPath, UrlPath variantPath) {
        List<UrlPath.PathSegment> variantSegments = variantPath.getSegments();
        int chopPrefixAt = Math.min(Math.max(normalizedLength(currentPath), 0), variantSegments.size());
        List<UrlPath.PathSegment> segments = variantSegments.subList(chopPrefixAt, variantSegments.size());
        int indexOfFirstValue = -1;
        for (int i = 0; i < segments.size(); i++) {
            if (!segments.get(i).isEmpty()) {
                indexOfFirstValue = i;
                break;
            }
        }
        if (indexOfFirstValue > 0) {
            return new UrlPath(segments.subList(indexOfFirstValue, segments.size()));
        }
        return new UrlPath(segments);
    }

    @Override
    @RequiredReadAction
    public boolean isReferenceTo(PsiElement element) {
        return HttpReferenceService.getInstance().isReferenceToUrlPathTarget(element) && super.isReferenceTo(element);
    }

    public static NavigatablePsiElement createSearchableElement(Project project, UrlPathContext urlPathContext) {
        return HttpReferenceService.getInstance().createSearchableUrlElement(project, urlPathContext);
    }

    public static @Nullable NavigatablePsiElement createSearchableElement(Project project, UrlTargetInfo targetInfo) {
        UrlPath urlPath = UrlPathContextUtil.chopTrailingEmptyBlock(targetInfo.getPath());
        if (!urlPath.equals(UrlPath.EMPTY)) {
            UrlTargetInfo searchableInfo =
                urlPath.equals(targetInfo.getPath()) ? targetInfo : new UrlTargetInfoWrapper(targetInfo, urlPath);

            UrlPathContext urlPathContext = new UrlPathContext(searchableInfo);
            if (urlPathContext.getResolveRequests().iterator().hasNext()) {
                return createSearchableElement(project, urlPathContext);
            }
        }
        return null;
    }

    public static @Nullable UrlPathReference getFromPomTargetPsiElement(PsiElement psiElement) {
        return HttpReferenceService.getInstance().getUrlFromPomTargetPsi(psiElement);
    }

    private static final class UrlTargetInfoWrapper implements UrlTargetInfo {
        private final UrlTargetInfo myOriginal;
        private final UrlPath myNewPath;

        private UrlTargetInfoWrapper(UrlTargetInfo original, UrlPath newPath) {
            myOriginal = original;
            myNewPath = newPath;
        }

        @Override
        public List<String> getSchemes() {
            return myOriginal.getSchemes();
        }

        @Override
        public List<Authority> getAuthorities() {
            return myOriginal.getAuthorities();
        }

        @Override
        public UrlPath getPath() {
            return myNewPath;
        }

        @Override
        public Image getIcon() {
            return myOriginal.getIcon();
        }

        @Override
        public boolean isDeprecated() {
            return myOriginal.isDeprecated();
        }

        @Override
        public Set<String> getMethods() {
            return myOriginal.getMethods();
        }

        @Override
        public String getSource() {
            return myOriginal.getSource();
        }

        @Override
        public @Nullable PsiElement getDocumentationPsiElement() {
            return myOriginal.getDocumentationPsiElement();
        }

        @Override
        public @Nullable PsiElement resolveToPsiElement() {
            return myOriginal.resolveToPsiElement();
        }

        @Override
        public Iterable<UrlQueryParameter> getQueryParameters() {
            return myOriginal.getQueryParameters();
        }

        @Override
        public Set<String> getContentTypes() {
            return myOriginal.getContentTypes();
        }
    }

    public static final class UrlPathLookupObject {
        private final String myUrl;

        public UrlPathLookupObject(String url) {
            myUrl = url;
        }

        @Override
        public boolean equals(@Nullable Object o) {
            if (this == o) {
                return true;
            }
            if (!(o instanceof UrlPathLookupObject that)) {
                return false;
            }
            return myUrl.equals(that.myUrl);
        }

        @Override
        public int hashCode() {
            return myUrl.hashCode();
        }

        @Override
        public String toString() {
            return myUrl;
        }
    }
}
