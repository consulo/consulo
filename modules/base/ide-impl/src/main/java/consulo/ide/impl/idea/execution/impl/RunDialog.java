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

package consulo.ide.impl.idea.execution.impl;

import consulo.annotation.DeprecationInfo;
import consulo.application.HelpManager;
import consulo.configurable.ConfigurationException;
import consulo.execution.RunnerAndConfigurationSettings;
import consulo.execution.configuration.RunConfiguration;
import consulo.execution.executor.Executor;
import consulo.execution.localize.ExecutionLocalize;
import consulo.execution.runner.ExecutionEnvironment;
import consulo.ide.impl.idea.openapi.options.ex.SingleConfigurableEditor;
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

    public RunDialog(final Project project, final Executor executor) {
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
        return "#consulo.ide.impl.idea.execution.impl.RunDialog";
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
    public void setOKActionEnabled(final boolean isEnabled) {
        super.setOKActionEnabled(isEnabled);
    }

    @Override
    protected void dispose() {
        myConfigurable.disposeUIResources();
        super.dispose();
    }

    public static boolean editConfiguration(final Project project, final RunnerAndConfigurationSettings configuration, final String title) {
        return editConfiguration(project, configuration, title, null);
    }

    public static boolean editConfiguration(@Nonnull ExecutionEnvironment environment, @Nonnull String title) {
        return editConfiguration(environment.getProject(), environment.getRunnerAndConfigurationSettings(), title, environment.getExecutor());
    }

    public static boolean editConfiguration(final Project project, final RunnerAndConfigurationSettings configuration, final String title, @Nullable final Executor executor) {
        final SingleConfigurationConfigurable<RunConfiguration> configurable = SingleConfigurationConfigurable.editSettings(configuration, executor);
        final SingleConfigurableEditor dialog = new SingleConfigurableEditor(project, configurable, IdeModalityType.PROJECT) {
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
        public void actionPerformed(final ActionEvent event) {
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
