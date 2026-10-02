package consulo.endpoint.impl.internal.view.action;

import consulo.annotation.component.ActionImpl;
import consulo.annotation.component.ActionRef;
import consulo.application.dumb.DumbAware;
import consulo.endpoint.impl.internal.diagram.ShowServicesDiagramAction;
import consulo.endpoint.impl.internal.view.detail.ToggleEndpointDetailsAction;
import consulo.ui.ex.action.DefaultActionGroup;

@ActionImpl(
    id = EndpointToolbarActionGroup.ID,
    children = {
        @ActionRef(type = ShowServicesDiagramAction.class),
        @ActionRef(type = ToggleEndpointDetailsAction.class)
    }
)
public final class EndpointToolbarActionGroup extends DefaultActionGroup implements DumbAware {
    public static final String ID = "Endpoints.ToolbarActions";
}
