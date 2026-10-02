// Copyright 2000-2025 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.endpoint;

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.TopicAPI;
import org.jspecify.annotations.Nullable;

@TopicAPI(ComponentScope.PROJECT)
public interface EndpointViewOpener {
    String ENDPOINTS_TOOLWINDOW_ID = "Endpoints";
    String ENDPOINTS_CONTEXT_MENU_PLACE = "popup@EndpointsContextMenu";
    String ENDPOINTS_FILTER_TOOLBAR_PLACE = "EndpointsFilterToolbar";

    void showEndpoints(@Nullable String filter);

    void showEndpoints(@Nullable String module, @Nullable String framework, @Nullable String filter);
}
