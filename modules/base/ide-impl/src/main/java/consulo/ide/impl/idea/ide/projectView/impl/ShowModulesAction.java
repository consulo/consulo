package consulo.ide.impl.idea.ide.projectView.impl;

import consulo.platform.base.icon.PlatformIconGroup;
import consulo.ide.localize.IdeLocalize;
import consulo.project.Project;
import consulo.project.ui.view.ProjectView;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.action.AnActionEvent;
import consulo.ui.ex.action.ToggleAction;
import consulo.util.lang.Comparing;

/**
* @author anna
* @since 2011-08-05
*/
public abstract class ShowModulesAction extends ToggleAction {
  private Project myProject;

  public ShowModulesAction(Project project) {
    super(
      IdeLocalize.actionShowModules(),
      IdeLocalize.actionDescriptionShowModules(),
      PlatformIconGroup.actionsGroupbymodule()
    );
    myProject = project;
  }

  @Override
  public boolean isSelected(AnActionEvent event) {
    return ProjectView.getInstance(myProject).isShowModules(getId());
  }

  protected abstract String getId();

  @Override
  @RequiredUIAccess
  public void setSelected(AnActionEvent event, boolean flag) {
    ProjectViewImpl projectView = (ProjectViewImpl)ProjectView.getInstance(myProject);
    projectView.setShowModules(flag, getId());
  }

  @Override
  public void update(AnActionEvent e) {
    super.update(e);
    ProjectViewImpl projectView = (ProjectViewImpl)ProjectView.getInstance(myProject);
    e.getPresentation().setVisible(Comparing.strEqual(projectView.getCurrentViewId(), getId()));
  }
}
