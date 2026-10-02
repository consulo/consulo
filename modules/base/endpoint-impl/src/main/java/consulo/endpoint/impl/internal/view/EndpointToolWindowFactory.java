package consulo.endpoint.impl.internal.view;

import consulo.annotation.component.ExtensionImpl;
import consulo.application.dumb.DumbAware;
import consulo.endpoint.EndpointProvider;
import consulo.endpoint.EndpointViewOpener;
import consulo.endpoint.icon.EndpointIconGroup;
import consulo.endpoint.impl.internal.view.action.EndpointCompactToggleAction;
import consulo.endpoint.impl.internal.view.action.EndpointGroupByModuleToggleAction;
import consulo.endpoint.impl.internal.view.action.EndpointShowFromLibrariesToggleAction;
import consulo.endpoint.impl.internal.view.action.EndpointShowFromTestsToggleAction;
import consulo.endpoint.localize.EndpointLocalize;
import consulo.localize.LocalizeValue;
import consulo.project.Project;
import consulo.project.ui.wm.ToolWindowFactory;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.action.ActionManager;
import consulo.ui.ex.action.DefaultActionGroup;
import consulo.ui.ex.content.Content;
import consulo.ui.ex.content.ContentFactory;
import consulo.ui.ex.content.ContentManager;
import consulo.ui.ex.toolWindow.ToolWindow;
import consulo.ui.ex.toolWindow.ToolWindowAnchor;
import consulo.ui.image.Image;

@ExtensionImpl
public final class EndpointToolWindowFactory implements ToolWindowFactory, DumbAware {
    @Override
    public String getId() {
        return EndpointViewOpener.ENDPOINTS_TOOLWINDOW_ID;
    }

    @RequiredUIAccess
    @Override
    public void createToolWindowContent(Project project, ToolWindow toolWindow) {
        EndpointView view = new EndpointView(project, toolWindow);

        Content content = ContentFactory.getInstance().createUIContent(view.getComponent(), "", false);
        content.setCloseable(false);
        content.setDisposer(view);

        ContentManager contentManager = toolWindow.getContentManager();
        contentManager.addUiDataProvider(view);
        contentManager.addContent(content);

        ActionManager actionManager = ActionManager.getInstance();
        DefaultActionGroup gearActions = new DefaultActionGroup();
        gearActions.add(actionManager.getAction(EndpointCompactToggleAction.class));
        gearActions.add(actionManager.getAction(EndpointGroupByModuleToggleAction.class));
        gearActions.add(actionManager.getAction(EndpointShowFromLibrariesToggleAction.class));
        gearActions.add(actionManager.getAction(EndpointShowFromTestsToggleAction.class));
        toolWindow.setAdditionalGearActions(gearActions);
    }

    @Override
    public boolean shouldBeAvailable(Project project) {
        return true;
    }

    @Override
    public boolean validate(Project project) {
        return EndpointProvider.hasAnyProviders(project);
    }

    @Override
    public ToolWindowAnchor getAnchor() {
        return ToolWindowAnchor.RIGHT;
    }

    @Override
    public Image getIcon() {
        return EndpointIconGroup.toolwindowEndpoints();
    }

    @Override
    public LocalizeValue getDisplayName() {
        return EndpointLocalize.endpointsToolWindowTitle();
    }
}
