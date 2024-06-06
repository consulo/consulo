package consulo.ide.impl.idea.openapi.vcs.actions;

import consulo.application.dumb.DumbAware;
import consulo.dataContext.DataContext;
import consulo.ide.impl.idea.ide.actions.QuickSwitchSchemeAction;
import consulo.language.editor.PlatformDataKeys;
import consulo.project.Project;
import consulo.ui.ex.action.*;
import consulo.util.lang.Pair;
import consulo.versionControlSystem.AbstractVcs;
import consulo.versionControlSystem.ProjectLevelVcsManager;
import consulo.versionControlSystem.action.VcsQuickListContentProvider;
import consulo.versionControlSystem.localize.VcsLocalize;
import consulo.virtualFileSystem.VirtualFile;
import jakarta.annotation.Nonnull;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Roman.Chernyatchik
 *
 * Context aware VCS actions quick list.
 * May be customized using consulo.versionControlSystem.action.VcsQuickListContentProvider extension point.
 */
public class VcsQuickListPopupAction extends QuickSwitchSchemeAction implements DumbAware {

  public VcsQuickListPopupAction() {
    myActionPlace = ActionPlaces.ACTION_PLACE_VCS_QUICK_LIST_POPUP_ACTION;
  }

  protected void fillActions(@jakarta.annotation.Nullable final Project project,
                             @Nonnull final DefaultActionGroup group,
                             @Nonnull final DataContext dataContext) {

    if (project == null) {
      return;
    }

    final Pair<SupportedVCS, AbstractVcs> typeAndVcs = getActiveVCS(project, dataContext);
    final AbstractVcs vcs = typeAndVcs.getSecond();
    final SupportedVCS popupType = typeAndVcs.getFirst();

    switch (popupType) {
      case VCS:
        fillVcsPopup(project, group, dataContext, vcs);
        break;

      case NOT_IN_VCS:
        fillNonInVcsActions(project, group, dataContext);
        break;
    }
  }

  protected boolean isEnabled() {
    return true;
  }

  private void fillVcsPopup(@Nonnull final Project project,
                                    @Nonnull final DefaultActionGroup group,
                                    @jakarta.annotation.Nullable final DataContext dataContext,
                                    @jakarta.annotation.Nullable final AbstractVcs vcs) {

    if (vcs != null) {
      // replace general vcs actions if necessary

      for (VcsQuickListContentProvider provider : VcsQuickListContentProvider.EP_NAME.getExtensionList()) {
        if (provider.replaceVcsActionsFor(vcs, dataContext)) {
          final List<AnAction> actionsToReplace = provider.getVcsActions(project, vcs, dataContext);
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

  private void fillGeneralVcsPopup(@Nonnull final Project project,
                                   @Nonnull final DefaultActionGroup group,
                                   @jakarta.annotation.Nullable final DataContext dataContext,
                                   @jakarta.annotation.Nullable final AbstractVcs vcs) {

    // include all custom actions in general popup
    final List<AnAction> actions = new ArrayList<AnAction>();
    for (VcsQuickListContentProvider provider : VcsQuickListContentProvider.EP_NAME.getExtensionList()) {
      final List<AnAction> providerActions = provider.getVcsActions(project, vcs, dataContext);
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

  private void fillNonInVcsActions(@Nonnull final Project project,
                                   @Nonnull final DefaultActionGroup group,
                                   @jakarta.annotation.Nullable final DataContext dataContext) {
    // add custom vcs actions
    for (VcsQuickListContentProvider provider : VcsQuickListContentProvider.EP_NAME.getExtensionList()) {
      final List<AnAction> actions = provider.getNotInVcsActions(project, dataContext);
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

  private void addActions(@Nonnull final List<AnAction> actions,
                             @Nonnull final DefaultActionGroup toGroup) {
    for (AnAction action : actions) {
      toGroup.addAction(action);
    }
  }

  private Pair<SupportedVCS, AbstractVcs> getActiveVCS(@Nonnull final Project project, @jakarta.annotation.Nullable final DataContext dataContext) {
    final AbstractVcs[] activeVcss = getActiveVCSs(project);
    if (activeVcss.length == 0) {
      // no vcs
      return new Pair<SupportedVCS, AbstractVcs>(SupportedVCS.NOT_IN_VCS, null);
    } else if (activeVcss.length == 1) {
      // get by name
      return new Pair<SupportedVCS, AbstractVcs>(SupportedVCS.VCS, activeVcss[0]);
    }

    // by current file
    final VirtualFile file =  dataContext != null ? dataContext.getData(PlatformDataKeys.VIRTUAL_FILE) : null;
    if (file != null) {
      final AbstractVcs vscForFile = ProjectLevelVcsManager.getInstance(project).getVcsFor(file);
      if (vscForFile != null) {
        return new Pair<SupportedVCS, AbstractVcs>(SupportedVCS.VCS, vscForFile);
      }
    }

    return new Pair<SupportedVCS, AbstractVcs>(SupportedVCS.VCS, null);
  }

  private AbstractVcs[] getActiveVCSs(final Project project) {
    final ProjectLevelVcsManager vcsManager = ProjectLevelVcsManager.getInstance(project);
    return vcsManager.getAllActiveVcss();
  }

  private void addAction(final String actionId, final DefaultActionGroup toGroup) {
    final AnAction action = ActionManager.getInstance().getAction(actionId);

    // add action to group if it is available
    if (action != null) {
      toGroup.add(action);
    }
  }

  private void addSeparator(final DefaultActionGroup toGroup) {
    addSeparator(toGroup, null);
  }

  private void addSeparator(final DefaultActionGroup toGroup, @jakarta.annotation.Nullable final String title) {
    final AnSeparator separator = title == null ? new AnSeparator() : new AnSeparator(title);
    toGroup.add(separator);
  }

  protected String getPopupTitle(AnActionEvent e) {
    return VcsLocalize.vcsQuicklistPopupTitle().get();
  }

  public enum SupportedVCS {
    VCS,
    NOT_IN_VCS
  }
}
