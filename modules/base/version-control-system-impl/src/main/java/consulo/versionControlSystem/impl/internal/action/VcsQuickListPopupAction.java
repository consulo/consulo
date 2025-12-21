package consulo.versionControlSystem.impl.internal.action;

import consulo.annotation.component.ActionImpl;
import consulo.application.dumb.DumbAware;
import consulo.dataContext.DataContext;
import consulo.localize.LocalizeValue;
import consulo.project.Project;
import consulo.project.ui.action.QuickSwitchSchemeAction;
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
@ActionImpl(id = "Vcs.QuickListPopupAction")
public class VcsQuickListPopupAction extends QuickSwitchSchemeAction implements DumbAware {
    public VcsQuickListPopupAction() {
        super(VcsLocalize.actionQuickListPopupText(), VcsLocalize.actionQuickListPopupDescription());
        myActionPlace = ActionPlaces.ACTION_PLACE_VCS_QUICK_LIST_POPUP_ACTION;
    }

    @Override
    protected void fillActions(
        @Nullable Project project,
        @Nonnull ActionGroup.Builder group,
        @Nonnull DataContext dataContext
    ) {
        if (project == null) {
            return;
        }

        Pair<SupportedVCS, AbstractVcs> typeAndVcs = getActiveVCS(project, dataContext);
        AbstractVcs vcs = typeAndVcs.getSecond();
        SupportedVCS popupType = typeAndVcs.getFirst();

        switch (popupType) {
            case VCS -> fillVcsPopup(project, group, dataContext, vcs);
            case NOT_IN_VCS -> fillNonInVcsActions(project, group, dataContext);
        }
    }

    @Override
    protected boolean isEnabled() {
        return true;
    }

    private void fillVcsPopup(
        @Nonnull Project project,
        @Nonnull ActionGroup.Builder group,
        @Nullable DataContext dataContext,
        @Nullable AbstractVcs vcs
    ) {
        if (vcs != null) {
            // replace general vcs actions if necessary
            List<AnAction> actionsToReplace = project.getApplication().getExtensionPoint(VcsQuickListContentProvider.class)
                .computeSafeIfAny(
                    provider -> provider.replaceVcsActionsFor(vcs, dataContext)
                        ? provider.getVcsActions(project, vcs, dataContext)
                        : null
                );
            if (actionsToReplace != null) {
                // replace general actions with custom ones:
                addActions(actionsToReplace, group);
                // local history
                addLocalHistoryActions(group);
                return;
            }
        }

        // general list
        fillGeneralVcsPopup(project, group, dataContext, vcs);
    }

    private void fillGeneralVcsPopup(
        @Nonnull Project project,
        @Nonnull ActionGroup.Builder group,
        @Nullable DataContext dataContext,
        @Nullable AbstractVcs vcs
    ) {
        // include all custom actions in general popup
        List<AnAction> actions = new ArrayList<>();
        project.getApplication().getExtensionPoint(VcsQuickListContentProvider.class).forEach(provider -> {
            List<AnAction> providerActions = provider.getVcsActions(project, vcs, dataContext);
            if (providerActions != null) {
                actions.addAll(providerActions);
            }
        });

        // basic operations
        addSeparator(group, vcs != null ? vcs.getDisplayName(): LocalizeValue.empty());
        addAction("ChangesView.AddUnversioned", group);
        addAction("CheckinProject", group);
        addAction("CheckinFiles", group);
        addAction(IdeActions.CHANGES_VIEW_REVERT, group);

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
        @Nonnull ActionGroup.Builder group,
        @Nullable DataContext dataContext
    ) {
        // add custom vcs actions
        project.getApplication().getExtensionPoint(VcsQuickListContentProvider.class).forEach(provider -> {
            List<AnAction> actions = provider.getNotInVcsActions(project, dataContext);
            if (actions != null) {
                addActions(actions, group);
            }
        });
        addSeparator(group);
        addAction("Start.Use.Vcs", group);
        addAction("Vcs.Checkout", group);

        // local history
        addLocalHistoryActions(group);
    }

    private void addLocalHistoryActions(ActionGroup.Builder group) {
        addSeparator(group, VcsLocalize.vcsQuicklistPupupSectionLocalHistory());

        addAction("LocalHistory.ShowHistory", group);
        addAction("LocalHistory.PutLabel", group);
    }

    private void addActions(@Nonnull List<AnAction> actions, @Nonnull ActionGroup.Builder toGroup) {
        for (AnAction action : actions) {
            toGroup.add(action);
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

    private void addAction(String actionId, ActionGroup.Builder toGroup) {
        AnAction action = ActionManager.getInstance().getAction(actionId);

        // add action to group if it is available
        if (action != null) {
            toGroup.add(action);
        }
    }

    private void addSeparator(ActionGroup.Builder toGroup) {
        toGroup.add(AnSeparator.create());
    }

    private void addSeparator(ActionGroup.Builder toGroup, @Nonnull LocalizeValue text) {
        toGroup.add(AnSeparator.create(text));
    }

    @Override
    protected LocalizeValue getPopupTitle(AnActionEvent e) {
        return VcsLocalize.vcsQuicklistPopupTitle();
    }

    public enum SupportedVCS {
        VCS,
        NOT_IN_VCS
    }
}
