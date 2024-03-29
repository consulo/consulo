/*
 * Copyright 2000-2016 JetBrains s.r.o.
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
package consulo.ide.impl.idea.execution.dashboard;

import consulo.annotation.component.ServiceImpl;
import consulo.application.AllIcons;
import consulo.application.util.registry.Registry;
import consulo.component.messagebus.MessageBusConnection;
import consulo.component.persist.PersistentStateComponent;
import consulo.component.persist.State;
import consulo.component.persist.Storage;
import consulo.component.persist.StoragePathMacros;
import consulo.disposer.Disposer;
import consulo.execution.RunManager;
import consulo.execution.RunnerAndConfigurationSettings;
import consulo.execution.configuration.RunConfiguration;
import consulo.execution.event.ExecutionListener;
import consulo.execution.event.RunManagerListener;
import consulo.execution.event.RunManagerListenerEvent;
import consulo.execution.runner.ExecutionEnvironment;
import consulo.execution.ui.RunContentDescriptor;
import consulo.ide.impl.idea.execution.dashboard.tree.DashboardGrouper;
import consulo.ide.impl.idea.execution.impl.ExecutionManagerImpl;
import consulo.ide.impl.idea.openapi.util.Comparing;
import consulo.process.ProcessHandler;
import consulo.project.Project;
import consulo.project.event.DumbModeListener;
import consulo.project.ui.wm.ToolWindowId;
import consulo.project.ui.wm.ToolWindowManager;
import consulo.ui.ex.content.Content;
import consulo.ui.ex.content.ContentFactory;
import consulo.ui.ex.content.ContentManager;
import consulo.ui.ex.content.ContentUI;
import consulo.ui.ex.toolWindow.ToolWindow;
import consulo.ui.ex.toolWindow.ToolWindowAnchor;
import consulo.ui.image.Image;
import consulo.util.lang.Pair;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * @author konstantin.aleev
 */
@Singleton
@State(name = "RunDashboard", storages = @Storage(StoragePathMacros.WORKSPACE_FILE))
@ServiceImpl
public class RunDashboardManagerImpl implements RunDashboardManager, PersistentStateComponent<RunDashboardManagerImpl.State> {
  @Nonnull
  private final Project myProject;
  @Nonnull
  private final ContentManager myContentManager;
  @Nonnull
  private final List<DashboardGrouper> myGroupers;

  private RunDashboardContent myDashboardContent;

  @Inject
  public RunDashboardManagerImpl(@Nonnull final Project project) {
    myProject = project;

    ContentFactory contentFactory = ContentFactory.getInstance();
    ContentUI contentUI = new PanelContentUI();
    myContentManager = contentFactory.createContentManager(contentUI, false, project);

    myGroupers = new ArrayList<>(DashboardGroupingRule.EP_NAME.getExtensionList()).stream().sorted(DashboardGroupingRule.PRIORITY_COMPARATOR).map(DashboardGrouper::new).collect(Collectors.toList());

    if (isDashboardEnabled()) {
      initToolWindowListeners();
    }
  }

  private static boolean isDashboardEnabled() {
    return Registry.is("ide.run.dashboard") && RunDashboardContributor.EP_NAME.hasAnyExtensions();
  }

  private void initToolWindowListeners() {
    myProject.getMessageBus().connect().subscribe(RunManagerListener.class, new RunManagerListener() {
      @Override
      public void runConfigurationAdded(@Nonnull RunManagerListenerEvent event) {
        updateDashboardIfNeeded(event.getSettings());
      }

      @Override
      public void runConfigurationRemoved(@Nonnull RunManagerListenerEvent event) {
        updateDashboardIfNeeded(event.getSettings());
      }

      @Override
      public void runConfigurationChanged(@Nonnull RunManagerListenerEvent event) {
        updateDashboardIfNeeded(event.getSettings());
      }
    });
    MessageBusConnection connection = myProject.getMessageBus().connect(myProject);
    connection.subscribe(ExecutionListener.class, new ExecutionListener() {
      @Override
      public void processStarted(@Nonnull String executorId, @Nonnull ExecutionEnvironment env, final @Nonnull ProcessHandler handler) {
        updateDashboardIfNeeded(env.getRunnerAndConfigurationSettings());
      }

      @Override
      public void processTerminated(@Nonnull String executorId, @Nonnull ExecutionEnvironment env, @Nonnull ProcessHandler handler, int exitCode) {
        updateDashboardIfNeeded(env.getRunnerAndConfigurationSettings());
      }
    });
    connection.subscribe(DashboardListener.class, new DashboardListener() {
      @Override
      public void contentChanged(boolean withStructure) {
        updateDashboard(withStructure);
      }
    });
    connection.subscribe(DumbModeListener.class, new DumbModeListener() {
      @Override
      public void enteredDumbMode() {
      }

      @Override
      public void exitDumbMode() {
        updateDashboard(false);
      }
    });
  }

  @Override
  public ContentManager getDashboardContentManager() {
    return myContentManager;
  }

  @Override
  public String getToolWindowId() {
    return ToolWindowId.RUN_DASHBOARD;
  }

