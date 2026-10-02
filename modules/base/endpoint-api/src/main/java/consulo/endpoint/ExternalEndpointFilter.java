// Copyright 2000-2025 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.endpoint;

/**
 * Filter for items defined outside the project: e.g., external OpenAPI specifications.
 */
public final class ExternalEndpointFilter implements EndpointFilter {
    public static final ExternalEndpointFilter INSTANCE = new ExternalEndpointFilter();

    private ExternalEndpointFilter() {
    }
}
