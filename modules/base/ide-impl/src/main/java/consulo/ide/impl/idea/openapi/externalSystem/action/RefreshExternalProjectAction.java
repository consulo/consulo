package consulo.ide.impl.idea.openapi.externalSystem.action;

import consulo.ui.ex.action.AnAction;
import consulo.ui.ex.action.AnActionEvent;
import consulo.language.editor.CommonDataKeys;
import consulo.ide.ServiceManager;
import consulo.externalSystem.model.DataNode;
import consulo.ide.impl.idea.openapi.externalSystem.model.ExternalSystemDataKeys;
import consulo.externalSystem.model.ProjectSystemId;
import consulo.externalSystem.service.project.ProjectData;
import consulo.ide.impl.idea.openapi.externalSystem.service.project.ExternalProjectRefreshCallback;
import consulo.ide.impl.idea.openapi.externalSystem.service.project.manage.ProjectDataManager;
import consulo.externalSystem.util.DisposeAwareProjectChange;
import consulo.externalSystem.util.ExternalSystemApiUtil;
import consulo.ide.impl.idea.openapi.externalSystem.util.ExternalSystemBundle;
import consulo.ide.impl.idea.openapi.externalSystem.util.ExternalSystemUtil;
import consulo.ide.impl.idea.openapi.externalSystem.service.execution.ProgressExecutionMode;
import consulo.document.FileDocumentManager;
import consulo.application.dumb.DumbAware;
import consulo.project.Project;
import consulo.module.content.internal.ProjectRootManagerEx;
import consulo.ui.annotation.RequiredUIAccess;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import java.util.Collections;

/**
 * * Forces the ide to retrieve the most up-to-date info about the linked external project and updates project state if necessary
 * (e.g. imports missing libraries).
 *
 * @author Vladislav.Soroka
 * @since 9/18/13
 */
public class RefreshExternalProjectAction extends AnAction implements DumbAware, AnAction.TransparentUpdate {

  public RefreshExternalProjectAction() {
    getTemplatePresentation().setText(ExternalSystemBundle.message("action.refresh.project.text", "external"));
    getTemplatePresentation().setDescription(ExternalSystemBundle.message("action.refresh.project.description", "external"));
  }

  @Override
  public void update(AnActionEvent e) {
    ExternalActionUtil.MyInfo info = ExternalActionUtil.getProcessingInfo(e.getDataContext());
    e.getPresentation().setEnabled(info.externalProject != null);
  }

  @Override
  public void actionPerformed(AnActionEvent e) {
    ExternalActionUtil.MyInfo info = ExternalActionUtil.getProcessingInfo(e.getDataContext());
    if (info.settings == null || info.localSettings == null || info.externalProject == null || info.ideProject == null
        || info.externalSystemId == null)
    {
      return;
    }
    ProjectSystemId externalSystemId = e.getDataContext().getData(ExternalSystemDataKeys.EXTERNAL_SYSTEM_ID);
    if (externalSystemId == null) {
      return;
    }

    final Project project = e.getDataContext().getData(CommonDataKeys.PROJECT);
    if (project == null) {
      e.getPresentation().setEnabled(false);
      return;
    }

    // We save all documents because there is a possible case that there is an external system config file changed inside the ide.
    FileDocumentManager.getInstance().saveAllDocuments();

    final ProjectDataManager projectDataManager = ServiceManager.getService(ProjectDataManager.class);
    ExternalSystemUtil.refreshProject(
      project, externalSystemId, info.externalProject.getPath(),
      new ExternalProjectRefreshCallback() {
        @Override
        public void onSuccess(@Nullable final DataNode<ProjectData> externalProject) {
          if (externalProject == null) {
            return;
          }
          ExternalSystemApiUtil.executeProjectChangeAction(true, new DisposeAwareProjectChange(project) {
            @RequiredUIAccess
            @Override
            public void execute() {
              ProjectRootManagerEx.getInstanceEx(project).mergeRootsChangesDuring(new Runnable() {
                @Override
                public void run() {
                  projectDataManager.importData(externalProject.getKey(), Collections.singleton(externalProject), project, true);
                }
              });
            }
          });
        }

        @Override
        public void onFailure(@Nonnull String errorMessage, @Nullable String errorDetails) {
        }
      }, false, ProgressExecutionMode.IN_BACKGROUND_ASYNC);
  }
}
