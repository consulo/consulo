package consulo.task.impl.internal.action;

import consulo.annotation.component.ActionImpl;
import consulo.application.dumb.DumbAware;
import consulo.externalService.statistic.FeatureUsageTracker;
import consulo.localize.LocalizeValue;
import consulo.platform.base.icon.PlatformIconGroup;
import consulo.project.Project;
import consulo.searchEverywhere.SearchEverywhereManager;
import consulo.task.internal.CreateNewTaskAction;
import consulo.task.localize.TaskLocalize;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.action.AnAction;
import consulo.ui.ex.action.AnActionEvent;
import consulo.ui.ex.action.IdeActions;
import consulo.ui.ex.awt.internal.IdeEventQueueProxy;
import jakarta.annotation.Nonnull;
import jakarta.inject.Inject;

/**
 * @author Evgeny Zakrevsky
 */
@ActionImpl(id = GotoTaskAction.ID)
public class GotoTaskAction extends AnAction implements DumbAware {
    public static final String ID = "tasks.goto";

    @Inject
    public GotoTaskAction() {
        super(TaskLocalize.openTaskActionMenuText(), LocalizeValue.absent(), PlatformIconGroup.generalAdd());
    }

    @Override
    public void update(@Nonnull AnActionEvent e) {
        super.update(e);
        e.getPresentation().setEnabled(e.hasData(Project.KEY));
    }

    @RequiredUIAccess
    @Override
    public void actionPerformed(@Nonnull AnActionEvent e) {
        Project project = e.getRequiredData(Project.KEY);
        
        SearchEverywhereManager seManager = SearchEverywhereManager.getInstance(project);
        FeatureUsageTracker.getInstance().triggerFeatureUsed(IdeActions.ACTION_SEARCH_EVERYWHERE);

        if (seManager.isShown()) {
            if (TaskSearchEverywhereContributor.ID.equals(seManager.getSelectedContributorID())) {
                seManager.toggleEverywhereFilter();
            }
            else {
                seManager.setSelectedContributor(TaskSearchEverywhereContributor.ID);
            }
            return;
        }

        IdeEventQueueProxy.getInstance().closeAllPopups(false);

        seManager.show(TaskSearchEverywhereContributor.ID, "", e);
    }
}
