package consulo.ide.impl.idea.ide.projectView.impl;

import consulo.application.AllIcons;
import consulo.ide.IdeBundle;
import consulo.ide.impl.idea.ide.projectView.ProjectView;
import consulo.ui.ex.action.AnActionEvent;
import consulo.ui.ex.action.Presentation;
import consulo.ui.ex.action.ToggleAction;
import consulo.project.Project;
import consulo.ide.impl.idea.openapi.util.Comparing;

/**
* Created by IntelliJ IDEA.
* User: anna
* Date: 8/5/11
* Time: 9:33 PM
* To change this template use File | Settings | File Templates.
*/
public abstract class ShowModulesAction extends ToggleAction {
  private Project myProject;

  public ShowModulesAction(Project project) {
    super(IdeBundle.message("action.show.modules"), IdeBundle.message("action.description.show.modules"),
          AllIcons.ObjectBrowser.ShowModules);
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
