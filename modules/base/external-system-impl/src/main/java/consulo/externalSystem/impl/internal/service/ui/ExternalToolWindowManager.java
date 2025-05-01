package consulo.externalSystem.impl.internal.service.ui;

import consulo.externalSystem.ExternalSystemManager;
import consulo.externalSystem.model.ProjectSystemId;
import consulo.externalSystem.setting.AbstractExternalSystemSettings;
import consulo.externalSystem.setting.ExternalSystemSettingsListenerAdapter;
import consulo.project.Project;
import consulo.project.ui.wm.ToolWindowManager;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.awt.UIUtil;
import consulo.ui.ex.toolWindow.ToolWindow;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.util.Collection;
import java.util.Set;

/**
 * We want to hide an external system tool window when last external project is unlinked from the current ide project
 * and show it when the first external project is linked to the ide project.
 * <p>
 * This class encapsulates that functionality.
 *
 * @author Denis Zhdanov
 * @since 2013-06-14
 */
public class ExternalToolWindowManager {
    @SuppressWarnings("unchecked")
    public static void handle(@Nonnull Project project) {
        project.getApplication().getExtensionPoint(ExternalSystemManager.class).forEach(manager -> {
            AbstractExternalSystemSettings<?, ?, ?> settings =
                ((ExternalSystemManager<?, ?, ?, ?, ?>)manager).getSettingsProvider().apply(project);
            settings.subscribe(new ExternalSystemSettingsListenerAdapter() {
                @Override
                @RequiredUIAccess
                public void onProjectsLinked(@Nonnull Collection linked) {
                    if (settings.getLinkedProjectsSettings().size() != 1) {
                        return;
                    }
                    ToolWindow toolWindow = getToolWindow(project, manager.getSystemId());
                    if (toolWindow != null) {
                        toolWindow.setAvailable(true, null);
                    }
                }

                @Override
                public void onProjectsUnlinked(@Nonnull Set linkedProjectPaths) {
                    if (!settings.getLinkedProjectsSettings().isEmpty()) {
                        return;
                    }
                    ToolWindow toolWindow = getToolWindow(project, manager.getSystemId());
                    if (toolWindow != null) {
                        UIUtil.invokeLaterIfNeeded(() -> toolWindow.setAvailable(false, null));
                    }
                }
            });
        });
    }

    @Nullable
    private static ToolWindow getToolWindow(@Nonnull Project project, @Nonnull ProjectSystemId externalSystemId) {
        ToolWindowManager toolWindowManager = ToolWindowManager.getInstance(project);
        if (toolWindowManager == null) {
            return null;
        }
        ToolWindow result = toolWindowManager.getToolWindow(externalSystemId.getToolWindowId());
        if (result != null) {
            result.getContentManager(); // init content
        }
        return result;
    }
}
