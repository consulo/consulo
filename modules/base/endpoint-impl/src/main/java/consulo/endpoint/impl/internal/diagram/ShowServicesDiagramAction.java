package consulo.endpoint.impl.internal.diagram;

import consulo.annotation.component.ActionImpl;
import consulo.dataContext.DataContext;
import consulo.endpoint.EndpointProvider;
import consulo.endpoint.localize.EndpointLocalize;
import consulo.platform.base.icon.PlatformIconGroup;
import consulo.project.Project;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.action.ActionManager;
import consulo.ui.ex.action.AnAction;
import consulo.ui.ex.action.AnActionEvent;
import consulo.ui.ex.action.AnActionWithSyncUpdate;
import jakarta.inject.Inject;

@ActionImpl(id = ShowServicesDiagramAction.ID)
public final class ShowServicesDiagramAction extends AnAction implements AnActionWithSyncUpdate {
    public static final String ID = "Endpoints.ServicesDiagram";

    private static final String SHOW_DIAGRAM_ACTION_ID = "ShowDiagram";

    private final ActionManager myActionManager;

    @Inject
    public ShowServicesDiagramAction(ActionManager actionManager) {
        super(
            EndpointLocalize.servicesDiagramActionText(),
            EndpointLocalize.servicesDiagramActionDescription(),
            PlatformIconGroup.filetypesDiagram()
        );
        myActionManager = actionManager;
    }

    @RequiredUIAccess
    @Override
    public void actionPerformed(AnActionEvent e) {
        Project project = e.getData(Project.KEY);
        if (project == null) {
            return;
        }

        AnAction showDiagramAction = myActionManager.getAction(SHOW_DIAGRAM_ACTION_ID);
        if (showDiagramAction == null) {
            return;
        }

        DataContext dataContext = DataContext.builder()
            .add(Project.KEY, project)
            .add(ServicesDiagramRoot.KEY, new ServicesDiagramRoot(project.getName()))
            .build();
        showDiagramAction.actionPerformed(AnActionEvent.createFromDataContext(e.getPlace(), null, dataContext));
    }

    @Override
    public void update(AnActionEvent e) {
        Project project = e.getData(Project.KEY);
        e.getPresentation().setEnabledAndVisible(project != null && !EndpointProvider.getAvailableProviders(project).isEmpty());
    }
}
