package consulo.ide.impl.idea.openapi.vcs.actions;

import consulo.application.dumb.DumbAware;
import consulo.dataContext.DataContext;
import consulo.ide.impl.idea.ide.actions.QuickSwitchSchemeAction;
import consulo.project.Project;
import consulo.ui.ex.action.*;
import consulo.util.lang.Pair;
import consulo.versionControlSystem.AbstractVcs;
import consulo.versionControlSystem.ProjectLevelVcsManager;
import consulo.versionControlSystem.action.VcsQuickListContentProvider;
import consulo.versionControlSystem.localize.VcsLocalize;
import consulo.virtualFileSystem.VirtualFile;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Roman.Chernyatchik
 * <p>
 * Context aware VCS actions quick list.
 * May be customized using consulo.versionControlSystem.action.VcsQuickListContentProvider extension point.
 */
public class VcsQuickListPopupAction extends QuickSwitchSchemeAction implements DumbAware {

    public VcsQuickListPopupAction() {
        myActionPlace = ActionPlaces.ACTION_PLACE_VCS_QUICK_LIST_POPUP_ACTION;
    }

    @Override
    protected void fillActions(
        @Nullable Project project,
        @Nonnull DefaultActionGroup group,
        @Nonnull DataContext dataContext
    ) {
        if (project == null) {
            return;
        }

        Pair<SupportedVCS, AbstractVcs> typeAndVcs = getActiveVCS(project, dataContext);
        AbstractVcs vcs = typeAndVcs.getSecond();
        SupportedVCS popupType = typeAndVcs.getFirst();

        switch (popupType) {
            case VCS:
                fillVcsPopup(project, group, dataContext, vcs);
                break;

            case NOT_IN_VCS:
                fillNonInVcsActions(project, group, dataContext);
                break;
        }
    }

    @Override
    protected boolean isEnabled() {
        return true;
    }

    private void fillVcsPopup(
        @Nonnull Project project,
        @Nonnull DefaultActionGroup group,
        @Nullable DataContext dataContext,
        @Nullable AbstractVcs vcs
    ) {
        if (vcs != null) {
            // replace general vcs actions if necessary

            for (VcsQuickListContentProvider provider : VcsQuickListContentProvider.EP_NAME.getExtensionList()) {
                if (provider.replaceVcsActionsFor(vcs, dataContext)) {
                    List<AnAction> actionsToReplace = provider.getVcsActions(project, vcs, dataContext);
                    if (actionsToReplace != null) {
                        // replace general actions with custom ones:
                        addActions(actionsToReplace, group);
                        // local history
                        addLocalHistoryActions(group);
                        return;
                    }
                }
            }
        }

        // general list
        fillGeneralVcsPopup(project, group, dataContext, vcs);
    }

    private void fillGeneralVcsPopup(
        @Nonnull Project project,
        @Nonnull DefaultActionGroup group,
        @Nullable DataContext dataContext,
        @Nullable AbstractVcs vcs
    ) {
        // include all custom actions in general popup
        List<AnAction> actions = new ArrayList<>();
        for (VcsQuickListContentProvider provider : VcsQuickListContentProvider.EP_NAME.getExtensionList()) {
            List<AnAction> providerActions = provider.getVcsActions(project, vcs, dataContext);
            if (providerActions != null) {
                actions.addAll(providerActions);
            }
        }

        // basic operations
        addSeparator(group, vcs != null ? vcs.getDisplayName() : null);
        addAction("ChangesView.AddUnversioned", group);
        addAction("CheckinProject", group);
        addAction("CheckinFiles", group);
        addAction(IdeActions.CHANGES_VIEW_ROLLBACK, group);

        // history and compare
        addSeparator(group);
        addAction("Vcs.ShowTabbedFileHistory", group);
        addAction("Annotate", group);
        addAction("Compare.SameVersion", group);

        // custom actions
        addSeparator(group);
        addActions(actions, group);

        // additional stuff
        addSeparator(group);
        addAction(IdeActions.MOVE_TO_ANOTHER_CHANGE_LIST, group);

        // local history
        addLocalHistoryActions(group);
    }

    private void fillNonInVcsActions(
        @Nonnull Project project,
        @Nonnull DefaultActionGroup group,
        @Nullable DataContext dataContext
    ) {
        // add custom vcs actions
        for (VcsQuickListContentProvider provider : VcsQuickListContentProvider.EP_NAME.getExtensionList()) {
            List<AnAction> actions = provider.getNotInVcsActions(project, dataContext);
            if (actions != null) {
                addActions(actions, group);
            }
        }
        addSeparator(group);
        addAction("Start.Use.Vcs", group);
        addAction("Vcs.Checkout", group);

        // local history
        addLocalHistoryActions(group);
    }

    private void addLocalHistoryActions(DefaultActionGroup group) {
        addSeparator(group, VcsLocalize.vcsQuicklistPupupSectionLocalHistory().get());

        addAction("LocalHistory.ShowHistory", group);
        addAction("LocalHistory.PutLabel", group);
    }

    private void addActions(
        @Nonnull List<AnAction> actions,
        @Nonnull DefaultActionGroup toGroup
    ) {
        for (AnAction action : actions) {
            toGroup.addAction(action);
        }
    }

    private Pair<SupportedVCS, AbstractVcs> getActiveVCS(@Nonnull Project project, @Nullable DataContext dataContext) {
        AbstractVcs[] activeVcss = getActiveVCSs(project);
        if (activeVcss.length == 0) {
            // no vcs
            return new Pair<>(SupportedVCS.NOT_IN_VCS, null);
        }
        else if (activeVcss.length == 1) {
            // get by name
            return Pair.create(SupportedVCS.VCS, activeVcss[0]);
        }

        // by current file
        VirtualFile file = dataContext != null ? dataContext.getData(VirtualFile.KEY) : null;
        if (file != null) {
            AbstractVcs vscForFile = ProjectLevelVcsManager.getInstance(project).getVcsFor(file);
            if (vscForFile != null) {
                return Pair.create(SupportedVCS.VCS, vscForFile);
            }
        }

        return new Pair<>(SupportedVCS.VCS, null);
    }

    private AbstractVcs[] getActiveVCSs(Project project) {
        ProjectLevelVcsManager vcsManager = ProjectLevelVcsManager.getInstance(project);
        return vcsManager.getAllActiveVcss();
    }

    private void addAction(String actionId, DefaultActionGroup toGroup) {
        AnAction action = ActionManager.getInstance().getAction(actionId);

        // add action to group if it is available
        if (action != null) {
            toGroup.add(action);
        }
    }

    private void addSeparator(DefaultActionGroup toGroup) {
        addSeparator(toGroup, null);
    }

    private void addSeparator(DefaultActionGroup toGroup, @Nullable String title) {
        AnSeparator separator = title == null ? new AnSeparator() : new AnSeparator(title);
        toGroup.add(separator);
    }

    @Override
    protected String getPopupTitle(AnActionEvent e) {
        return VcsLocalize.vcsQuicklistPopupTitle().get();
    }

    public enum SupportedVCS {
        VCS,
        NOT_IN_VCS
    }
}
