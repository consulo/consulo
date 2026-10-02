package consulo.endpoint.impl.internal.view.action;

import consulo.annotation.component.ActionImpl;
import consulo.annotation.component.ActionRef;
import consulo.application.dumb.DumbAware;
import consulo.ui.ex.action.AnSeparator;
import consulo.ui.ex.action.DefaultActionGroup;

@ActionImpl(
    id = EndpointToolbarGroup.ID,
    children = {
        @ActionRef(type = EndpointModuleFilterAction.class),
        @ActionRef(type = EndpointTypeFilterAction.class),
        @ActionRef(type = EndpointFrameworkFilterAction.class),
        @ActionRef(type = AnSeparator.class),
        @ActionRef(type = EndpointToolbarActionGroup.class),
        @ActionRef(type = EndpointOptionsGroup.class)
    }
)
public final class EndpointToolbarGroup extends DefaultActionGroup implements DumbAware {
    public static final String ID = "Endpoints.Toolbar";
}
