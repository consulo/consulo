package consulo.ide.impl.idea.openapi.externalSystem.action;

import consulo.application.dumb.DumbAware;
import consulo.document.FileDocumentManager;
import consulo.externalSystem.ExternalSystemManager;
import consulo.externalSystem.model.ProjectSystemId;
import consulo.externalSystem.model.task.ExternalSystemTaskType;
import consulo.ide.ServiceManager;
import consulo.ide.impl.idea.openapi.externalSystem.model.ExternalSystemDataKeys;
import consulo.ide.impl.idea.openapi.externalSystem.service.internal.ExternalSystemProcessingManager;
import consulo.ide.impl.idea.openapi.externalSystem.util.ExternalSystemBundle;
import consulo.ide.impl.idea.openapi.externalSystem.util.ExternalSystemUtil;
import consulo.ide.impl.idea.openapi.util.text.StringUtil;
import consulo.ide.impl.idea.util.containers.ContainerUtil;
import consulo.language.editor.CommonDataKeys;
import consulo.project.Project;
import consulo.ui.ex.action.AnAction;
import consulo.ui.ex.action.AnActionEvent;

import java.util.List;

/**
 * Forces the ide to retrieve the most up-to-date info about the linked external projects and updates project state if necessary
 * (e.g. imports missing libraries).
 *
 * @author Denis Zhdanov
 * @since 1/23/12 3:48 PM
 */
public class RefreshAllExternalProjectsAction extends AnAction implements DumbAware, AnAction.TransparentUpdate {

  public RefreshAllExternalProjectsAction() {
    getTemplatePresentation().setText(ExternalSystemBundle.message("action.refresh.all.projects.text", "external"));
    getTemplatePresentation().setDescription(ExternalSystemBundle.message("action.refresh.all.projects.description", "external"));
  }

  @Override
  public void update(AnActionEvent e) {
    final Project project = e.getDataContext().getData(CommonDataKeys.PROJECT);
    if (project == null) {
      e.getPresentation().setEnabled(false);
      return;
    }

    final List<ProjectSystemId> systemIds = getSystemIds(e);
    if (systemIds.isEmpty()) {
      e.getPresentation().setEnabled(false);
      return;
    }

    final String name = StringUtil.join(systemIds, projectSystemId -> projectSystemId.getReadableName(), ",");
    e.getPresentation().setText(ExternalSystemBundle.message("action.refresh.all.projects.text", name));
    e.getPresentation().setDescription(ExternalSystemBundle.message("action.refresh.all.projects.description", name));

    ExternalSystemProcessingManager processingManager = ServiceManager.getService(ExternalSystemProcessingManager.class);
    e.getPresentation().setEnabled(!processingManager.hasTaskOfTypeInProgress(ExternalSystemTaskType.RESOLVE_PROJECT, project));
  }

  @Override
  public void actionPerformed(AnActionEvent e) {
    final Project project = e.getDataContext().getData(CommonDataKeys.PROJECT);
    if (project == null) {
      e.getPresentation().setEnabled(false);
      return;
    }

    final List<ProjectSystemId> systemIds = getSystemIds(e);
    if (systemIds.isEmpty()) {
      e.getPresentation().setEnabled(false);
      return;
    }

    // We save all documents because there is a possible case that there is an external system config file changed inside the ide.
    FileDocumentManager.getInstance().saveAllDocuments();

    for (ProjectSystemId externalSystemId : systemIds) {
      ExternalSystemUtil.refreshProjects(project, externalSystemId, true);
    }
  }

  private static List<ProjectSystemId> getSystemIds(AnActionEvent e) {
    final List<ProjectSystemId> systemIds = ContainerUtil.newArrayList();

    final ProjectSystemId externalSystemId = e.getDataContext().getData(ExternalSystemDataKeys.EXTERNAL_SYSTEM_ID);
    if (externalSystemId != null) {
      systemIds.add(externalSystemId);
    }
    else {
      for (ExternalSystemManager manager : ExternalSystemManager.EP_NAME.getExtensionList()) {
        systemIds.add(manager.getSystemId());
      }
    }

    return systemIds;
  }
}
