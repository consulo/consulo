package consulo.endpoint.impl.internal.view.action;

import consulo.annotation.component.ActionImpl;
import consulo.endpoint.impl.internal.view.EndpointFilterChoice;
import consulo.endpoint.impl.internal.view.EndpointView;
import consulo.endpoint.impl.internal.view.EndpointViewManager;
import consulo.endpoint.localize.EndpointLocalize;
import consulo.localize.LocalizeValue;
import consulo.ui.annotation.RequiredUIAccess;

import java.util.List;

@ActionImpl(id = EndpointTypeFilterAction.ID)
public final class EndpointTypeFilterAction extends EndpointFilterComboBoxAction {
    public static final String ID = "Endpoints.TypeFilter";

    public EndpointTypeFilterAction() {
        super(EndpointLocalize.frameworksFiltersType(), EndpointLocalize.frameworksFiltersTypeTitle());
    }

    @Override
    protected LocalizeValue getFilterName(EndpointViewManager manager) {
        return EndpointLocalize.frameworksFiltersType();
    }

    @Override
    protected LocalizeValue getFilterPopupTitle(EndpointViewManager manager) {
        return EndpointLocalize.frameworksFiltersTypeTitle();
    }

    @Override
    protected List<EndpointFilterChoice> getChoices(EndpointView view) {
        return view.getSnapshot().getTypes();
    }

    @Override
    protected boolean isChoiceSelected(EndpointViewManager manager, String id) {
        return manager.isTypeVisible(id);
    }

    @Override
    @RequiredUIAccess
    protected void setChoiceSelected(EndpointViewManager manager, String id, boolean selected) {
        manager.setTypeVisible(id, selected);
    }
}
