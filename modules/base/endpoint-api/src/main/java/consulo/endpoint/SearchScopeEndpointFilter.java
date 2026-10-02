// Copyright 2000-2025 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.endpoint;

import consulo.language.psi.scope.GlobalSearchScope;

/**
 * Filter for items defined in a project scope.
 */
public interface SearchScopeEndpointFilter extends EndpointFilter {
    /**
     * Does not include any transitive modules, dependencies, libraries.
     * Should be used for specification-like declarations, e.g., OpenAPI, gRPC.
     */
    GlobalSearchScope getContentSearchScope();

    /**
     * Includes transitive dependencies, such as modules and libraries. Should be used for application-like frameworks that inherit all
     * endpoint handlers from dependency modules, e.g., Spring, Micronaut.
     */
    GlobalSearchScope getTransitiveSearchScope();
}
