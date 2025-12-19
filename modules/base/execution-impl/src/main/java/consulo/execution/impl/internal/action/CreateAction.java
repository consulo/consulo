/*
 * Copyright 2000-2009 JetBrains s.r.o.
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

import consulo.execution.RunManager;
import consulo.execution.RunnerAndConfigurationSettings;
import consulo.execution.action.ConfigurationContext;
import consulo.execution.action.ConfigurationFromContext;
import consulo.execution.configuration.RunConfiguration;
import consulo.execution.impl.internal.configuration.RunManagerImpl;
import consulo.execution.internal.action.BaseRunConfigurationAction;
import consulo.execution.localize.ExecutionLocalize;
import consulo.execution.impl.internal.ui.RunDialog;
import consulo.localize.LocalizeValue;
import consulo.ui.ex.action.Presentation;
import jakarta.annotation.Nonnull;

import java.util.List;

public class CreateAction extends BaseRunConfigurationAction {
    public CreateAction() {
        super(ExecutionLocalize.createRunConfigurationActionName(), LocalizeValue.absent(), null);
    }

    @Override
    protected void perform(ConfigurationContext context) {
        choosePolicy(context).perform(context);
    }

    @Override
    protected void updatePresentation(
        Presentation presentation,
        @Nonnull String actionText,
        ConfigurationContext context
    ) {
        choosePolicy(context).update(presentation, context, actionText);
    }

    private static BaseCreatePolicy choosePolicy(ConfigurationContext context) {
        RunnerAndConfigurationSettings configuration = context.findExisting();
        if (configuration == null) {
            return CREATE_AND_EDIT;
        }
        RunManager runManager = context.getRunManager();
        if (runManager.getSelectedConfiguration() != configuration) {
            return SELECT;
        }
        if (configuration.isTemporary()) {
            return SAVE;
        }
        return SELECTED_STABLE;
    }


    private static abstract class BaseCreatePolicy {
        public enum ActionType {
            CREATE,
            SAVE,
            SELECT
        }

        private final ActionType myType;

        public BaseCreatePolicy(ActionType type) {
            myType = type;
        }

        public void update(Presentation presentation, ConfigurationContext context, @Nonnull String actionText) {
            updateText(presentation, actionText);
            updateIcon(presentation, context);
        }

        protected void updateIcon(Presentation presentation, ConfigurationContext context) {
            List<ConfigurationFromContext> fromContext = context.getConfigurationsFromContext();
            if (fromContext != null && fromContext.size() == 1) { //hide fuzzy icon when multiple run configurations are possible
                presentation.setIcon(fromContext.iterator().next().getConfiguration().getFactory().getIcon());
            }
        }

        protected void updateText(Presentation presentation, String actionText) {
            presentation.setText(generateName(actionText), false);
        }

        private String generateName(String actionText) {
            switch (myType) {
                case CREATE:
                    return ExecutionLocalize.createRunConfigurationForItemActionName(actionText).get();
                case SELECT:
                    return ExecutionLocalize.selectRunConfigurationForItemActionName(actionText).get();
                default:
                    return ExecutionLocalize.saveRunConfigurationForItemActionName(actionText).get();
            }
        }

        public abstract void perform(ConfigurationContext context);
    }

    private static class SelectPolicy extends BaseCreatePolicy {
        public SelectPolicy() {
            super(ActionType.SELECT);
        }

        @Override
        public void perform(ConfigurationContext context) {
            RunnerAndConfigurationSettings configuration = context.findExisting();
            if (configuration == null) {
                return;
            }
            context.getRunManager().setSelectedConfiguration(configuration);
        }

        @Override
        protected void updateIcon(Presentation presentation, ConfigurationContext context) {
            RunnerAndConfigurationSettings configuration = context.findExisting();
            if (configuration != null) {
                presentation.setIcon(configuration.getType().getIcon());
            }
            else {
                super.updateIcon(presentation, context);
            }
        }
    }

    private static class CreatePolicy extends BaseCreatePolicy {
        public CreatePolicy() {
            super(ActionType.CREATE);
        }

        @Override
        public void perform(ConfigurationContext context) {
            RunManagerImpl runManager = (RunManagerImpl) context.getRunManager();
            RunnerAndConfigurationSettings configuration = context.getConfiguration();
            RunnerAndConfigurationSettings template = runManager.getConfigurationTemplate(configuration.getFactory());
            RunConfiguration templateConfiguration = template.getConfiguration();
            runManager.addConfiguration(
                configuration,
                runManager.isConfigurationShared(template),
                runManager.getBeforeRunTasks(templateConfiguration),
                false
            );
            runManager.setSelectedConfiguration(configuration);
        }
    }

    private static class CreateAndEditPolicy extends CreatePolicy {
        @Override
        protected void updateText(Presentation presentation, String actionText) {
            presentation.setText(
                actionText.length() > 0
                    ? ExecutionLocalize.createRunConfigurationForItemActionName(actionText).get() + "..."
                    : ExecutionLocalize.createRunConfigurationActionName().get(),
                false
            );
        }

        @Override
        public void perform(ConfigurationContext context) {
            RunnerAndConfigurationSettings configuration = context.getConfiguration();
            if (RunDialog.editConfiguration(
                context.getProject(),
                configuration,
                ExecutionLocalize.createRunConfigurationForItemDialogTitle(configuration.getName()).get()
            )) {
                RunManagerImpl runManager = (RunManagerImpl) context.getRunManager();
                runManager.addConfiguration(
                    configuration,
                    runManager.isConfigurationShared(configuration),
                    runManager.getBeforeRunTasks(configuration.getConfiguration()),
                    false
                );
                runManager.setSelectedConfiguration(configuration);
            }
        }
    }

    private static class SavePolicy extends BaseCreatePolicy {
        public SavePolicy() {
            super(ActionType.SAVE);
        }

        @Override
        public void perform(ConfigurationContext context) {
            RunnerAndConfigurationSettings settings = context.findExisting();
            if (settings != null) {
                context.getRunManager().makeStable(settings);
            }
        }

        @Override
        protected void updateIcon(Presentation presentation, ConfigurationContext context) {
            RunnerAndConfigurationSettings configuration = context.findExisting();
            if (configuration != null) {
                presentation.setIcon(configuration.getType().getIcon());
            }
            else {
                super.updateIcon(presentation, context);
            }
        }
    }

    private static final BaseCreatePolicy CREATE_AND_EDIT = new CreateAndEditPolicy();
    private static final BaseCreatePolicy SELECT = new SelectPolicy();
    private static final BaseCreatePolicy SAVE = new SavePolicy();
    private static final BaseCreatePolicy SELECTED_STABLE = new BaseCreatePolicy(BaseCreatePolicy.ActionType.SELECT) {
        @Override
        public void perform(ConfigurationContext context) {
        }

        @Override
        public void update(Presentation presentation, ConfigurationContext context, @Nonnull String actionText) {
            super.update(presentation, context, actionText);
            presentation.setVisible(false);
        }
    };
}
