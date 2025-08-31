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

package consulo.execution.impl.internal.ui;

import consulo.annotation.DeprecationInfo;
import consulo.application.HelpManager;
import consulo.configurable.ConfigurationException;
import consulo.execution.RunnerAndConfigurationSettings;
import consulo.execution.configuration.RunConfiguration;
import consulo.execution.executor.Executor;
import consulo.execution.localize.ExecutionLocalize;
import consulo.execution.runner.ExecutionEnvironment;
import consulo.project.Project;
import consulo.ui.ex.awt.*;
import consulo.ui.ex.awtUnsafe.TargetAWT;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import javax.swing.*;
import java.awt.event.ActionEvent;

@Deprecated
@DeprecationInfo("Use RunConfigurationEditor")
public class RunDialog extends DialogWrapper implements RunConfigurable.RunDialogBase {
    public static final String HELP_ID = "reference.dialogs.rundebug";

    private final Project myProject;
    private final RunConfigurable myConfigurable;
    private JComponent myCenterPanel;
    private final Executor myExecutor;

    public RunDialog(Project project, Executor executor) {
        super(project, true);
        myProject = project;
        myExecutor = executor;

        TitlelessDecorator titlelessDecorator = TitlelessDecorator.of(getRootPane());

        setTitle(executor.getActionName());

        setOKButtonText(executor.getStartActionText());
        setOKButtonIcon(TargetAWT.to(executor.getIcon()));

        myConfigurable = new RunConfigurable(project, this, titlelessDecorator);

        init();

        titlelessDecorator.install(getWindow());

        myConfigurable.reset();
    }

    @Override
    @Nonnull
    protected Action[] createActions() {
        return new Action[]{getOKAction(), getCancelAction(), new ApplyAction(), getHelpAction()};
    }

    @Override
    protected void doHelpAction() {
        HelpManager.getInstance().invokeHelp(HELP_ID);
    }

    @Override
    protected String getDimensionServiceKey() {
        return "#consulo.execution.impl.internal.ui.RunDialog";
    }

    @Override
    public JComponent getPreferredFocusedComponent() {
        return IdeFocusTraversalPolicy.getPreferredFocusedComponent(myCenterPanel);
    }

    @Override
    protected void doOKAction() {
        try {
            myConfigurable.apply();
        }
        catch (ConfigurationException e) {
            Messages.showMessageDialog(myProject, e.getMessage(), ExecutionLocalize.invalidDataDialogTitle().get(), UIUtil.getErrorIcon());
            return;
        }
        super.doOKAction();
    }

    @Override
    protected JComponent createCenterPanel() {
        myCenterPanel = myConfigurable.createComponent();
        return myCenterPanel;
    }

    @Override
    public void setOKActionEnabled(boolean isEnabled) {
        super.setOKActionEnabled(isEnabled);
    }

    @Override
    protected void dispose() {
        myConfigurable.disposeUIResources();
        super.dispose();
    }

    public static boolean editConfiguration(Project project, RunnerAndConfigurationSettings configuration, String title) {
        return editConfiguration(project, configuration, title, null);
    }

    public static boolean editConfiguration(@Nonnull ExecutionEnvironment environment, @Nonnull String title) {
        return editConfiguration(environment.getProject(), environment.getRunnerAndConfigurationSettings(), title, environment.getExecutor());
    }

    public static boolean editConfiguration(final Project project, RunnerAndConfigurationSettings configuration, String title, @Nullable final Executor executor) {
        final SingleConfigurationConfigurable<RunConfiguration> configurable = SingleConfigurationConfigurable.editSettings(configuration, executor);
        SingleConfigurableEditor dialog = new SingleConfigurableEditor(project, configurable, IdeModalityType.PROJECT) {
            {
                if (executor != null) {
                    setOKButtonText(executor.getActionName());
                }
                if (executor != null) {
                    setOKButtonIcon(TargetAWT.to(executor.getIcon()));
                }
            }
        };

        dialog.setTitle(title);
        dialog.show();
        return dialog.isOK();
    }

    private class ApplyAction extends AbstractAction {
        public ApplyAction() {
            super(ExecutionLocalize.applyActionName().get());
        }

        @Override
        public void actionPerformed(ActionEvent event) {
            try {
                myConfigurable.apply();
            }
            catch (ConfigurationException e) {
                Messages.showMessageDialog(myProject, e.getMessage(), ExecutionLocalize.invalidDataDialogTitle().get(), UIUtil.getErrorIcon());
            }
        }
    }

    @Override
    public Executor getExecutor() {
        return myExecutor;
    }
}
