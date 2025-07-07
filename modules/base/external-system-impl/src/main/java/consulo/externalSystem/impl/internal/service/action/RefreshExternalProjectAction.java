package consulo.externalSystem.impl.internal.service.action;

import consulo.application.Application;
import consulo.application.dumb.DumbAware;
import consulo.document.FileDocumentManager;
import consulo.externalSystem.impl.internal.util.ExternalSystemUtil;
import consulo.externalSystem.localize.ExternalSystemLocalize;
import consulo.externalSystem.model.DataNode;
import consulo.externalSystem.model.ExternalSystemDataKeys;
import consulo.externalSystem.model.ProjectSystemId;
import consulo.externalSystem.model.task.ProgressExecutionMode;
import consulo.externalSystem.service.project.ExternalProjectRefreshCallback;
import consulo.externalSystem.service.project.ProjectData;
import consulo.externalSystem.service.project.manage.ProjectDataManager;
import consulo.externalSystem.util.DisposeAwareProjectChange;
import consulo.externalSystem.util.ExternalSystemApiUtil;
import consulo.localize.LocalizeValue;
import consulo.module.content.internal.ProjectRootManagerEx;
import consulo.project.Project;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.action.AnAction;
import consulo.ui.ex.action.AnActionEvent;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.util.Collections;

/**
 * Forces the ide to retrieve the most up-to-date info about the linked external project and updates project state if necessary
 * (e.g. imports missing libraries).
 *
 * @author Vladislav.Soroka
 * @since 2013-09-18
 */
public class RefreshExternalProjectAction extends AnAction implements DumbAware {

  public RefreshExternalProjectAction() {
    super(
        ExternalSystemLocalize.actionRefreshExternalProjectText(),
        ExternalSystemLocalize.actionRefreshExternalProjectDescription()
    );
  }

  @Override
  public void update(@Nonnull AnActionEvent e) {
    ExternalActionUtil.MyInfo info = ExternalActionUtil.getProcessingInfo(e.getDataContext());
    e.getPresentation().setEnabled(info.externalProject != null);
    if (info.externalSystemId != null) {
      LocalizeValue displayName = info.externalSystemId.getDisplayName();
      e.getPresentation().setTextValue(ExternalSystemLocalize.actionRefreshExternalProject0Text(displayName));
      e.getPresentation().setDescriptionValue(ExternalSystemLocalize.actionRefreshExternalProject0Description(displayName));
    }
    else {
      e.getPresentation().setTextValue(ExternalSystemLocalize.actionRefreshExternalProjectText());
      e.getPresentation().setDescriptionValue(ExternalSystemLocalize.actionRefreshExternalProjectDescription());
    }
  }

  @Override
  @RequiredUIAccess
  public void actionPerformed(@Nonnull AnActionEvent e) {
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

    final Project project = e.getDataContext().getData(Project.KEY);
    if (project == null) {
      e.getPresentation().setEnabled(false);
      return;
    }

    // We save all documents because there is a possible case that there is an external system config file changed inside the ide.
    FileDocumentManager.getInstance().saveAllDocuments();

    final ProjectDataManager projectDataManager = Application.get().getInstance(ProjectDataManager.class);
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
