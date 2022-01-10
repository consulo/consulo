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
package com.intellij.execution.runners;

import com.intellij.execution.ExecutionManager;
import com.intellij.execution.ExecutionResult;
import com.intellij.execution.Executor;
import com.intellij.execution.configurations.RunConfigurationBase;
import com.intellij.execution.configurations.RunProfile;
import com.intellij.execution.configurations.SearchScopeProvider;
import com.intellij.execution.impl.ConsoleViewImpl;
import com.intellij.execution.ui.*;
import com.intellij.execution.ui.layout.PlaceInGrid;
import com.intellij.icons.AllIcons;
import com.intellij.openapi.actionSystem.*;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.ui.content.Content;
import com.intellij.ui.content.tabs.PinToolwindowTabAction;
import com.intellij.util.SmartList;
import consulo.disposer.Disposer;
import org.jetbrains.annotations.NonNls;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Collection;
import java.util.List;

public class RunContentBuilder extends RunTab {
  @NonNls private static final String JAVA_RUNNER = "JavaRunner";

  private final List<AnAction> myRunnerActions = new SmartList<AnAction>();
  private final ExecutionResult myExecutionResult;

  /**
   * @deprecated use {@link #RunContentBuilder(com.intellij.execution.ExecutionResult, ExecutionEnvironment)}
   * to remove in IDEA 14
   */
  @SuppressWarnings("UnusedParameters")
  public RunContentBuilder(@Nonnull Project project,
                           ProgramRunner runner,
                           Executor executor,
                           ExecutionResult executionResult,
                           @Nonnull ExecutionEnvironment environment) {
    //noinspection deprecation
    this(runner, executionResult, environment);
  }

  /**
   * @deprecated use {@link #RunContentBuilder(com.intellij.execution.ExecutionResult, ExecutionEnvironment)}
   * to remove in IDEA 15
   */
  public RunContentBuilder(ProgramRunner runner,
                           ExecutionResult executionResult,
                           @Nonnull ExecutionEnvironment environment) {
    this(executionResult, fix(environment, runner));
  }

  public RunContentBuilder(@Nonnull ExecutionResult executionResult, @Nonnull ExecutionEnvironment environment) {
    super(environment, getRunnerType(executionResult.getExecutionConsole()));

    myExecutionResult = executionResult;
    myUi.getOptions().setMoveToGridActionEnabled(false).setMinimizeActionEnabled(false);
  }

  @Nonnull
  public static ExecutionEnvironment fix(@Nonnull ExecutionEnvironment environment, @Nullable ProgramRunner runner) {
    if (runner == null || runner.equals(environment.getRunner())) {
      return environment;
    }
    else {
      return new ExecutionEnvironmentBuilder(environment).runner(runner).build();
    }
  }

  @SuppressWarnings("UnusedDeclaration")
  @Deprecated
  @Nonnull
  /**
   * @deprecated to remove in IDEA 15
   */
  public static GlobalSearchScope createSearchScope(Project project, RunProfile runProfile) {
    return SearchScopeProvider.createSearchScope(project, runProfile);
  }

  @Nonnull
  public ExecutionResult getExecutionResult() {
    return myExecutionResult;
  }

  public void addAction(@Nonnull final AnAction action) {
    myRunnerActions.add(action);
  }

  @Nonnull
  private RunContentDescriptor createDescriptor() {
    final RunProfile profile = myEnvironment.getRunProfile();
    if (ApplicationManager.getApplication().isUnitTestMode()) {
      return new RunContentDescriptor(profile, myExecutionResult, myUi);
    }

    final ExecutionConsole console = myExecutionResult.getExecutionConsole();
    RunContentDescriptor contentDescriptor = new RunContentDescriptor(profile, myExecutionResult, myUi);
    if (console != null) {
      if (console instanceof ExecutionConsoleEx) {
        ((ExecutionConsoleEx)console).buildUi(myUi);
      }
      else {
        buildConsoleUiDefault(myUi, console);
      }
      initLogConsoles(profile, contentDescriptor, console);
    }
    myUi.getOptions().setLeftToolbar(createActionToolbar(contentDescriptor), ActionPlaces.UNKNOWN);

    if (profile instanceof RunConfigurationBase) {
      if (console instanceof ObservableConsoleView && !ApplicationManager.getApplication().isUnitTestMode()) {
        ((ObservableConsoleView)console).addChangeListener(new ConsoleToFrontListener((RunConfigurationBase)profile,
                                                                                      myProject,
                                                                                      myEnvironment.getExecutor(),
                                                                                      contentDescriptor,
                                                                                      myUi),
                                                           this);
      }
    }

    return contentDescriptor;
  }

