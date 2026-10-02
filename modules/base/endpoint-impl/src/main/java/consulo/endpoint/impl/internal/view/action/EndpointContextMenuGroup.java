package consulo.endpoint.impl.internal.view.action;

import consulo.annotation.component.ActionImpl;
import consulo.annotation.component.ActionRef;
import consulo.application.dumb.DumbAware;
import consulo.ui.ex.action.DefaultActionGroup;
import consulo.ui.ex.action.IdeActions;

@ActionImpl(id = EndpointContextMenuGroup.ID, children = @ActionRef(id = IdeActions.ACTION_EDIT_SOURCE))
public final class EndpointContextMenuGroup extends DefaultActionGroup implements DumbAware {
    public static final String ID = "Endpoints.ContextMenu";
}
