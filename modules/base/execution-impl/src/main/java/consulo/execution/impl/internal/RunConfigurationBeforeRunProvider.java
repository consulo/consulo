/*
 * Copyright 2000-2012 JetBrains s.r.o.
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
package consulo.execution.impl.internal;

import consulo.annotation.component.ExtensionImpl;
import consulo.application.AllIcons;
import consulo.dataContext.DataContext;
import consulo.execution.*;
import consulo.execution.configuration.ConfigurationType;
import consulo.execution.configuration.RunConfiguration;
import consulo.execution.executor.DefaultRunExecutor;
import consulo.execution.executor.Executor;
import consulo.execution.impl.internal.configuration.RunManagerImpl;
import consulo.execution.internal.RunManagerEx;
import consulo.execution.localize.ExecutionLocalize;
import consulo.execution.runner.ExecutionEnvironment;
import consulo.execution.runner.ProgramRunner;
import consulo.logging.Logger;
import consulo.process.ExecutionException;
import consulo.process.ProcessHandler;
import consulo.process.event.ProcessEvent;
import consulo.process.event.ProcessListener;
import consulo.project.Project;
import consulo.ui.UIAccess;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.SimpleTextAttributes;
import consulo.ui.ex.awt.ColoredListCellRenderer;
import consulo.ui.ex.awt.DialogWrapper;
import consulo.ui.ex.awt.JBList;
import consulo.ui.ex.awt.ScrollPaneFactory;
import consulo.ui.image.Image;
import consulo.util.concurrent.AsyncResult;
import consulo.util.dataholder.Key;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import jakarta.inject.Inject;
import org.jdom.Attribute;
import org.jdom.Element;

import javax.swing.*;
import java.awt.*;
import java.util.List;
import java.util.*;

/**
 * @author Vassiliy Kudryashov
 */
@ExtensionImpl
public class RunConfigurationBeforeRunProvider extends BeforeRunTaskProvider<RunConfigurationBeforeRunProvider.RunConfigurableBeforeRunTask> {

  public static final Key<RunConfigurableBeforeRunTask> ID = Key.create("RunConfigurationTask");

  private static final Logger LOG = Logger.getInstance(RunConfigurationBeforeRunProvider.class);

  private final Project myProject;

  @Inject
  public RunConfigurationBeforeRunProvider(Project project) {
    myProject = project;
  }

  @Nonnull
  @Override
  public Key<RunConfigurableBeforeRunTask> getId() {
    return ID;
  }

  @Override
  public Image getIcon() {
    return AllIcons.Actions.Execute;
  }

  @Override
  public Image getTaskIcon(RunConfigurableBeforeRunTask task) {
    if (task.getSettings() == null) return null;
    return ProgramRunnerUtil.getConfigurationIcon(task.getSettings(), false);
  }

  @Nonnull
  @Override
  public String getName() {
    return ExecutionLocalize.beforeLaunchRunAnotherConfiguration().get();
  }

  @Nonnull
  @Override
  public String getDescription(RunConfigurableBeforeRunTask task) {
    if (task.getSettings() == null) {
      return ExecutionLocalize.beforeLaunchRunAnotherConfiguration().get();
    }
    else {
      return ExecutionLocalize.beforeLaunchRunCertainConfiguration(task.getSettings().getName()).get();
    }
  }

  @Override
  public boolean isConfigurable() {
    return true;
  }

  @Override
  @Nullable
  public RunConfigurableBeforeRunTask createTask(RunConfiguration runConfiguration) {
    if (runConfiguration.getProject().isInitialized()) {
      Collection<RunnerAndConfigurationSettings> configurations = RunManagerImpl.getInstanceImpl(runConfiguration.getProject()).getSortedConfigurations();
      if (configurations.isEmpty() || (configurations.size() == 1 && configurations.iterator().next().getConfiguration() == runConfiguration)) {
        return null;
      }
    }
    return new RunConfigurableBeforeRunTask();
  }

  @Nonnull
  @RequiredUIAccess
  @Override
  public AsyncResult<Void> configureTask(RunConfiguration runConfiguration, RunConfigurableBeforeRunTask task) {
    SelectionDialog dialog = new SelectionDialog(task.getSettings(), getAvailableConfigurations(runConfiguration));
    AsyncResult<Void> result = dialog.showAsync();
    result.doWhenDone(() -> {
      RunnerAndConfigurationSettings settings = dialog.getSelectedSettings();
      task.setSettings(settings);
    });
    return result;
  }

