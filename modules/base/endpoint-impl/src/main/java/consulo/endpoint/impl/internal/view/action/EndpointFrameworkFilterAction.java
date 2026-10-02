package consulo.endpoint.impl.internal.view.action;

import consulo.annotation.component.ActionImpl;
import consulo.endpoint.impl.internal.view.EndpointFilterChoice;
import consulo.endpoint.impl.internal.view.EndpointView;
import consulo.endpoint.impl.internal.view.EndpointViewManager;
import consulo.endpoint.localize.EndpointLocalize;
import consulo.localize.LocalizeValue;
import consulo.ui.annotation.RequiredUIAccess;

import java.util.List;

@ActionImpl(id = EndpointFrameworkFilterAction.ID)
public final class EndpointFrameworkFilterAction extends EndpointFilterComboBoxAction {
    public static final String ID = "Endpoints.FrameworkFilter";

    public EndpointFrameworkFilterAction() {
        super(EndpointLocalize.frameworksFiltersFramework(), EndpointLocalize.frameworksFiltersFrameworkTitle());
    }

    @Override
    protected LocalizeValue getFilterName(EndpointViewManager manager) {
        return EndpointLocalize.frameworksFiltersFramework();
    }

    @Override
    protected LocalizeValue getFilterPopupTitle(EndpointViewManager manager) {
        return EndpointLocalize.frameworksFiltersFrameworkTitle();
    }

    @Override
    protected List<EndpointFilterChoice> getChoices(EndpointView view) {
        return view.getSnapshot().getFrameworks();
    }

    @Override
    protected boolean isChoiceSelected(EndpointViewManager manager, String id) {
        return manager.isFrameworkVisible(id);
    }

    @Override
    @RequiredUIAccess
    protected void setChoiceSelected(EndpointViewManager manager, String id, boolean selected) {
        manager.setFrameworkVisible(id, selected);
    }
}
