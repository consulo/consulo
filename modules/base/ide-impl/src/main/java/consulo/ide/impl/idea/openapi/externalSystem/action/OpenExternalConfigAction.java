package consulo.ide.impl.idea.openapi.externalSystem.action;

import consulo.application.dumb.DumbAware;
import consulo.dataContext.DataContext;
import consulo.externalSystem.ExternalSystemBundle;
import consulo.externalSystem.model.ExternalSystemDataKeys;
import consulo.externalSystem.model.ProjectSystemId;
import consulo.externalSystem.model.project.ExternalProjectPojo;
import consulo.externalSystem.service.setting.ExternalSystemConfigLocator;
import consulo.externalSystem.ui.awt.ExternalSystemUiUtil;
import consulo.fileEditor.FileEditorManager;
import consulo.fileEditor.impl.internal.OpenFileDescriptorImpl;
import consulo.project.Project;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.action.AnAction;
import consulo.ui.ex.action.AnActionEvent;
import consulo.virtualFileSystem.LocalFileSystem;
import consulo.virtualFileSystem.VirtualFile;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

/**
 * @author Denis Zhdanov
 * @since 7/16/13 2:19 PM
 */
public class OpenExternalConfigAction extends AnAction implements DumbAware {

  public OpenExternalConfigAction() {
    getTemplatePresentation().setText(ExternalSystemBundle.message("action.open.config.text", "external"));
    getTemplatePresentation().setDescription(ExternalSystemBundle.message("action.open.config.description", "external"));
  }

  @Override
  public void update(AnActionEvent e) {
    ProjectSystemId externalSystemId = e.getDataContext().getData(ExternalSystemDataKeys.EXTERNAL_SYSTEM_ID);
    if (externalSystemId == null) {
      e.getPresentation().setEnabled(false);
      return;
    }

    e.getPresentation().setText(ExternalSystemBundle.message("action.open.config.text", externalSystemId.getReadableName()));
    e.getPresentation().setDescription(ExternalSystemBundle.message("action.open.config.description", externalSystemId.getReadableName()));
    e.getPresentation().setIcon(ExternalSystemUiUtil.getUiAware(externalSystemId).getProjectIcon());

    VirtualFile config = getExternalConfig(e.getDataContext());
    e.getPresentation().setEnabled(config != null);
  }

  @Override
  @RequiredUIAccess
  public void actionPerformed(AnActionEvent e) {
    Project project = e.getDataContext().getData(Project.KEY);
    if (project == null) {
      return;
    }
    
    VirtualFile configFile = getExternalConfig(e.getDataContext());
    if (configFile == null) {
      return;
    }
    
    OpenFileDescriptorImpl descriptor = new OpenFileDescriptorImpl(project, configFile);
    FileEditorManager.getInstance(project).openTextEditor(descriptor, true); 
  }

  @Nullable
  private static VirtualFile getExternalConfig(@Nonnull DataContext context) {
    ProjectSystemId externalSystemId = context.getData(ExternalSystemDataKeys.EXTERNAL_SYSTEM_ID);
    if (externalSystemId == null) {
      return null;
    }

    ExternalProjectPojo projectPojo = context.getData(ExternalSystemDataKeys.SELECTED_PROJECT);
    if (projectPojo == null) {
      return null;
    }

    String path = projectPojo.getPath();
    LocalFileSystem fileSystem = LocalFileSystem.getInstance();
    VirtualFile externalSystemConfigPath = fileSystem.refreshAndFindFileByPath(path);
    if (externalSystemConfigPath == null) {
      return null;
    }

    VirtualFile toOpen = externalSystemConfigPath;
    for (ExternalSystemConfigLocator locator : ExternalSystemConfigLocator.EP_NAME.getExtensionList()) {
      if (externalSystemId.equals(locator.getTargetExternalSystemId())) {
        toOpen = locator.adjust(toOpen);
        if (toOpen == null) {
          return null;
        }
      }
    }
    return toOpen.isDirectory() ? null : toOpen;
  }
}
