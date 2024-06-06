package consulo.ide.impl.idea.ide.projectView.impl;

import consulo.ide.IdeBundle;
import consulo.util.lang.Comparing;
import consulo.platform.base.icon.PlatformIconGroup;
import consulo.project.Project;
import consulo.project.ui.view.ProjectView;
import consulo.ui.ex.action.AnActionEvent;
import consulo.ui.ex.action.Presentation;
import consulo.ui.ex.action.ToggleAction;

/**
* @author anna
* @since 9:33 PM 8/5/11
*/
public abstract class ShowModulesAction extends ToggleAction {
  private Project myProject;

  public ShowModulesAction(Project project) {
    super(IdeBundle.message("action.show.modules"), IdeBundle.message("action.description.show.modules"), PlatformIconGroup.actionsGroupbymodule());
    myProject = project;
  }

  @Override
  public boolean isSelected(AnActionEvent event) {
    return ProjectView.getInstance(myProject).isShowModules(getId());
  }

  protected abstract String getId();

  @Override
  public void setSelected(AnActionEvent event, boolean flag) {
    final ProjectViewImpl projectView = (ProjectViewImpl)ProjectView.getInstance(myProject);
    projectView.setShowModules(flag, getId());
  }

  @Override
  public void update(AnActionEvent e) {
    super.update(e);
    final Presentation presentation = e.getPresentation();
    final ProjectViewImpl projectView = (ProjectViewImpl)ProjectView.getInstance(myProject);
    presentation.setVisible(Comparing.strEqual(projectView.getCurrentViewId(), getId()));
  }
}