  @Nonnull
  private List<RunnerAndConfigurationSettings> getAvailableConfigurations(RunConfiguration runConfiguration) {
    Project project = runConfiguration.getProject();
    if (project == null || !project.isInitialized()) return Collections.emptyList();
    RunManagerImpl runManager = RunManagerImpl.getInstanceImpl(project);

    ArrayList<RunnerAndConfigurationSettings> configurations = new ArrayList<>(runManager.getSortedConfigurations());
    String executorId = DefaultRunExecutor.getRunExecutorInstance().getId();
    for (Iterator<RunnerAndConfigurationSettings> iterator = configurations.iterator(); iterator.hasNext(); ) {
      RunnerAndConfigurationSettings settings = iterator.next();
      ProgramRunner runner = ProgramRunnerUtil.getRunner(executorId, settings);
      if (runner == null || settings.getConfiguration() == runConfiguration) iterator.remove();
    }
    return configurations;
  }

  @Override
  public boolean canExecuteTask(RunConfiguration configuration, RunConfigurableBeforeRunTask task) {
    RunnerAndConfigurationSettings settings = task.getSettings();
    if (settings == null) {
      return false;
    }
    String executorId = DefaultRunExecutor.getRunExecutorInstance().getId();
    ProgramRunner runner = ProgramRunnerUtil.getRunner(executorId, settings);
    if (runner == null) return false;
    return runner.canRun(executorId, settings.getConfiguration());
  }

  @Nonnull
  @Override
  public AsyncResult<Void> executeTaskAsync(UIAccess uiAccess, DataContext context, RunConfiguration configuration, ExecutionEnvironment env, RunConfigurableBeforeRunTask task) {
    RunnerAndConfigurationSettings settings = task.getSettings();
    if (settings == null) {
      return AsyncResult.rejected();
    }
    Executor executor = DefaultRunExecutor.getRunExecutorInstance();
    String executorId = executor.getId();
    ProgramRunner runner = ProgramRunnerUtil.getRunner(executorId, settings);
    if (runner == null) return AsyncResult.rejected();
    ExecutionEnvironment environment = new ExecutionEnvironment(executor, runner, settings, myProject);
    environment.setExecutionId(env.getExecutionId());
    if (!ExecutionTargetManager.canRun(settings, env.getExecutionTarget())) {
      return AsyncResult.rejected();
    }

    if (!runner.canRun(executorId, environment.getRunProfile())) {
      return AsyncResult.rejected();
    }
    else {
      AsyncResult<Void> result = AsyncResult.undefined();

      uiAccess.give(() -> {
        try {
          runner.execute(environment, descriptor -> {
            ProcessHandler processHandler = descriptor != null ? descriptor.getProcessHandler() : null;
            if (processHandler != null) {
              processHandler.addProcessListener(new ProcessListener() {
                @Override
                public void processTerminated(ProcessEvent event) {
                  if(event.getExitCode() == 0) {
                    result.setDone();
                  }
                  else {
                    result.setRejected();
                  }
                }
              });
            }
          });
        }
        catch (ExecutionException e) {
          result.setRejected();
          LOG.error(e);
        }
      }).doWhenRejectedWithThrowable(result::rejectWithThrowable);

      return result;
    }
  }

  public class RunConfigurableBeforeRunTask extends BeforeRunTask<RunConfigurableBeforeRunTask> {
    private String myConfigurationName;
    private String myConfigurationType;
    private boolean myInitialized = false;

    private RunnerAndConfigurationSettings mySettings;

    RunConfigurableBeforeRunTask() {
      super(ID);
    }

    @Override
    public void writeExternal(Element element) {
      super.writeExternal(element);
      if (myConfigurationName != null && myConfigurationType != null) {
        element.setAttribute("run_configuration_name", myConfigurationName);
        element.setAttribute("run_configuration_type", myConfigurationType);
      }
      else if (mySettings != null) {
        element.setAttribute("run_configuration_name", mySettings.getName());
        element.setAttribute("run_configuration_type", mySettings.getType().getId());
      }
    }

    @Override
    public void readExternal(Element element) {
      super.readExternal(element);
      Attribute configurationNameAttr = element.getAttribute("run_configuration_name");
      Attribute configurationTypeAttr = element.getAttribute("run_configuration_type");
      myConfigurationName = configurationNameAttr != null ? configurationNameAttr.getValue() : null;
      myConfigurationType = configurationTypeAttr != null ? configurationTypeAttr.getValue() : null;
    }

