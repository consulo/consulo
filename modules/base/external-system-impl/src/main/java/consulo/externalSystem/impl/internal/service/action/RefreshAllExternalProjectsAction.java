package consulo.externalSystem.impl.internal.service.action;

import consulo.annotation.component.ActionImpl;
import consulo.application.Application;
import consulo.application.dumb.DumbAware;
import consulo.document.FileDocumentManager;
import consulo.externalSystem.ExternalSystemManager;
import consulo.externalSystem.impl.internal.service.ExternalSystemProcessingManager;
import consulo.externalSystem.impl.internal.util.ExternalSystemUtil;
import consulo.externalSystem.importing.ImportSpecBuilder;
import consulo.externalSystem.localize.ExternalSystemLocalize;
import consulo.externalSystem.model.ExternalSystemDataKeys;
import consulo.externalSystem.model.ProjectSystemId;
import consulo.externalSystem.model.task.ExternalSystemTaskType;
import consulo.externalSystem.model.task.ProgressExecutionMode;
import consulo.platform.base.icon.PlatformIconGroup;
import consulo.project.Project;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.action.AnAction;
import consulo.ui.ex.action.AnActionEvent;
import consulo.util.lang.StringUtil;
import jakarta.annotation.Nonnull;

import java.util.ArrayList;
import java.util.List;

/**
 * Forces the ide to retrieve the most up-to-date info about the linked external projects and updates project state if necessary
 * (e.g. imports missing libraries).
 *
 * @author Denis Zhdanov
 * @since 2012-01-23
 */
@ActionImpl(id = "ExternalSystem.RefreshAllProjects")
public class RefreshAllExternalProjectsAction extends AnAction implements DumbAware {
    public RefreshAllExternalProjectsAction() {
        super(
            ExternalSystemLocalize.actionRefreshAllExternalProjectsText(),
            ExternalSystemLocalize.actionRefreshAllExternalProjectsDescription(),
            PlatformIconGroup.actionsRefresh()
        );
    }

    @Override
    public void update(@Nonnull AnActionEvent e) {
        Project project = e.getData(Project.KEY);
        if (project == null) {
            e.getPresentation().setEnabled(false);
            return;
        }

        List<ProjectSystemId> systemIds = getSystemIds(e);
        if (systemIds.isEmpty()) {
            e.getPresentation().setEnabled(false);
            return;
        }

        String name = StringUtil.join(systemIds, projectSystemId -> projectSystemId.getDisplayName().get(), ",");
        e.getPresentation().setTextValue(ExternalSystemLocalize.actionRefreshAllExternalProjects0Text(name));
        e.getPresentation().setDescriptionValue(ExternalSystemLocalize.actionRefreshAllExternalProjects0Description(name));

        ExternalSystemProcessingManager processingManager = Application.get().getInstance(ExternalSystemProcessingManager.class);
        e.getPresentation().setEnabled(!processingManager.hasTaskOfTypeInProgress(ExternalSystemTaskType.RESOLVE_PROJECT, project));
    }

    @Override
    @RequiredUIAccess
    public void actionPerformed(@Nonnull AnActionEvent e) {
        Project project = e.getRequiredData(Project.KEY);

        List<ProjectSystemId> systemIds = getSystemIds(e);
        if (systemIds.isEmpty()) {
            return;
        }

        // We save all documents because there is a possible case that there is an external system config file changed inside the ide.
        FileDocumentManager.getInstance().saveAllDocuments();

        for (ProjectSystemId externalSystemId : systemIds) {
            ExternalSystemUtil.refreshProjects(
                new ImportSpecBuilder(project, externalSystemId)
                    .forceWhenUptodate(true)
                    .use(ProgressExecutionMode.IN_BACKGROUND_ASYNC)
            );
        }
    }

    private static List<ProjectSystemId> getSystemIds(@Nonnull AnActionEvent e) {
        List<ProjectSystemId> systemIds = new ArrayList<>();

        ProjectSystemId externalSystemId = e.getData(ExternalSystemDataKeys.EXTERNAL_SYSTEM_ID);
        if (externalSystemId != null) {
            systemIds.add(externalSystemId);
        }
        else {
            Application.get().getExtensionPoint(ExternalSystemManager.class)
                .collectMapped(systemIds, ExternalSystemManager::getSystemId);
        }

        return systemIds;
    }
}