  @Nonnull
  @Override
  public Image getToolWindowIcon() {
    return AllIcons.Toolwindows.ToolWindowRun; // TODO [konstantin.aleev] provide new icon
  }

  @Override
  public boolean isToolWindowAvailable() {
    return isDashboardEnabled() && hasContent();
  }

  @Override
  public void createToolWindowContent(@Nonnull ToolWindow toolWindow) {
    myDashboardContent = new RunDashboardContent(myProject, myContentManager, myGroupers);
    ContentManager contentManager = toolWindow.getContentManager();
    Content content = contentManager.getFactory().createContent(myDashboardContent, null, false);
    Disposer.register(content, myDashboardContent);
    Disposer.register(content, () -> myDashboardContent = null);
    toolWindow.getContentManager().addContent(content);
  }

  @Override
  public List<Pair<RunnerAndConfigurationSettings, RunContentDescriptor>> getRunConfigurations() {
    List<Pair<RunnerAndConfigurationSettings, RunContentDescriptor>> result = new ArrayList<>();

    List<RunnerAndConfigurationSettings> configurations =
            RunManager.getInstance(myProject).getAllSettings().stream().filter(runConfiguration -> RunDashboardContributor.isShowInDashboard(runConfiguration.getType())).collect(Collectors.toList());

    //noinspection ConstantConditions ???
    ExecutionManagerImpl executionManager = ExecutionManagerImpl.getInstance(myProject);
    configurations.forEach(configurationSettings -> {
      List<RunContentDescriptor> descriptors = executionManager.getDescriptors(settings -> Comparing.equal(settings.getConfiguration(), configurationSettings.getConfiguration()));
      if (descriptors.isEmpty()) {
        result.add(Pair.create(configurationSettings, null));
      }
      else {
        descriptors.forEach(descriptor -> result.add(Pair.create(configurationSettings, descriptor)));
      }
    });

    // It is possible that run configuration was deleted, but there is running content descriptor for such run configuration.
    // It should be shown in he dashboard tree.
    List<RunConfiguration> storedConfigurations = configurations.stream().map(RunnerAndConfigurationSettings::getConfiguration).collect(Collectors.toList());
    List<RunContentDescriptor> notStoredDescriptors = executionManager.getRunningDescriptors(settings -> {
      RunConfiguration configuration = settings.getConfiguration();
      return RunDashboardContributor.isShowInDashboard(settings.getType()) && !storedConfigurations.contains(configuration);
    });
    notStoredDescriptors.forEach(descriptor -> {
      Set<RunnerAndConfigurationSettings> settings = executionManager.getConfigurations(descriptor);
      settings.forEach(setting -> result.add(Pair.create(setting, descriptor)));
    });

    return result;
  }

  private void updateDashboardIfNeeded(@Nullable RunnerAndConfigurationSettings settings) {
    if (settings != null && RunDashboardContributor.isShowInDashboard(settings.getType())) {
      updateDashboard(true);
    }
  }

  private void updateDashboard(final boolean withStructure) {
    final ToolWindowManager toolWindowManager = ToolWindowManager.getInstance(myProject);
    toolWindowManager.invokeLater(() -> {
      if (myProject.isDisposed()) {
        return;
      }

      if (withStructure) {
        boolean available = hasContent();
        ToolWindow toolWindow = toolWindowManager.getToolWindow(getToolWindowId());
        if (toolWindow == null) {
          if (available) {
            createToolWindow();
          }
          return;
        }

        boolean doShow = !toolWindow.isAvailable() && available;
        toolWindow.setAvailable(available, null);
        if (doShow) {
          toolWindow.show(null);
        }
      }

      if (myDashboardContent != null) {
        myDashboardContent.updateContent(withStructure);
      }
    });
  }

  private void createToolWindow() {
    ToolWindowManager toolWindowManager = ToolWindowManager.getInstance(myProject);
    ToolWindow toolWindow = toolWindowManager.registerToolWindow(getToolWindowId(), false, ToolWindowAnchor.BOTTOM, myProject, true);
    toolWindow.setIcon(getToolWindowIcon());
    createToolWindowContent(toolWindow);
  }

  private boolean hasContent() {
    return !getRunConfigurations().isEmpty();
  }

  @Nullable
  @Override
  public State getState() {
    State state = new State();
    state.ruleStates =
            myGroupers.stream().filter(grouper -> !grouper.getRule().isAlwaysEnabled()).map(grouper -> new RuleState(grouper.getRule().getName(), grouper.isEnabled())).collect(Collectors.toList());
    return state;
  }

  @Override
  public void loadState(State state) {
    state.ruleStates.forEach(ruleState -> {
      for (DashboardGrouper grouper : myGroupers) {
        if (grouper.getRule().getName().equals(ruleState.name) && !grouper.getRule().isAlwaysEnabled()) {
          grouper.setEnabled(ruleState.enabled);
          return;
        }
      }
    });
  }

  static class State {
    public List<RuleState> ruleStates = new ArrayList<>();
  }

  private static class RuleState {
    public String name;
    public boolean enabled = true;

    @SuppressWarnings("UnusedDeclaration")
    public RuleState() {
    }

    public RuleState(String name, boolean enabled) {
      this.name = name;
      this.enabled = enabled;
    }
  }
}
