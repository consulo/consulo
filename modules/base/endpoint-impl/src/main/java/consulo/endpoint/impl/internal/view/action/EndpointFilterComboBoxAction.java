package consulo.endpoint.impl.internal.view.action;

import consulo.application.dumb.DumbAware;
import consulo.dataContext.DataContext;
import consulo.endpoint.EndpointViewOpener;
import consulo.endpoint.impl.internal.view.EndpointFilterChoice;
import consulo.endpoint.impl.internal.view.EndpointView;
import consulo.endpoint.impl.internal.view.EndpointViewManager;
import consulo.endpoint.localize.EndpointLocalize;
import consulo.localize.LocalizeValue;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.action.ActionGroup;
import consulo.ui.ex.action.AnActionEvent;
import consulo.ui.ex.action.ComboBoxAction;
import consulo.ui.ex.action.DefaultActionGroup;
import consulo.ui.ex.action.Presentation;

import java.util.ArrayList;
import java.util.List;

public abstract class EndpointFilterComboBoxAction extends ComboBoxAction implements DumbAware {
    protected EndpointFilterComboBoxAction(LocalizeValue text, LocalizeValue description) {
        getTemplatePresentation().setText(text);
        getTemplatePresentation().setDescription(description);
    }

    protected abstract LocalizeValue getFilterName(EndpointViewManager manager);

    protected abstract LocalizeValue getFilterPopupTitle(EndpointViewManager manager);

    protected abstract List<EndpointFilterChoice> getChoices(EndpointView view);

    protected abstract boolean isChoiceSelected(EndpointViewManager manager, String id);

    @RequiredUIAccess
    protected abstract void setChoiceSelected(EndpointViewManager manager, String id, boolean selected);

    @Override
    public void update(AnActionEvent e) {
        Presentation presentation = e.getPresentation();
        EndpointView view = e.getData(EndpointView.KEY);
        EndpointViewManager manager = EndpointViewManager.getInstance(e);
        if (view == null || manager == null) {
            presentation.setEnabledAndVisible(false);
            return;
        }

        presentation.setEnabledAndVisible(true);
        setPopupTitle(getFilterPopupTitle(manager));

        LocalizeValue name = getFilterName(manager);
        List<EndpointFilterChoice> choices = getChoices(view);
        List<EndpointFilterChoice> selected = new ArrayList<>();
        for (EndpointFilterChoice choice : choices) {
            if (isChoiceSelected(manager, choice.id())) {
                selected.add(choice);
            }
        }

        if (selected.size() == choices.size()) {
            presentation.setText(name);
        }
        else if (selected.size() == 1) {
            presentation.setText(EndpointLocalize.endpointsFilterSelectionSingle(name, selected.get(0).text()));
        }
        else {
            presentation.setText(EndpointLocalize.endpointsFilterSelectionCount(name, selected.size()));
        }
    }

    @Override
    protected ActionGroup createPopupActionGroup(DataContext context) {
        DefaultActionGroup group = new DefaultActionGroup();
        EndpointView view = context.getData(EndpointView.KEY);
        if (view == null) {
            return group;
        }

        EndpointViewManager manager = EndpointViewManager.getInstance(view.getProject());
        for (EndpointFilterChoice choice : getChoices(view)) {
            String id = choice.id();
            group.add(new EndpointFilterItemToggleAction(
                choice.text(),
                choice.icon(),
                () -> isChoiceSelected(manager, id),
                state -> setChoiceSelected(manager, id, state)
            ));
        }
        return group;
    }

    @Override
    public String getPopupActionPlace() {
        return EndpointViewOpener.ENDPOINTS_FILTER_TOOLBAR_PLACE;
    }
}
