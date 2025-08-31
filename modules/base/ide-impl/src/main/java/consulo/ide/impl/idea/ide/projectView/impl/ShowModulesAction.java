package consulo.ide.impl.idea.ide.projectView.impl;

import consulo.platform.base.icon.PlatformIconGroup;
import consulo.ide.localize.IdeLocalize;
import consulo.project.Project;
import consulo.project.ui.view.ProjectView;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.action.AnActionEvent;
import consulo.ui.ex.action.ToggleAction;
import consulo.util.lang.Comparing;
import jakarta.annotation.Nonnull;

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
  public boolean isSelected(@Nonnull AnActionEvent event) {
    return ProjectView.getInstance(myProject).isShowModules(getId());
  }

  protected abstract String getId();

  @Override
  @RequiredUIAccess
  public void setSelected(@Nonnull AnActionEvent event, boolean flag) {
    ProjectViewImpl projectView = (ProjectViewImpl)ProjectView.getInstance(myProject);
    projectView.setShowModules(flag, getId());
  }

  @Override
  public void update(@Nonnull AnActionEvent e) {
    super.update(e);
    ProjectViewImpl projectView = (ProjectViewImpl)ProjectView.getInstance(myProject);
    e.getPresentation().setVisible(Comparing.strEqual(projectView.getCurrentViewId(), getId()));
  }
}