  @Nonnull
  private static String getRunnerType(@Nullable ExecutionConsole console) {
    String runnerType = JAVA_RUNNER;
    if (console instanceof ExecutionConsoleEx) {
      String id = ((ExecutionConsoleEx)console).getExecutionConsoleId();
      if (id != null) {
        return JAVA_RUNNER + '.' + id;
      }
    }
    return runnerType;
  }

  public static void buildConsoleUiDefault(RunnerLayoutUi ui, final ExecutionConsole console) {
    final Content consoleContent = ui.createContent(ExecutionConsole.CONSOLE_CONTENT_ID, console.getComponent(), "Console",
                                                    AllIcons.Debugger.Console,
                                                    console.getPreferredFocusableComponent());

    consoleContent.setCloseable(false);
    addAdditionalConsoleEditorActions(console, consoleContent);
    ui.addContent(consoleContent, 0, PlaceInGrid.bottom, false);
  }

  public static void addAdditionalConsoleEditorActions(final ExecutionConsole console, final Content consoleContent) {
    final DefaultActionGroup consoleActions = new DefaultActionGroup();
    if (console instanceof ConsoleView) {
      for (AnAction action : ((ConsoleView)console).createConsoleActions()) {
        consoleActions.add(action);
      }
    }

    consoleContent.setActions(consoleActions, ActionPlaces.UNKNOWN, console.getComponent());
  }

  @Nonnull
  private ActionGroup createActionToolbar(@Nonnull RunContentDescriptor contentDescriptor) {
    final DefaultActionGroup actionGroup = new DefaultActionGroup();
    actionGroup.add(ActionManager.getInstance().getAction(IdeActions.ACTION_RERUN));
    final AnAction[] actions = contentDescriptor.getRestartActions();
    actionGroup.addAll(actions);
    if (actions.length > 0) {
      actionGroup.addSeparator();
    }

    actionGroup.add(ActionManager.getInstance().getAction(IdeActions.ACTION_STOP_PROGRAM));

    actionGroup.addAll(myExecutionResult.getActions());

    for (AnAction anAction : myRunnerActions) {
      if (anAction != null) {
        actionGroup.add(anAction);
      }
      else {
        actionGroup.addSeparator();
      }
    }

    actionGroup.addSeparator();
    actionGroup.add(myUi.getOptions().getLayoutActions());
    actionGroup.addSeparator();
    actionGroup.add(PinToolwindowTabAction.getPinAction());
    return actionGroup;
  }

  /**
   * @param reuseContent see {@link RunContentDescriptor#myContent}
   */
  public RunContentDescriptor showRunContent(@Nullable RunContentDescriptor reuseContent) {
    RunContentDescriptor descriptor = createDescriptor();
    Disposer.register(descriptor, this);
    Disposer.register(myProject, descriptor);
    RunContentManagerImpl.copyContentAndBehavior(descriptor, reuseContent);
    myRunContentDescriptor = descriptor;
    return descriptor;
  }

  public static class ConsoleToFrontListener implements ConsoleViewImpl.ChangeListener {
    @Nonnull
    private final RunConfigurationBase myRunConfigurationBase;
    @Nonnull
    private final Project myProject;
    @Nonnull
    private final Executor myExecutor;
    @Nonnull
    private final RunContentDescriptor myRunContentDescriptor;
    @Nonnull
    private final RunnerLayoutUi myUi;

    public ConsoleToFrontListener(@Nonnull RunConfigurationBase runConfigurationBase,
                                  @Nonnull Project project,
                                  @Nonnull Executor executor,
                                  @Nonnull RunContentDescriptor runContentDescriptor,
                                  @Nonnull RunnerLayoutUi ui) {
      myRunConfigurationBase = runConfigurationBase;
      myProject = project;
      myExecutor = executor;
      myRunContentDescriptor = runContentDescriptor;
      myUi = ui;
    }

    @Override
    public void contentAdded(Collection<ConsoleViewContentType> types) {
      if (myProject.isDisposed() || myUi.isDisposed())
        return;
      for (ConsoleViewContentType type : types) {
        if ((type == ConsoleViewContentType.NORMAL_OUTPUT) && myRunConfigurationBase.isShowConsoleOnStdOut()
            || (type == ConsoleViewContentType.ERROR_OUTPUT) && myRunConfigurationBase.isShowConsoleOnStdErr()) {
          ExecutionManager.getInstance(myProject).getContentManager().toFrontRunContent(myExecutor, myRunContentDescriptor);
          myUi.selectAndFocus(myUi.findContent(ExecutionConsole.CONSOLE_CONTENT_ID), false, false);
          return;
        }
      }
    }
  }
}
