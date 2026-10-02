// Copyright 2000-2025 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.endpoint;

import consulo.project.Project;
import consulo.project.ui.wm.ToolWindowManager;
import consulo.ui.ex.toolWindow.ToolWindow;
import org.jspecify.annotations.Nullable;

public final class EndpointViewOpenerUtil {
    private EndpointViewOpenerUtil() {
    }

    public static boolean isAvailable(Project project) {
        ToolWindow toolWindow = ToolWindowManager.getInstance(project).getToolWindow(EndpointViewOpener.ENDPOINTS_TOOLWINDOW_ID);
        return toolWindow != null && toolWindow.isAvailable();
    }

    public static void showAllEndpoints(Project project) {
        showEndpointsWithFilter(project, null);
    }

    public static void showEndpointsWithFilter(
        Project project,
        @Nullable String module,
        @Nullable String framework,
        @Nullable String filter
    ) {
        project.getMessageBus().syncPublisher(EndpointViewOpener.class).showEndpoints(module, framework, filter);
    }

    public static void showEndpointsWithFilter(Project project, @Nullable String filter) {
        project.getMessageBus().syncPublisher(EndpointViewOpener.class).showEndpoints(filter);
    }
}
