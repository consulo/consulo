package consulo.externalSystem.impl.internal.service.action;

import consulo.application.dumb.DumbAware;
import consulo.dataContext.DataContext;
import consulo.externalSystem.localize.ExternalSystemLocalize;
import consulo.externalSystem.model.ExternalSystemDataKeys;
import consulo.externalSystem.model.ProjectSystemId;
import consulo.externalSystem.model.project.ExternalProjectPojo;
import consulo.externalSystem.service.setting.ExternalSystemConfigLocator;
import consulo.externalSystem.ui.awt.ExternalSystemUiUtil;
import consulo.fileEditor.FileEditorManager;
import consulo.localize.LocalizeValue;
import consulo.navigation.OpenFileDescriptor;
import consulo.navigation.OpenFileDescriptorFactory;
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
 * @since 2013-07-16
 */
public class OpenExternalConfigAction extends AnAction implements DumbAware {
    public OpenExternalConfigAction() {
        super(ExternalSystemLocalize.actionOpenExternalConfigText(), ExternalSystemLocalize.actionOpenExternalConfigDescription());
    }

    @Override
    public void update(@Nonnull AnActionEvent e) {
        ProjectSystemId externalSystemId = e.getDataContext().getData(ExternalSystemDataKeys.EXTERNAL_SYSTEM_ID);
        if (externalSystemId != null) {
            LocalizeValue displayName = externalSystemId.getDisplayName();
            e.getPresentation().setTextValue(ExternalSystemLocalize.actionOpenExternalConfig0Text(displayName));
            e.getPresentation().setDescriptionValue(ExternalSystemLocalize.actionOpenExternalConfig0Description(displayName));
            e.getPresentation().setIcon(ExternalSystemUiUtil.getUiAware(externalSystemId).getProjectIcon());

            VirtualFile config = getExternalConfig(e.getDataContext());
            e.getPresentation().setEnabled(config != null);
        }
        else {
            e.getPresentation().setTextValue(ExternalSystemLocalize.actionOpenExternalConfigText());
            e.getPresentation().setDescriptionValue(ExternalSystemLocalize.actionOpenExternalConfigDescription());
            e.getPresentation().setIcon(null);
            e.getPresentation().setEnabled(false);
        }
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

        OpenFileDescriptor descriptor = OpenFileDescriptorFactory.getInstance(project).newBuilder(configFile).build();
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
