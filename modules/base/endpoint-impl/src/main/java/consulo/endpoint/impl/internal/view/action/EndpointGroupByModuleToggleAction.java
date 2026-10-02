package consulo.endpoint.impl.internal.view.action;

import consulo.annotation.component.ActionImpl;
import consulo.endpoint.EndpointProjectModel;
import consulo.endpoint.impl.internal.view.EndpointViewManager;
import consulo.endpoint.localize.EndpointLocalize;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.action.AnActionEvent;
import consulo.ui.ex.action.DumbAwareToggleAction;
import consulo.ui.ex.action.Presentation;

@ActionImpl(id = EndpointGroupByModuleToggleAction.ID)
public final class EndpointGroupByModuleToggleAction extends DumbAwareToggleAction {
    public static final String ID = "Endpoints.GroupByModule";

    public EndpointGroupByModuleToggleAction() {
        super(
            EndpointLocalize.endpointsViewGroupByModuleShort(),
            EndpointLocalize.endpointsViewGroupByModuleFull(),
            null
        );
    }

    @Override
    public boolean isSelected(AnActionEvent e) {
        EndpointViewManager manager = EndpointViewManager.getInstance(e);
        return manager != null && manager.isGroupByModule();
    }

    @Override
    @RequiredUIAccess
    public void setSelected(AnActionEvent e, boolean state) {
        EndpointViewManager manager = EndpointViewManager.getInstance(e);
        if (manager != null) {
            manager.setGroupByModule(state);
        }
    }

    @Override
    public void update(AnActionEvent e) {
        super.update(e);

        Presentation presentation = e.getPresentation();
        EndpointViewManager manager = EndpointViewManager.getInstance(e);
        presentation.setEnabled(manager != null);
        if (manager != null) {
            EndpointProjectModel model = manager.getProjectModel();
            presentation.setText(model.getGroupByModuleTitleShort());
            presentation.setDescription(model.getGroupByModuleTitleFull());
        }
    }
}
