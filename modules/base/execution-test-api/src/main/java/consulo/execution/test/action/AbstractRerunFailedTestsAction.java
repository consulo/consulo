/*
 * Copyright 2000-2015 JetBrains s.r.o.
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
package consulo.execution.test.action;

import consulo.execution.runner.RunnerRegistry;
import consulo.execution.RuntimeConfigurationException;
import consulo.execution.configuration.*;
import consulo.execution.configuration.log.LogFileOptions;
import consulo.execution.configuration.log.PredefinedLogFile;
import consulo.execution.configuration.ui.SettingsEditor;
import consulo.execution.debug.DefaultDebugExecutor;
import consulo.execution.executor.DefaultRunExecutor;
import consulo.execution.executor.Executor;
import consulo.execution.runner.ExecutionEnvironment;
import consulo.execution.runner.ExecutionEnvironmentBuilder;
import consulo.execution.runner.ProgramRunner;
import consulo.execution.test.AbstractTestProxy;
import consulo.execution.test.Filter;
import consulo.execution.test.TestConsoleProperties;
import consulo.execution.test.TestFrameworkRunningModel;
import consulo.language.psi.scope.GlobalSearchScope;
import consulo.logging.Logger;
import consulo.platform.base.localize.ActionLocalize;
import consulo.process.ExecutionException;
import consulo.project.Project;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.ComponentContainer;
import consulo.ui.ex.action.ActionManager;
import consulo.ui.ex.action.AnAction;
import consulo.ui.ex.action.AnActionEvent;
import consulo.ui.ex.action.Presentation;
import consulo.ui.ex.awt.ColoredListCellRenderer;
import consulo.ui.ex.popup.IPopupChooserBuilder;
import consulo.ui.ex.popup.JBPopupFactory;
import consulo.util.xml.serializer.InvalidDataException;
import consulo.util.xml.serializer.WriteExternalException;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.jdom.Element;
import org.jetbrains.annotations.TestOnly;

import javax.swing.*;
import java.awt.event.InputEvent;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.function.Supplier;

/**
 * @author anna
 * @since 2008-12-24
 */
public class AbstractRerunFailedTestsAction extends AnAction {
  private static final Logger LOG = Logger.getInstance(AbstractRerunFailedTestsAction.class);

  private TestFrameworkRunningModel myModel;
  private Supplier<TestFrameworkRunningModel> myModelProvider;
  protected TestConsoleProperties myConsoleProperties;

  protected AbstractRerunFailedTestsAction(@Nonnull ComponentContainer componentContainer) {
    copyFrom(ActionManager.getInstance().getAction("RerunFailedTests"));
    registerCustomShortcutSet(getShortcutSet(), componentContainer.getComponent());
  }

  public void init(TestConsoleProperties consoleProperties) {
    myConsoleProperties = consoleProperties;
  }

  public void setModel(TestFrameworkRunningModel model) {
    myModel = model;
  }

  public void setModelProvider(Supplier<TestFrameworkRunningModel> modelProvider) {
    myModelProvider = modelProvider;
  }

  @Override
  public final void update(@Nonnull AnActionEvent e) {
    e.getPresentation().setEnabled(isActive(e));
  }

  private boolean isActive(AnActionEvent e) {
    Project project = e.getData(Project.KEY);
    if (project == null) {
      return false;
    }

    TestFrameworkRunningModel model = getModel();
    if (model == null || model.getRoot() == null) {
      return false;
    }
    Filter filter = getFailuresFilter();
    for (AbstractTestProxy test : model.getRoot().getAllTests()) {
      //noinspection unchecked
      if (filter.shouldAccept(test)) {
        return true;
      }
    }
    return false;
  }

  @Nonnull
  protected List<AbstractTestProxy> getFailedTests(@Nonnull Project project) {
    TestFrameworkRunningModel model = getModel();
    if (model == null) return Collections.emptyList();
    //noinspection unchecked
    return getFilter(project, model.getProperties().getScope()).select(model.getRoot().getAllTests());
  }

  @Nonnull
  protected Filter getFilter(@Nonnull Project project, @Nonnull GlobalSearchScope searchScope) {
    return getFailuresFilter();
  }

  protected Filter<?> getFailuresFilter() {
    return getFailuresFilter(myConsoleProperties);
  }

  @TestOnly
  public static Filter<?> getFailuresFilter(TestConsoleProperties consoleProperties) {
    if (TestConsoleProperties.INCLUDE_NON_STARTED_IN_RERUN_FAILED.value(consoleProperties)) {
      return Filter.NOT_PASSED.or(Filter.FAILED_OR_INTERRUPTED).and(Filter.IGNORED.not());
    }
    return Filter.FAILED_OR_INTERRUPTED.and(Filter.IGNORED.not());
  }

  @Override
  @RequiredUIAccess
  public void actionPerformed(@Nonnull AnActionEvent e) {
    ExecutionEnvironment environment = e.getData(ExecutionEnvironment.KEY);
    if (environment == null) {
      return;
    }

    execute(e, environment);
  }

