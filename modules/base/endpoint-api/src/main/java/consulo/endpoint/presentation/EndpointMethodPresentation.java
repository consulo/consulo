// Copyright 2000-2025 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.endpoint.presentation;

import org.jspecify.annotations.Nullable;

import java.util.List;

/**
 * Provides additional method attribute for endpoint presentation in Endpoints View  if it is applicable for
 * {@link consulo.endpoint.EndpointProvider}.
 */
public interface EndpointMethodPresentation {
    /**
     * @return presentation of endpoint method, e.g. HTTP request method
     */
    @Nullable String getEndpointMethodPresentation();

    /**
     * @return list of HTTP verbs supported by endpoint
     */
    List<String> getEndpointMethods();

    /**
     * @return order of endpoint method for sorting
     */
    int getEndpointMethodOrder();
}
