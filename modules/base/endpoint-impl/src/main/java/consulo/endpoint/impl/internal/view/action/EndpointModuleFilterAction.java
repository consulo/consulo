package consulo.endpoint.impl.internal.view.action;

import consulo.annotation.component.ActionImpl;
import consulo.endpoint.impl.internal.view.EndpointFilterChoice;
import consulo.endpoint.impl.internal.view.EndpointModuleSnapshot;
import consulo.endpoint.impl.internal.view.EndpointView;
import consulo.endpoint.impl.internal.view.EndpointViewManager;
import consulo.endpoint.localize.EndpointLocalize;
import consulo.localize.LocalizeValue;
import consulo.platform.base.icon.PlatformIconGroup;
import consulo.ui.annotation.RequiredUIAccess;

import java.util.ArrayList;
import java.util.List;

@ActionImpl(id = EndpointModuleFilterAction.ID)
public final class EndpointModuleFilterAction extends EndpointFilterComboBoxAction {
    public static final String ID = "Endpoints.ModuleFilter";

    public EndpointModuleFilterAction() {
        super(EndpointLocalize.frameworksFiltersModule(), EndpointLocalize.frameworksFiltersModuleTitle());
    }

    @Override
    protected LocalizeValue getFilterName(EndpointViewManager manager) {
        return manager.getProjectModel().getModuleDisplayName();
    }

    @Override
    protected LocalizeValue getFilterPopupTitle(EndpointViewManager manager) {
        return manager.getProjectModel().getSelectModulesTitle();
    }

    @Override
    protected List<EndpointFilterChoice> getChoices(EndpointView view) {
        List<EndpointFilterChoice> choices = new ArrayList<>(view.getSnapshot().getModules());
        choices.add(new EndpointFilterChoice(
            EndpointModuleSnapshot.EXTERNAL_KEY,
            EndpointLocalize.frameworksFiltersModuleExternal(),
            PlatformIconGroup.generalWeb()
        ));
        return choices;
    }

    @Override
    protected boolean isChoiceSelected(EndpointViewManager manager, String id) {
        return EndpointModuleSnapshot.EXTERNAL_KEY.equals(id) ? manager.isExternalVisible() : manager.isModuleVisible(id);
    }

    @Override
    @RequiredUIAccess
    protected void setChoiceSelected(EndpointViewManager manager, String id, boolean selected) {
        if (EndpointModuleSnapshot.EXTERNAL_KEY.equals(id)) {
            manager.setExternalVisible(selected);
        }
        else {
            manager.setModuleVisible(id, selected);
        }
    }
}
