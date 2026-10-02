package consulo.endpoint.impl.internal.view;

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.TopicImpl;
import consulo.endpoint.EndpointViewOpener;
import consulo.project.Project;
import consulo.project.ui.wm.ToolWindowManager;
import consulo.ui.ex.toolWindow.ToolWindow;
import jakarta.inject.Inject;
import org.jspecify.annotations.Nullable;

@TopicImpl(ComponentScope.PROJECT)
public final class EndpointViewOpenerImpl implements EndpointViewOpener {
    private final Project myProject;

    @Inject
    public EndpointViewOpenerImpl(Project project) {
        myProject = project;
    }

    @Override
    public void showEndpoints(@Nullable String filter) {
        showEndpoints(null, null, filter);
    }

    @Override
    public void showEndpoints(@Nullable String module, @Nullable String framework, @Nullable String filter) {
        if (myProject.isDisposed()) {
            return;
        }

        myProject.getUIAccess().giveIfNeed(() -> {
            if (myProject.isDisposed()) {
                return;
            }

            ToolWindow toolWindow = ToolWindowManager.getInstance(myProject).getToolWindow(ENDPOINTS_TOOLWINDOW_ID);
            if (toolWindow == null) {
                return;
            }

            toolWindow.activate(() -> EndpointViewManager.getInstance(myProject).showEndpoints(module, framework, filter));
        });
    }
}
