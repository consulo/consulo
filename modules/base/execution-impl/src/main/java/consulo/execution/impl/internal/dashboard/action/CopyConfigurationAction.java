// Copyright 2000-2022 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.execution.impl.internal.dashboard.action;

import consulo.annotation.component.ActionImpl;
import consulo.execution.RunConfigurationEditor;
import consulo.execution.RunManager;
import consulo.execution.RunnerAndConfigurationSettings;
import consulo.execution.configuration.ConfigurationFactory;
import consulo.execution.configuration.RunConfiguration;
import consulo.execution.dashboard.RunDashboardRunConfigurationNode;
import consulo.execution.impl.internal.configuration.RunnerAndConfigurationSettingsImpl;
import consulo.execution.localize.ExecutionLocalize;
import consulo.platform.base.icon.PlatformIconGroup;
import consulo.project.Project;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.action.ActionPlaces;
import consulo.ui.ex.action.AnAction;
import consulo.ui.ex.action.AnActionEvent;
import consulo.ui.ex.action.Presentation;
import consulo.ui.image.Image;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

/**
 * @author konstantin.aleev
 */
@ActionImpl(id = "RunDashboard.CopyConfiguration")
public final class CopyConfigurationAction extends AnAction {

//  @Override
//  public @NotNull ActionUpdateThread getActionUpdateThread() {
//    return ActionUpdateThread.BGT;
//  }


    @Nullable
    @Override
    protected Image getTemplateIcon() {
        return PlatformIconGroup.actionsCopy();
    }

    @RequiredUIAccess
    @Override
    public void update(@Nonnull AnActionEvent e) {
        Project project = e.getData(Project.KEY);
        RunDashboardRunConfigurationNode node = project == null ? null : RunDashboardActionUtils.getTarget(e);
        boolean enabled = node != null && RunManager.getInstance(project).hasSettings(node.getConfigurationSettings());
        Presentation presentation = e.getPresentation();
        presentation.setEnabled(enabled);
        boolean popupPlace = ActionPlaces.isPopupPlace(e.getPlace());
        presentation.setVisible(enabled || !popupPlace);
        if (popupPlace) {
            presentation.setText(getTemplatePresentation().getText() + "...");
        }
    }

    @RequiredUIAccess
    @Override
    public void actionPerformed(@Nonnull AnActionEvent e) {
        Project project = e.getData(Project.KEY);
        RunDashboardRunConfigurationNode node = project == null ? null : RunDashboardActionUtils.getTarget(e);
        if (node == null) {
            return;
        }

        RunManager runManager = RunManager.getInstance(project);
        RunnerAndConfigurationSettings settings = node.getConfigurationSettings();

        RunnerAndConfigurationSettings copiedSettings = ((RunnerAndConfigurationSettingsImpl) settings).clone();
        runManager.setUniqueNameIfNeed(copiedSettings);
        copiedSettings.setFolderName(settings.getFolderName());
        //FIXME copiedSettings.getConfiguration().setBeforeRunTasks(settings.getConfiguration().getBeforeRunTasks());

        final ConfigurationFactory factory = settings.getFactory();
        RunConfiguration configuration = settings.getConfiguration();
        factory.onConfigurationCopied(configuration);

        boolean edited = RunConfigurationEditor.getInstance(project)
            .editConfiguration(project, copiedSettings, ExecutionLocalize.runDashboardEditConfigurationDialogTitle());

        if (edited) {
            runManager.addConfiguration(copiedSettings);
        }
    }
}
