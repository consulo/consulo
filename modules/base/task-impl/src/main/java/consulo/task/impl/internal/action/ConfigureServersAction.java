package consulo.task.impl.internal.action;

import consulo.annotation.component.ActionImpl;
import consulo.application.AllIcons;
import consulo.application.Application;
import consulo.configurable.internal.ShowConfigurableService;
import consulo.project.Project;
import consulo.task.impl.internal.setting.TaskRepositoriesConfigurable;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.action.AnActionEvent;

import javax.annotation.Nonnull;

/**
 * User: Evgeny Zakrevsky
 */
@ActionImpl(id = "tasks.configure.servers")
public class ConfigureServersAction extends BaseTaskAction {
  public ConfigureServersAction() {
    super("Configure Servers...", null, AllIcons.General.Settings);
  }

  @RequiredUIAccess
  @Override
  public void actionPerformed(@Nonnull AnActionEvent e) {
    Project project = e.getData(Project.KEY);

    Application.get().getInstance(ShowConfigurableService.class).showAndSelect(project, TaskRepositoriesConfigurable.class);

    // we need call serversChanged - but its impossible
  }

  protected void serversChanged() {

  }
}
