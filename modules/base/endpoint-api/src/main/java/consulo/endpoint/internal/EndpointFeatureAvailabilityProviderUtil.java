// Copyright 2000-2025 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.endpoint.internal;

import consulo.endpoint.EndpointFeatureAvailabilityProvider;
import consulo.project.Project;
import consulo.ui.annotation.RequiredUIAccess;

public final class EndpointFeatureAvailabilityProviderUtil {
    private EndpointFeatureAvailabilityProviderUtil() {
    }

    @RequiredUIAccess
    public static boolean isSearchEverywhereAvailableExplicitly(Project project) {
        return project.getExtensionPoint(EndpointFeatureAvailabilityProvider.class)
            .anyMatchSafe(EndpointFeatureAvailabilityProvider::isSearchEverywhereTabAvailable);
    }
}
