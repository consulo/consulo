package consulo.endpoint.impl.internal.view.action;

import consulo.annotation.component.ActionImpl;
import consulo.endpoint.impl.internal.view.EndpointViewManager;
import consulo.endpoint.localize.EndpointLocalize;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.action.AnActionEvent;
import consulo.ui.ex.action.DumbAwareToggleAction;

@ActionImpl(id = EndpointShowFromTestsToggleAction.ID)
public final class EndpointShowFromTestsToggleAction extends DumbAwareToggleAction {
    public static final String ID = "Endpoints.ShowFromTests";

    public EndpointShowFromTestsToggleAction() {
        super(EndpointLocalize.endpointsViewShowFromTests(), EndpointLocalize.endpointsViewShowFromTests(), null);
    }

    @Override
    public boolean isSelected(AnActionEvent e) {
        EndpointViewManager manager = EndpointViewManager.getInstance(e);
        return manager != null && manager.isFromTests();
    }

    @Override
    @RequiredUIAccess
    public void setSelected(AnActionEvent e, boolean state) {
        EndpointViewManager manager = EndpointViewManager.getInstance(e);
        if (manager != null) {
            manager.setFromTests(state);
        }
    }

    @Override
    public void update(AnActionEvent e) {
        super.update(e);
        e.getPresentation().setEnabled(EndpointViewManager.getInstance(e) != null);
    }
}
