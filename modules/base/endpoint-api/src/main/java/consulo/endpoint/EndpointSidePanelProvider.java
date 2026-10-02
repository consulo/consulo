// Copyright 2000-2025 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.endpoint;

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ExtensionAPI;
import consulo.ui.annotation.RequiredUIAccess;
import org.jspecify.annotations.Nullable;

/**
 * Represents a custom details tab in the Endpoints tool window for selected endpoints.
 */
@ExtensionAPI(ComponentScope.PROJECT)
public interface EndpointSidePanelProvider {
    @RequiredUIAccess
    @Nullable EndpointSidePanel create();
}
