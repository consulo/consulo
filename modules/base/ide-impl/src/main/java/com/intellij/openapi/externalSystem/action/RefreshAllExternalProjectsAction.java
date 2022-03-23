package com.intellij.openapi.externalSystem.action;

import consulo.ui.ex.action.AnAction;
import consulo.ui.ex.action.AnActionEvent;
import consulo.language.editor.CommonDataKeys;
import consulo.ide.ServiceManager;
import com.intellij.openapi.externalSystem.ExternalSystemManager;
import com.intellij.openapi.externalSystem.model.ExternalSystemDataKeys;
import com.intellij.openapi.externalSystem.model.ProjectSystemId;
import com.intellij.openapi.externalSystem.model.task.ExternalSystemTaskType;
import com.intellij.openapi.externalSystem.service.internal.ExternalSystemProcessingManager;
import com.intellij.openapi.externalSystem.util.ExternalSystemBundle;
import com.intellij.openapi.externalSystem.util.ExternalSystemUtil;
import consulo.document.FileDocumentManager;
import consulo.application.dumb.DumbAware;
import consulo.project.Project;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.util.Function;
import com.intellij.util.containers.ContainerUtil;

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

    final String name = StringUtil.join(systemIds, new Function<ProjectSystemId, String>() {
      @Override
      public String fun(ProjectSystemId projectSystemId) {
        return projectSystemId.getReadableName();
      }
    }, ",");
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
