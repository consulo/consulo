package consulo.endpoint.impl.internal.view.detail;

import consulo.annotation.component.ActionImpl;
import consulo.endpoint.localize.EndpointLocalize;
import consulo.platform.base.icon.PlatformIconGroup;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.action.AnActionEvent;
import consulo.ui.ex.action.DumbAwareToggleAction;

@ActionImpl(id = ToggleEndpointDetailsAction.ID)
public final class ToggleEndpointDetailsAction extends DumbAwareToggleAction {
    public static final String ID = "Endpoints.ToggleDetails";

    public ToggleEndpointDetailsAction() {
        super(
            EndpointLocalize.endpointsDetailsToggleText(),
            EndpointLocalize.endpointsDetailsToggleDescription(),
            PlatformIconGroup.actionsPreviewdetails()
        );
    }

    @Override
    public boolean isSelected(AnActionEvent e) {
        EndpointDetailsPane pane = e.getData(EndpointDetailsPane.KEY);
        return pane != null && pane.isDetailsVisible();
    }

    @Override
    @RequiredUIAccess
    public void setSelected(AnActionEvent e, boolean state) {
        EndpointDetailsPane pane = e.getData(EndpointDetailsPane.KEY);
        if (pane != null) {
            pane.setDetailsVisible(state);
        }
    }

    @Override
    public void update(AnActionEvent e) {
        super.update(e);
        e.getPresentation().setEnabledAndVisible(e.getData(EndpointDetailsPane.KEY) != null);
    }
}
