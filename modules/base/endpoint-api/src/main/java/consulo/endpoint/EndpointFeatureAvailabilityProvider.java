// Copyright 2000-2025 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.endpoint;

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ExtensionAPI;
import consulo.ui.annotation.RequiredUIAccess;

@ExtensionAPI(ComponentScope.PROJECT)
public interface EndpointFeatureAvailabilityProvider {
    /**
     * Decides whether Search Everywhere should have a separate tab for URL search in this project
     */
    @RequiredUIAccess
    boolean isSearchEverywhereTabAvailable();
}
