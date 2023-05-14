package consulo.execution;

import consulo.application.AllIcons;
import consulo.application.ApplicationManager;
import consulo.application.dumb.DumbAware;
import consulo.disposer.Disposable;
import consulo.disposer.Disposer;
import consulo.document.FileDocumentManager;
import consulo.execution.action.CloseAction;
import consulo.execution.executor.DefaultRunExecutor;
import consulo.execution.executor.Executor;
import consulo.execution.ui.RunContentDescriptor;
import consulo.execution.ui.console.ConsoleView;
import consulo.execution.ui.console.Filter;
import consulo.execution.ui.console.TextConsoleBuilder;
import consulo.execution.ui.console.TextConsoleBuilderFactory;
import consulo.platform.base.icon.PlatformIconGroup;
import consulo.process.ProcessHandler;
import consulo.process.event.ProcessAdapter;
import consulo.process.event.ProcessEvent;
import consulo.project.Project;
import consulo.project.ui.wm.ToolWindowId;
import consulo.project.ui.wm.ToolWindowManager;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.action.*;
import consulo.ui.image.Image;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.function.BooleanSupplier;

/**
 * Runs a process and prints the output in a content tab within the Run toolwindow.
 *
 * @author yole
 */
public class RunContentExecutor implements Disposable {
  private final Project myProject;
  private final ProcessHandler myProcess;
  private final List<Filter> myFilterList = new ArrayList<>();
  private Runnable myRerunAction;
  private Runnable myStopAction;
  private Runnable myAfterCompletion;
  private BooleanSupplier myStopEnabled;
  private String myTitle = "Output";
  private String myHelpId = null;
  private boolean myActivateToolWindow = true;
  private Image myIcon = PlatformIconGroup.generalRun();

  /**
   * User-provided console that has to be used instead of newly created
   */
  private ConsoleView myUserProvidedConsole;

  public RunContentExecutor(@Nonnull Project project, @Nonnull ProcessHandler process) {
    myProject = project;
    myProcess = process;
  }

  public RunContentExecutor withFilter(Filter filter) {
    myFilterList.add(filter);
    return this;
  }

  public RunContentExecutor withTitle(String title) {
    myTitle = title;
    return this;
  }

  public RunContentExecutor withIcon(@Nonnull Image icon) {
    myIcon = icon;
    return this;
  }

  public RunContentExecutor withRerun(Runnable rerun) {
    myRerunAction = rerun;
    return this;
  }

  public RunContentExecutor withStop(@Nonnull Runnable stop, @Nonnull BooleanSupplier stopEnabled) {
    myStopAction = stop;
    myStopEnabled = stopEnabled;
    return this;
  }

  public RunContentExecutor withAfterCompletion(Runnable afterCompletion) {
    myAfterCompletion = afterCompletion;
    return this;
  }

  public RunContentExecutor withHelpId(String helpId) {
    myHelpId = helpId;
    return this;
  }

  public RunContentExecutor withActivateToolWindow(boolean activateToolWindow) {
    myActivateToolWindow = activateToolWindow;
    return this;
  }

  private ConsoleView createConsole(@Nonnull Project project) {
    TextConsoleBuilder consoleBuilder = TextConsoleBuilderFactory.getInstance().createBuilder(project);
    consoleBuilder.filters(myFilterList);
    final ConsoleView console = consoleBuilder.getConsole();

    if (myHelpId != null) {
      console.setHelpId(myHelpId);
    }
    Executor executor = DefaultRunExecutor.getRunExecutorInstance();
    DefaultActionGroup actions = new DefaultActionGroup();

    final JComponent consolePanel = createConsolePanel(console, actions);
    RunContentDescriptor descriptor = new RunContentDescriptor(console, myProcess, consolePanel, myTitle, myIcon);

    Disposer.register(descriptor, this);
    Disposer.register(descriptor, console);

    actions.add(new RerunAction(consolePanel));
    actions.add(new StopAction());
    actions.add(new CloseAction(executor, descriptor, myProject));

    ExecutionManager.getInstance(myProject).getContentManager().showRunContent(executor, descriptor);

    if (myActivateToolWindow) {
      activateToolWindow();
    }

    return console;
  }

  public void run() {
    FileDocumentManager.getInstance().saveAllDocuments();

    // Use user-provided console if exist. Create new otherwise
    ConsoleView view = (myUserProvidedConsole != null ? myUserProvidedConsole : createConsole(myProject));
    view.attachToProcess(myProcess);
    if (myAfterCompletion != null) {
      myProcess.addProcessListener(new ProcessAdapter() {
        @Override
        public void processTerminated(ProcessEvent event) {
          SwingUtilities.invokeLater(myAfterCompletion);
        }
      });
    }
    myProcess.startNotify();
  }

  public void activateToolWindow() {
    ApplicationManager.getApplication().invokeLater(() -> ToolWindowManager.getInstance(myProject).getToolWindow(ToolWindowId.RUN).activate(null));
  }

  private static JComponent createConsolePanel(ConsoleView view, ActionGroup actions) {
    JPanel panel = new JPanel(new BorderLayout());

    panel.add(view.getComponent(), BorderLayout.CENTER);
    panel.add(createToolbar(actions, panel), BorderLayout.WEST);
    return panel;
  }

  private static JComponent createToolbar(ActionGroup actions, JPanel panel) {
    ActionToolbar actionToolbar = ActionManager.getInstance().createActionToolbar(ActionPlaces.UNKNOWN, actions, false);
    actionToolbar.setTargetComponent(panel);
    return actionToolbar.getComponent();
  }

  @Override
  public void dispose() {
  }

  /**
   * @param console console to use instead of new one. Pass null to always create new
   */
  @Nonnull
  public RunContentExecutor withConsole(@Nullable ConsoleView console) {
    myUserProvidedConsole = console;
    return this;
  }

  private class RerunAction extends AnAction {
    public RerunAction(JComponent consolePanel) {
      super("Rerun", "Rerun", AllIcons.Actions.Restart);
      registerCustomShortcutSet(CommonShortcuts.getRerun(), consolePanel);
    }

    @RequiredUIAccess
    @Override
    public void actionPerformed(AnActionEvent e) {
      myRerunAction.run();
    }

    @RequiredUIAccess
    @Override
    public void update(AnActionEvent e) {
      e.getPresentation().setVisible(myRerunAction != null);
      e.getPresentation().setEnabled(myRerunAction != null);
    }

    @Override
    public boolean isDumbAware() {
      return true;
    }
  }

  private class StopAction extends AnAction implements DumbAware {
    public StopAction() {
      super("Stop", "Stop", AllIcons.Actions.Suspend);
    }

    @RequiredUIAccess
    @Override
    public void actionPerformed(AnActionEvent e) {
      myStopAction.run();
    }

    @RequiredUIAccess
    @Override
    public void update(AnActionEvent e) {
      e.getPresentation().setVisible(myStopAction != null);
      e.getPresentation().setEnabled(myStopEnabled != null && myStopEnabled.getAsBoolean());
    }
  }
}
