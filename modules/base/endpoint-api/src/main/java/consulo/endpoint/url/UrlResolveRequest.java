// Copyright 2000-2025 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.endpoint.url;

import org.jspecify.annotations.Nullable;

import java.util.Objects;

public final class UrlResolveRequest {
    private final @Nullable String mySchemeHint;
    private final @Nullable String myAuthorityHint;
    private final UrlPath myPath;
    private final @Nullable String myMethod;

    public UrlResolveRequest(@Nullable String schemeHint, @Nullable String authorityHint, UrlPath path) {
        this(schemeHint, authorityHint, path, null);
    }

    public UrlResolveRequest(@Nullable String schemeHint, @Nullable String authorityHint, UrlPath path, @Nullable String method) {
        mySchemeHint = schemeHint;
        myAuthorityHint = authorityHint;
        myPath = path;
        myMethod = method;
    }

    public @Nullable String getSchemeHint() {
        return mySchemeHint;
    }

    public @Nullable String getAuthorityHint() {
        return myAuthorityHint;
    }

    public UrlPath getPath() {
        return myPath;
    }

    public @Nullable String getMethod() {
        return myMethod;
    }

    @Override
    public boolean equals(@Nullable Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof UrlResolveRequest that)) {
            return false;
        }
        return Objects.equals(mySchemeHint, that.mySchemeHint)
            && Objects.equals(myAuthorityHint, that.myAuthorityHint)
            && myPath.equals(that.myPath)
            && Objects.equals(myMethod, that.myMethod);
    }

    @Override
    public int hashCode() {
        int result = Objects.hashCode(mySchemeHint);
        result = 31 * result + Objects.hashCode(myAuthorityHint);
        result = 31 * result + myPath.hashCode();
        result = 31 * result + Objects.hashCode(myMethod);
        return result;
    }

    @Override
    public String toString() {
        return "UrlResolveRequest(schemeHint=" + mySchemeHint +
            ", authorityHint=" + myAuthorityHint +
            ", path=" + myPath +
            ", method=" + myMethod + ")";
    }
}
