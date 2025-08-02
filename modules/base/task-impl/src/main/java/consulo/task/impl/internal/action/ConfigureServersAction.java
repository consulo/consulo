package consulo.task.impl.internal.action;

import consulo.annotation.component.ActionImpl;
import consulo.application.Application;
import consulo.configurable.internal.ShowConfigurableService;
import consulo.localize.LocalizeValue;
import consulo.platform.base.icon.PlatformIconGroup;
import consulo.project.Project;
import consulo.task.impl.internal.setting.TaskRepositoriesConfigurable;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.action.AnActionEvent;
import jakarta.annotation.Nonnull;
import jakarta.inject.Inject;

/**
 * @author Evgeny Zakrevsky
 */
@ActionImpl(id = "tasks.configure.servers")
public class ConfigureServersAction extends BaseTaskAction {
    @Nonnull
    private final Application myApplication;

    @Inject
    public ConfigureServersAction(@Nonnull Application application) {
        super(LocalizeValue.localizeTODO("Configure Servers..."), LocalizeValue.empty(), PlatformIconGroup.generalSettings());
        myApplication = application;
    }

    @Override
    @RequiredUIAccess
    public void actionPerformed(@Nonnull AnActionEvent e) {
        Project project = e.getData(Project.KEY);

        myApplication.getInstance(ShowConfigurableService.class)
            .showAndSelect(project, TaskRepositoriesConfigurable.class);

        // we need call serversChanged - but its impossible
    }

    protected void serversChanged() {
    }
}
