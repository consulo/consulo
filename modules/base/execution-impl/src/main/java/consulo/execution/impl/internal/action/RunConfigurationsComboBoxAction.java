/*
 * Copyright 2000-2014 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package consulo.execution.impl.internal.action;

import consulo.annotation.component.ActionImpl;
import consulo.application.Application;
import consulo.application.ReadAction;
import consulo.application.dumb.DumbAware;
import consulo.application.dumb.IndexNotReadyException;
import consulo.dataContext.DataContext;
import consulo.dataContext.DataManager;
import consulo.execution.*;
import consulo.execution.configuration.ConfigurationType;
import consulo.execution.executor.Executor;
import consulo.execution.impl.internal.ExecutionManagerImpl;
import consulo.execution.impl.internal.action.runPopup.*;
import consulo.execution.internal.RunConfigurationStartHistory;
import consulo.execution.internal.RunCurrentFileExecutor;
import consulo.execution.internal.RunManagerEx;
import consulo.execution.localize.ExecutionLocalize;
import consulo.execution.ui.RunContentDescriptor;
import consulo.localize.LocalizeValue;
import consulo.platform.base.icon.PlatformIconGroup;
import consulo.platform.base.localize.ActionLocalize;
import consulo.project.Project;
import consulo.ui.ex.action.*;
import consulo.ui.ex.awt.action.ComboBoxAction;
import consulo.ui.ex.awt.action.ComboBoxButton;
import consulo.ui.ex.awt.popup.AWTListPopup;
import consulo.ui.ex.popup.JBPopup;
import consulo.ui.ex.popup.JBPopupFactory;
import consulo.ui.image.Image;
import consulo.ui.image.ImageEffects;
import consulo.util.collection.ContainerUtil;
import consulo.util.collection.SmartHashSet;
import consulo.util.dataholder.UserDataHolder;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import jakarta.inject.Inject;

import javax.swing.*;
import java.awt.*;
import java.util.List;
import java.util.Map;
import java.util.Set;

@ActionImpl(id = "RunConfiguration")
public class RunConfigurationsComboBoxAction extends ComboBoxAction implements DumbAware {
    private final Application myApplication;
    private final RunCurrentFileService myRunCurrentFileService;

    @Inject
    public RunConfigurationsComboBoxAction(Application application, RunCurrentFileService runCurrentFileService) {
        myApplication = application;
        myRunCurrentFileService = runCurrentFileService;
        getTemplatePresentation().setTextValue(ActionLocalize.actionRunconfigurationText());
        getTemplatePresentation().setTextValue(ActionLocalize.actionRunconfigurationDescription());
    }

    @Nonnull
    @Override
    public String getPopupActionPlace() {
        return ActionPlaces.RUN_CONFIGURATIONS_COMBOBOX;
    }

    @Override
    public void update(@Nonnull AnActionEvent e) {
        Presentation presentation = e.getPresentation();
        Project project = e.getData(Project.KEY);
        if (ActionPlaces.isMainMenuOrActionSearch(e.getPlace())) {
            presentation.setDescriptionValue(ExecutionLocalize.chooseRunConfigurationActionDescription());
        }
        try {
            if (project == null || project.isDisposed() || !project.isInitialized()) {
                updatePresentation(null, null, null, presentation, e.getPlace());
                presentation.setEnabled(false);
            }
            else {
                ExecutionTarget activeTarget = ExecutionTargetManager.getActiveTarget(project);
                RunnerAndConfigurationSettings selectedConfiguration = RunManagerEx.getInstanceEx(project).getSelectedConfiguration();
                updatePresentation(activeTarget, selectedConfiguration, project, presentation, e.getPlace());
                if (selectedConfiguration == null) {
                    Set<Image> defaultRunImages = new SmartHashSet<>();

                    for (Executor executor : RunCurrentFileExecutor.getExecutors(myApplication)) {
                        RunCurrentFileActionStatus status = ReadAction.compute(() -> myRunCurrentFileService.getRunCurrentFileActionStatus(executor, e, false));

                        for (RunnerAndConfigurationSettings runConfig : status.runConfigs()) {
                            defaultRunImages.add(runConfig.getConfiguration().getType().getIcon());
                        }
                    }

                    if (defaultRunImages.size() == 1) {
                        presentation.setIcon(ContainerUtil.getFirstItem(defaultRunImages));
                    }
                    else {
                        presentation.setIcon(Image.empty(Image.DEFAULT_ICON_SIZE));
                    }
                }
                presentation.setEnabled(true);
            }
        }
        catch (IndexNotReadyException e1) {
            presentation.setEnabled(false);
        }
    }

    public static void updatePresentation(
        @Nullable ExecutionTarget target,
        @Nullable RunnerAndConfigurationSettings settings,
        @Nullable Project project,
        @Nonnull Presentation presentation,
        @Nullable String actionPlace
    ) {
        if (project != null && target != null && settings != null) {
            String name = settings.getName();
            if (target != DefaultExecutionTarget.INSTANCE) {
                name += " | " + target.getDisplayName();
            }
            else {
                if (!settings.canRunOn(target)) {
                    name += " | Nothing to run on";
                }
            }
            presentation.setDisabledMnemonic(true);
            presentation.setTextValue(LocalizeValue.localizeTODO(name));
            presentation.putClientProperty(ComboBoxButton.LIKE_BUTTON, null);
            setConfigurationIcon(presentation, settings, project);
        }
        else {
            if (project != null) {
                presentation.setTextValue(ExecutionLocalize.runConfigurationsComboRunCurrentFileSelected());
                presentation.setIcon(Image.empty(Image.DEFAULT_ICON_SIZE));
                return;
            }

            presentation.setTextValue(ExecutionLocalize.runComboBoxAddConfiguration());
            presentation.putClientProperty(
                ComboBoxButton.LIKE_BUTTON,
                (Runnable) () -> ActionManager.getInstance().getAction(IdeActions.ACTION_EDIT_RUN_CONFIGURATIONS)
                    .actionPerformed(AnActionEvent.createFromDataContext("", null, DataManager.getInstance().getDataContext()))
            );
            presentation.setDescriptionValue(ActionLocalize.actionEditrunconfigurationsDescription());

            if (ActionPlaces.TOUCHBAR_GENERAL.equals(actionPlace)) {
                presentation.setIcon(PlatformIconGroup.generalAdd());
            }
            else {
                presentation.setIcon(null);
            }
        }
    }

    public static void setConfigurationIcon(Presentation presentation, RunnerAndConfigurationSettings settings, Project project) {
        try {
            Image icon = RunManagerEx.getInstanceEx(project).getConfigurationIcon(settings);
            ExecutionManagerImpl executionManager = ExecutionManagerImpl.getInstance(project);
            List<RunContentDescriptor> runningDescriptors = executionManager.getRunningDescriptors(s -> s == settings);
            if (runningDescriptors.size() == 1) {
                icon = ExecutionUtil.getIconWithLiveIndicator(icon);
            }
            else if (runningDescriptors.size() > 1) {
                icon = ImageEffects.withText(icon, String.valueOf(runningDescriptors.size()));
            }
            presentation.setIcon(icon);
        }
        catch (IndexNotReadyException ignored) {
        }
    }

    @Override
    public boolean shouldShowDisabledActions() {
        return true;
    }

    @Nonnull
    @Override
    public ActionGroup createPopupActionGroup(JComponent button) {
        Project project = DataManager.getInstance().getDataContext(button).getData(Project.KEY);
        if (project == null) {
            return ActionGroup.EMPTY_GROUP;
        }

        RunManagerEx runManager = RunManagerEx.getInstanceEx(project);

        ActionGroup.Builder allActionsGroup = ActionGroup.newImmutableBuilder();

        //allActionsGroup.add(new AllRunConfigurationsToggle());

        RunnerAndConfigurationSettings selected = RunManager.getInstance(project).getSelectedConfiguration();
        if (selected != null) {
            boolean added = false;
            ExecutionTarget activeTarget = ExecutionTargetManager.getActiveTarget(project);
            for (ExecutionTarget eachTarget : ExecutionTargetManager.getTargetsToChooseFor(project, selected.getConfiguration())) {
                allActionsGroup.add(new SelectTargetAction(project, eachTarget, eachTarget.equals(activeTarget)));
                added = true;
            }

            if (added) {
                allActionsGroup.add(createSeparatorWithTag(ActionFilterUtil.TAG_REGULAR_HIDE));
            }
        }

        // region hided elements
        List<ConfigurationType> types = runManager.getConfigurationFactories();
        for (ConfigurationType type : types) {
            DefaultActionGroup actionGroup = new DefaultActionGroup();
            Map<String, List<RunnerAndConfigurationSettings>> structure = runManager.getStructure(type);

            if (structure.isEmpty()) {
                continue;
            }


            for (Map.Entry<String, List<RunnerAndConfigurationSettings>> entry : structure.entrySet()) {
                DefaultActionGroup group = entry.getKey() != null ? new DefaultActionGroup(LocalizeValue.of(entry.getKey()), true) : actionGroup;
                group.getTemplatePresentation().setIcon(PlatformIconGroup.nodesFolder());
                for (RunnerAndConfigurationSettings settings : entry.getValue()) {
                    group.add(new SelectConfigAction(settings, project, ActionFilterUtil.TAG_REGULAR_HIDE));
                }

                if (group != actionGroup) {
                    actionGroup.add(group);
                }
            }

            allActionsGroup.add(actionGroup);
            allActionsGroup.add(createSeparatorWithTag(ActionFilterUtil.TAG_REGULAR_HIDE));
        }
        // endregion

        allActionsGroup.addSeparator();
        allActionsGroup.add(new RunCurrentFileAction());
        allActionsGroup.addSeparator();
        allActionsGroup.add(ActionManager.getInstance().getAction(IdeActions.ACTION_EDIT_RUN_CONFIGURATIONS));

//        RunnerAndConfigurationSettings selected = RunManager.getInstance(project).getSelectedConfiguration();
//        if (selected != null) {
//            ExecutionTarget activeTarget = ExecutionTargetManager.getActiveTarget(project);
//            for (ExecutionTarget eachTarget : ExecutionTargetManager.getTargetsToChooseFor(project, selected.getConfiguration())) {
//                allActionsGroup.add(new SelectTargetAction(project, eachTarget, eachTarget.equals(activeTarget)));
//            }
//            allActionsGroup.addSeparator();
//        }
//
//        List<ConfigurationType> types = runManager.getConfigurationFactories();
//        for (ConfigurationType type : types) {
//            DefaultActionGroup actionGroup = new DefaultActionGroup();
//            Map<String, List<RunnerAndConfigurationSettings>> structure = runManager.getStructure(type);
//            for (Map.Entry<String, List<RunnerAndConfigurationSettings>> entry : structure.entrySet()) {
//                DefaultActionGroup group = entry.getKey() != null ? new DefaultActionGroup(LocalizeValue.of(entry.getKey()), true) : actionGroup;
//                group.getTemplatePresentation().setIcon(PlatformIconGroup.nodesFolder());
//                for (RunnerAndConfigurationSettings settings : entry.getValue()) {
//                    group.add(new SelectConfigAction(settings, project));
//                }
//                if (group != actionGroup) {
//                    actionGroup.add(group);
//                }
//            }
//
//            allActionsGroup.add(actionGroup);
//            allActionsGroup.addSeparator();
//        }
        return allActionsGroup.build();
    }

    private AnSeparator createSeparatorWithTag(String tag) {
        AnSeparator separator = new AnSeparator();
        separator.getTemplatePresentation().putClientProperty(ActionFilterUtil.SEARCH_TAG, tag);
        return separator;
    }

    @Nonnull
    @Override
    public JBPopup createPopup(@Nonnull JComponent component, @Nonnull DataContext context, @Nonnull Runnable onDispose) {
        ActionGroup group = createPopupActionGroup(component, context);

        Project project = context.getRequiredData(Project.KEY);

        RunConfigurationStartHistory runConfigurationStartHistory = RunConfigurationStartHistory.getInstance(project);

        AWTListPopup popup = (AWTListPopup) JBPopupFactory.getInstance().createActionGroupPopup(
            getPopupTitle(),
            group,
            context,
            JBPopupFactory.ActionSelectionAid.SPEEDSEARCH,
            shouldShowDisabledActions(),
            onDispose,
            getMaxRows(),
            getPreselectCondition(),
            getPopupActionPlace(),
            (o, isHoldingFilter) -> {
                if (Boolean.TRUE) {
                    // TODO hack until support
                    return true;
                }

                if (!(o instanceof UserDataHolder userDataHolder)) {
                    return false;
                }

                String searchTag = userDataHolder.getUserData(ActionFilterUtil.SEARCH_TAG);
                return searchTag == null || switch (searchTag) {
                    case ActionFilterUtil.TAG_REGULAR_HIDE -> runConfigurationStartHistory.isAllConfigurationsExpanded();
                    default -> true;
                };

            }
        );

        popup.getListModel().syncModel();

        popup.setMinimumSize(new Dimension(270, 0));

        return popup;
    }
}
