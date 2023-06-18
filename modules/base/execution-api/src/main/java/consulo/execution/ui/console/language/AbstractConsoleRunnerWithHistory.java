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
package consulo.execution.ui.console.language;

import consulo.execution.ExecutionManager;
import consulo.execution.action.CloseAction;
import consulo.execution.executor.DefaultRunExecutor;
import consulo.execution.executor.Executor;
import consulo.execution.util.ConsoleTitleGen;
import consulo.execution.ui.console.ConsoleExecuteAction;
import consulo.execution.process.ProcessTerminatedListener;
import consulo.execution.ui.RunContentDescriptor;
import consulo.process.ExecutionException;
import consulo.process.ProcessHandler;
import consulo.process.event.ProcessEvent;
import consulo.process.event.ProcessListener;
import consulo.project.Project;
import consulo.ui.ex.JBColor;
import consulo.ui.ex.action.*;
import consulo.ui.ex.awt.SideBorder;
import consulo.ui.ex.awt.UIUtil;
import consulo.ui.image.Image;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

/**
 * This class provides basic functionality for running consoles.
 * It launches external process and handles line input with history
 *
 * @author oleg
 */
public abstract class AbstractConsoleRunnerWithHistory<T extends LanguageConsoleView> {
  private final String myConsoleTitle;

  private ProcessHandler myProcessHandler;
  private final String myWorkingDir;

  private T myConsoleView;

  @Nonnull
  private final Project myProject;

  private ProcessBackedConsoleExecuteActionHandler myConsoleExecuteActionHandler;

  public AbstractConsoleRunnerWithHistory(@Nonnull Project project, @Nonnull String consoleTitle, @Nullable String workingDir) {
    myProject = project;
    myConsoleTitle = consoleTitle;
    myWorkingDir = workingDir;
  }

  /**
   * Launch process, setup history, actions etc.
   *
   * @throws ExecutionException
   */
  public void initAndRun() throws ExecutionException {
    // Create Server process
    myProcessHandler = createProcessHandler();
    UIUtil.invokeLaterIfNeeded(() -> {
      // Init console view
      myConsoleView = createConsoleView();
      if (myConsoleView instanceof JComponent) {
        ((JComponent)myConsoleView).setBorder(new SideBorder(JBColor.border(), SideBorder.LEFT));
      }
      myConsoleExecuteActionHandler = createExecuteActionHandler();

      ProcessTerminatedListener.attach(myProcessHandler);

      myProcessHandler.addProcessListener(new ProcessListener() {
        @Override
        public void processTerminated(ProcessEvent event) {
          finishConsole();
        }
      });

      // Attach to process
      myConsoleView.attachToProcess(myProcessHandler);

      // Runner creating
      createContentDescriptorAndActions();

      // Run
      myProcessHandler.startNotify();
    });
  }

  protected Executor getExecutor() {
    return DefaultRunExecutor.getRunExecutorInstance();
  }

  protected void createContentDescriptorAndActions() {
    final Executor defaultExecutor = getExecutor();
    final DefaultActionGroup toolbarActions = new DefaultActionGroup();
    final ActionToolbar actionToolbar = ActionManager.getInstance().createActionToolbar(ActionPlaces.UNKNOWN, toolbarActions, false);

    // Runner creating
    final JPanel panel = new JPanel(new BorderLayout());
    panel.add(actionToolbar.getComponent(), BorderLayout.WEST);
    panel.add(myConsoleView.getComponent(), BorderLayout.CENTER);

    actionToolbar.setTargetComponent(panel);

    final RunContentDescriptor contentDescriptor = new RunContentDescriptor(myConsoleView, myProcessHandler, panel, constructConsoleTitle(myConsoleTitle), getConsoleIcon());

    contentDescriptor.setFocusComputable(() -> getConsoleView().getConsoleEditor().getContentComponent());
    contentDescriptor.setAutoFocusContent(isAutoFocusContent());


    // tool bar actions
    final List<AnAction> actions = fillToolBarActions(toolbarActions, defaultExecutor, contentDescriptor);
    registerActionShortcuts(actions, getConsoleView().getConsoleEditor().getComponent());
    registerActionShortcuts(actions, panel);

    showConsole(defaultExecutor, contentDescriptor);
  }

  @Nullable
  protected Image getConsoleIcon() {
    return null;
  }

  protected String constructConsoleTitle(final @Nonnull String consoleTitle) {
    return new ConsoleTitleGen(myProject, consoleTitle, shouldAddNumberToTitle()).makeTitle();
  }

  public boolean isAutoFocusContent() {
    return true;
  }

  protected boolean shouldAddNumberToTitle() {
    return false;
  }

  protected void showConsole(Executor defaultExecutor, @Nonnull RunContentDescriptor contentDescriptor) {
    // Show in run toolwindow
    ExecutionManager.getInstance(myProject).getContentManager().showRunContent(defaultExecutor, contentDescriptor);
  }

  protected void finishConsole() {
    myConsoleView.setEditable(false);
  }

  protected abstract T createConsoleView();

  @Nonnull
  protected abstract ProcessHandler createProcessHandler() throws ExecutionException;

  public static void registerActionShortcuts(final List<AnAction> actions, final JComponent component) {
    for (AnAction action : actions) {
      if (action.getShortcutSet() != null) {
        action.registerCustomShortcutSet(action.getShortcutSet(), component);
      }
    }
  }

  protected List<AnAction> fillToolBarActions(final DefaultActionGroup toolbarActions, final Executor defaultExecutor, final RunContentDescriptor contentDescriptor) {

    List<AnAction> actionList = new ArrayList<>();

    //stop
    actionList.add(createStopAction());

    //close
    actionList.add(createCloseAction(defaultExecutor, contentDescriptor));

    // run action
    actionList.add(createConsoleExecAction(myConsoleExecuteActionHandler));

    // Help
    actionList.add(CommonActionsManager.getInstance().createHelpAction("interactive_console"));

    toolbarActions.addAll(actionList);

    return actionList;
  }

  protected AnAction createCloseAction(final Executor defaultExecutor, final RunContentDescriptor myDescriptor) {
    return new CloseAction(defaultExecutor, myDescriptor, myProject);
  }

  protected AnAction createStopAction() {
    return ActionManager.getInstance().getAction(IdeActions.ACTION_STOP_PROGRAM);
  }

  protected AnAction createConsoleExecAction(@Nonnull ProcessBackedConsoleExecuteActionHandler consoleExecuteActionHandler) {
    String emptyAction = consoleExecuteActionHandler.getEmptyExecuteAction();
    return new ConsoleExecuteAction(myConsoleView, consoleExecuteActionHandler, emptyAction, consoleExecuteActionHandler);
  }

  @Nonnull
  protected abstract ProcessBackedConsoleExecuteActionHandler createExecuteActionHandler();

  public T getConsoleView() {
    return myConsoleView;
  }

  @Nonnull
  public Project getProject() {
    return myProject;
  }

  public String getConsoleTitle() {
    return myConsoleTitle;
  }

  public String getWorkingDir() {
    return myWorkingDir;
  }

  public ProcessHandler getProcessHandler() {
    return myProcessHandler;
  }

  public ProcessBackedConsoleExecuteActionHandler getConsoleExecuteActionHandler() {
    return myConsoleExecuteActionHandler;
  }


}