  public void execute(@Nonnull AnActionEvent e, @Nonnull ExecutionEnvironment environment) {
    MyRunProfile profile = getRunProfile(environment);
    if (profile == null) {
      return;
    }

    final ExecutionEnvironmentBuilder environmentBuilder = new ExecutionEnvironmentBuilder(environment).runProfile(profile);

    final InputEvent event = e.getInputEvent();
    if (!(event instanceof MouseEvent) || !event.isShiftDown()) {
      performAction(environmentBuilder);
      return;
    }

    final LinkedHashMap<Executor, ProgramRunner> availableRunners = new LinkedHashMap<>();
    for (Executor ex : new Executor[]{DefaultRunExecutor.getRunExecutorInstance(), DefaultDebugExecutor.getDebugExecutorInstance()}) {
      final ProgramRunner runner = RunnerRegistry.getInstance().getRunner(ex.getId(), profile);
      if (runner != null) {
        availableRunners.put(ex, runner);
      }
    }

    if (availableRunners.isEmpty()) {
      LOG.error(environment.getExecutor().getActionName() + " is not available now");
    }
    else if (availableRunners.size() == 1) {
      //noinspection ConstantConditions
      performAction(environmentBuilder.runner(availableRunners.get(environment.getExecutor())));
    }
    else {
      IPopupChooserBuilder<Executor> builder = JBPopupFactory.getInstance().createPopupChooserBuilder(new ArrayList<>(availableRunners.keySet()));
      builder.setSelectedValue(environment.getExecutor(), true);
      builder.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
      builder.setRenderer(new ColoredListCellRenderer<Executor>() {
        @Override
        protected void customizeCellRenderer(@Nonnull JList<? extends Executor> list, Executor value, int index, boolean selected, boolean hasFocus) {
          append(value.getStartActionText().map(Presentation.NO_MNEMONIC).get());
          setIcon(value.getIcon());
        }
      });
      builder.setItemSelectedCallback(executor -> {
        if (executor != null) {
          //noinspection ConstantConditions
          performAction(environmentBuilder.runner(availableRunners.get(executor)).executor(executor));
        }
      });
      builder.setMovable(false).setResizable(false).setRequestFocus(true);
      builder.setTitle("Restart Failed Tests");

      builder.createPopup().showUnderneathOf(event.getComponent());
    }
  }

  private static void performAction(@Nonnull ExecutionEnvironmentBuilder builder) {
    ExecutionEnvironment environment = builder.build();
    try {
      environment.getRunner().execute(environment);
    }
    catch (ExecutionException e) {
      LOG.error(e);
    }
    finally {
      ((MyRunProfile)environment.getRunProfile()).clear();
    }
  }

  @Deprecated
  public MyRunProfile getRunProfile() {
    return null;
  }

  @Nullable
  protected MyRunProfile getRunProfile(@Nonnull ExecutionEnvironment environment) {
    //noinspection deprecation
    return getRunProfile();
  }

  @Nullable
  public TestFrameworkRunningModel getModel() {
    if (myModel != null) {
      return myModel;
    }
    if (myModelProvider != null) {
      return myModelProvider.get();
    }
    return null;
  }

  protected static abstract class MyRunProfile extends RunConfigurationBase implements ModuleRunProfile, WrappingRunConfiguration<RunConfigurationBase> {
    @Deprecated
    public RunConfigurationBase getConfiguration() {
      return getPeer();
    }

    @Override
    public RunConfigurationBase getPeer() {
      return myConfiguration;
    }

    private final RunConfigurationBase myConfiguration;

    public MyRunProfile(RunConfigurationBase configuration) {
      super(configuration.getProject(), configuration.getFactory(), ActionLocalize.actionRerunfailedtestsText().get());
      myConfiguration = configuration;
    }

    public void clear() {
    }

    @Override
    public void checkConfiguration() throws RuntimeConfigurationException {
    }

    ///////////////////////////////////Delegates
    @Override
    public void readExternal(final Element element) throws InvalidDataException {
      myConfiguration.readExternal(element);
    }

    @Override
    public void writeExternal(final Element element) throws WriteExternalException {
      myConfiguration.writeExternal(element);
    }

    @Override
    @Nonnull
    public SettingsEditor<? extends RunConfiguration> getConfigurationEditor() {
      return myConfiguration.getConfigurationEditor();
    }

    @Override
    @Nonnull
    public ConfigurationType getType() {
      return myConfiguration.getType();
    }

    @Override
    public ConfigurationPerRunnerSettings createRunnerSettings(final ConfigurationInfoProvider provider) {
      return myConfiguration.createRunnerSettings(provider);
    }

    @Override
    public SettingsEditor<ConfigurationPerRunnerSettings> getRunnerSettingsEditor(final ProgramRunner runner) {
      return myConfiguration.getRunnerSettingsEditor(runner);
    }

    @Override
    public RunConfiguration clone() {
      return myConfiguration.clone();
    }

    @Override
    public int getUniqueID() {
      return myConfiguration.getUniqueID();
    }

    @Override
    public LogFileOptions getOptionsForPredefinedLogFile(PredefinedLogFile predefinedLogFile) {
      return myConfiguration.getOptionsForPredefinedLogFile(predefinedLogFile);
    }

    @Override
    public ArrayList<PredefinedLogFile> getPredefinedLogFiles() {
      return myConfiguration.getPredefinedLogFiles();
    }

    @Nonnull
    @Override
    public ArrayList<LogFileOptions> getAllLogFiles() {
      return myConfiguration.getAllLogFiles();
    }

    @Override
    public ArrayList<LogFileOptions> getLogFiles() {
      return myConfiguration.getLogFiles();
    }
  }
}
