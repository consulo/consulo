package consulo.endpoint.impl.internal.view.action;

import consulo.annotation.component.ActionImpl;
import consulo.annotation.component.ActionRef;
import consulo.application.dumb.DumbAware;
import consulo.endpoint.localize.EndpointLocalize;
import consulo.platform.base.icon.PlatformIconGroup;
import consulo.ui.ex.action.DefaultActionGroup;

@ActionImpl(
    id = EndpointOptionsGroup.ID,
    children = {
        @ActionRef(type = EndpointCompactToggleAction.class),
        @ActionRef(type = EndpointGroupByModuleToggleAction.class),
        @ActionRef(type = EndpointShowFromLibrariesToggleAction.class),
        @ActionRef(type = EndpointShowFromTestsToggleAction.class)
    }
)
public final class EndpointOptionsGroup extends DefaultActionGroup implements DumbAware {
    public static final String ID = "Endpoints.Options";

    public EndpointOptionsGroup() {
        super(EndpointLocalize.endpointsViewOptions(), true);
        getTemplatePresentation().setDescription(EndpointLocalize.endpointsViewOptions());
        getTemplatePresentation().setIcon(PlatformIconGroup.actionsShow());
    }
}