    void init() {
      if (myInitialized) {
        return;
      }
      if (myConfigurationName != null && myConfigurationType != null) {
        Collection<RunnerAndConfigurationSettings> configurations = RunManagerImpl.getInstanceImpl(myProject).getSortedConfigurations();
        for (RunnerAndConfigurationSettings runConfiguration : configurations) {
          ConfigurationType type = runConfiguration.getType();
          if (myConfigurationName.equals(runConfiguration.getName()) && type != null && myConfigurationType.equals(type.getId())) {
            setSettings(runConfiguration);
            return;
          }
        }
      }
    }

    void setSettings(RunnerAndConfigurationSettings settings) {
      mySettings = settings;
      myInitialized = true;
    }

    public RunnerAndConfigurationSettings getSettings() {
      init();
      return mySettings;
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) return true;
      if (o == null || getClass() != o.getClass()) return false;
      if (!super.equals(o)) return false;

      RunConfigurableBeforeRunTask that = (RunConfigurableBeforeRunTask)o;

      if (myConfigurationName != null ? !myConfigurationName.equals(that.myConfigurationName) : that.myConfigurationName != null) return false;
      if (myConfigurationType != null ? !myConfigurationType.equals(that.myConfigurationType) : that.myConfigurationType != null) return false;

      return true;
    }

    @Override
    public int hashCode() {
      int result = super.hashCode();
      result = 31 * result + (myConfigurationName != null ? myConfigurationName.hashCode() : 0);
      result = 31 * result + (myConfigurationType != null ? myConfigurationType.hashCode() : 0);
      return result;
    }
  }

  private class SelectionDialog extends DialogWrapper {
    private RunnerAndConfigurationSettings mySelectedSettings;
    @Nonnull
    private final List<RunnerAndConfigurationSettings> mySettings;
    private JBList myJBList;

    private SelectionDialog(RunnerAndConfigurationSettings selectedSettings, @Nonnull List<RunnerAndConfigurationSettings> settings) {
      super(myProject);
      setTitle(ExecutionLocalize.beforeLaunchRunAnotherConfigurationChoose());
      mySelectedSettings = selectedSettings;
      mySettings = settings;
      init();
      myJBList.setSelectedValue(mySelectedSettings, true);
      FontMetrics fontMetrics = myJBList.getFontMetrics(myJBList.getFont());
      int maxWidth = fontMetrics.stringWidth("m") * 30;
      for (RunnerAndConfigurationSettings setting : settings) {
        maxWidth = Math.max(fontMetrics.stringWidth(setting.getConfiguration().getName()), maxWidth);
      }
      maxWidth += 24;//icon and gap
      myJBList.setMinimumSize(new Dimension(maxWidth, myJBList.getPreferredSize().height));
    }

    @Nullable
    @Override
    protected String getDimensionServiceKey() {
      return "consulo.execution.impl.internal.RunConfigurationBeforeRunProvider.dimensionServiceKey;";
    }

    @Override
    protected JComponent createCenterPanel() {
      myJBList = new JBList(mySettings);
      myJBList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
      myJBList.getSelectionModel().addListSelectionListener(e -> {
        Object selectedValue = myJBList.getSelectedValue();
        if (selectedValue instanceof RunnerAndConfigurationSettings) {
          mySelectedSettings = (RunnerAndConfigurationSettings)selectedValue;
        }
        else {
          mySelectedSettings = null;
        }
        setOKActionEnabled(mySelectedSettings != null);
      });
      myJBList.setCellRenderer(new ColoredListCellRenderer() {
        @Override
        protected void customizeCellRenderer(JList list, Object value, int index, boolean selected, boolean hasFocus) {
          if (value instanceof RunnerAndConfigurationSettings) {
            RunnerAndConfigurationSettings settings = (RunnerAndConfigurationSettings)value;
            RunManagerEx runManager = RunManagerEx.getInstanceEx(myProject);
            setIcon(runManager.getConfigurationIcon(settings));
            RunConfiguration configuration = settings.getConfiguration();
            append(configuration.getName(), settings.isTemporary() ? SimpleTextAttributes.GRAY_ATTRIBUTES : SimpleTextAttributes.REGULAR_ATTRIBUTES);
          }
        }
      });
      return ScrollPaneFactory.createScrollPane(myJBList);
    }

    public RunnerAndConfigurationSettings getSelectedSettings() {
      return mySelectedSettings;
    }
  }
}
