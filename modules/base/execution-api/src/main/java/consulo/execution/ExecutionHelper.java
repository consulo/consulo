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

package consulo.execution;

import consulo.application.ApplicationManager;
import consulo.application.progress.ProgressIndicator;
import consulo.application.progress.ProgressManager;
import consulo.application.progress.Task;
import consulo.application.util.Semaphore;
import consulo.dataContext.DataContext;
import consulo.disposer.Disposer;
import consulo.execution.executor.DefaultRunExecutor;
import consulo.execution.process.ExecutionMode;
import consulo.execution.ui.RunContentDescriptor;
import consulo.execution.ui.RunContentManager;
import consulo.logging.Logger;
import consulo.navigation.Navigatable;
import consulo.process.ProcessHandler;
import consulo.process.cmd.GeneralCommandLine;
import consulo.process.local.ProcessOutput;
import consulo.project.Project;
import consulo.project.ui.view.MessageView;
import consulo.project.ui.wm.ToolWindowId;
import consulo.project.ui.wm.ToolWindowManager;
import consulo.ui.ex.MessageCategory;
import consulo.ui.ex.awt.ColoredListCellRenderer;
import consulo.ui.ex.awt.Messages;
import consulo.ui.ex.content.Content;
import consulo.ui.ex.content.ContentFactory;
import consulo.ui.ex.errorTreeView.ErrorTreeView;
import consulo.ui.ex.errorTreeView.NewErrorTreeViewPanel;
import consulo.ui.ex.errorTreeView.NewErrorTreeViewPanelFactory;
import consulo.ui.ex.popup.IPopupChooserBuilder;
import consulo.ui.ex.popup.JBPopupFactory;
import consulo.ui.ex.toolWindow.ToolWindow;
import consulo.ui.image.Image;
import consulo.undoRedo.CommandProcessor;
import consulo.util.collection.ArrayUtil;
import consulo.util.collection.ContainerUtil;
import consulo.util.lang.StringUtil;
import consulo.virtualFileSystem.VirtualFile;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.swing.*;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * Created by IntelliJ IDEA.
 *
 * @author: Roman Chernyatchik
 * @date: Oct 4, 2007
 */
public class ExecutionHelper {
  private static final Logger LOG = Logger.getInstance(ExecutionHelper.class);

  private ExecutionHelper() {
  }

  public static void showErrors(@Nonnull final Project myProject, @Nonnull final List<? extends Exception> errors, @Nonnull final String tabDisplayName, @Nullable final VirtualFile file) {
    showExceptions(myProject, errors, Collections.<Exception>emptyList(), tabDisplayName, file);
  }

  public static void showExceptions(@Nonnull final Project myProject,
                                    @Nonnull final List<? extends Exception> errors,
                                    @Nonnull final List<? extends Exception> warnings,
                                    @Nonnull final String tabDisplayName,
                                    @Nullable final VirtualFile file) {
    if (ApplicationManager.getApplication().isUnitTestMode() && !errors.isEmpty()) {
      throw new RuntimeException(errors.get(0));
    }
    ApplicationManager.getApplication().invokeLater(new Runnable() {
      @Override
      public void run() {
        if (myProject.isDisposed()) return;
        if (errors.isEmpty() && warnings.isEmpty()) {
          removeContents(null, myProject, tabDisplayName);
          return;
        }

        final NewErrorTreeViewPanel errorTreeView = myProject.getApplication().getInstance(NewErrorTreeViewPanelFactory.class).createPanel(myProject, "reference.toolWindows.messages");
        errorTreeView.setCanHideWarningsOrInfos(false);
        try {
          openMessagesView(errorTreeView, myProject, tabDisplayName);
        }
        catch (NullPointerException e) {
          final StringBuilder builder = new StringBuilder();
          builder.append("Exceptions occurred:");
          for (final Exception exception : errors) {
            builder.append("\n");
            builder.append(exception.getMessage());
          }
          builder.append("Warnings occurred:");
          for (final Exception exception : warnings) {
            builder.append("\n");
            builder.append(exception.getMessage());
          }
          Messages.showErrorDialog(builder.toString(), "Execution Error");
          return;
        }

        addMessages(MessageCategory.ERROR, errors, errorTreeView, file, "Unknown Error");
        addMessages(MessageCategory.WARNING, warnings, errorTreeView, file, "Unknown Warning");

        ToolWindowManager.getInstance(myProject).getToolWindow(ToolWindowId.MESSAGES_WINDOW).activate(null);
      }
    });
  }

  private static void addMessages(final int messageCategory,
                                  @Nonnull final List<? extends Exception> exceptions,
                                  @Nonnull NewErrorTreeViewPanel errorTreeView,
                                  @Nullable final VirtualFile file,
                                  @Nonnull final String defaultMessage) {
    for (final Exception exception : exceptions) {
      String[] messages = StringUtil.splitByLines(exception.getMessage());
      if (messages.length == 0) {
        messages = new String[]{defaultMessage};
      }
      errorTreeView.addMessage(messageCategory, messages, file, -1, -1, null);
    }
  }

  public static void showOutput(@Nonnull final Project myProject,
                                @Nonnull final ProcessOutput output,
                                @Nonnull final String tabDisplayName,
                                @Nullable final VirtualFile file,
                                final boolean activateWindow) {
    final String stdout = output.getStdout();
    final String stderr = output.getStderr();
    if (ApplicationManager.getApplication().isUnitTestMode() && !(stdout.isEmpty() || stderr.isEmpty())) {
      throw new RuntimeException("STDOUT:\n" + stdout + "\nSTDERR:\n" + stderr);
    }

    ApplicationManager.getApplication().invokeLater(new Runnable() {
      @Override
      public void run() {
        if (myProject.isDisposed()) return;

        final String stdOutTitle = "[Stdout]:";
        final String stderrTitle = "[Stderr]:";
        final NewErrorTreeViewPanel errorTreeView = myProject.getApplication().getInstance(NewErrorTreeViewPanelFactory.class).createPanel(myProject, "reference.toolWindows.messages");
        errorTreeView.setCanHideWarningsOrInfos(false);
        try {
          openMessagesView(errorTreeView, myProject, tabDisplayName);
        }
        catch (NullPointerException e) {
          final StringBuilder builder = new StringBuilder();
          builder.append(stdOutTitle).append("\n").append(stdout != null ? stdout : "<empty>").append("\n");
          builder.append(stderrTitle).append("\n").append(stderr != null ? stderr : "<empty>");
          Messages.showErrorDialog(builder.toString(), "Process Output");
          return;
        }

        if (!StringUtil.isEmpty(stdout)) {
          final String[] stdoutLines = StringUtil.splitByLines(stdout);
          if (stdoutLines.length > 0) {
            if (StringUtil.isEmpty(stderr)) {
              // Only stdout available
              errorTreeView.addMessage(MessageCategory.SIMPLE, stdoutLines, file, -1, -1, null);
            }
            else {
              // both stdout and stderr available, show as groups
              if (file == null) {
                errorTreeView.addMessage(MessageCategory.SIMPLE, stdoutLines, stdOutTitle, new FakeNavigatable(), null, null, null);
              }
              else {
                errorTreeView.addMessage(MessageCategory.SIMPLE, new String[]{stdOutTitle}, file, -1, -1, null);
                errorTreeView.addMessage(MessageCategory.SIMPLE, new String[]{""}, file, -1, -1, null);
                errorTreeView.addMessage(MessageCategory.SIMPLE, stdoutLines, file, -1, -1, null);
              }
            }
          }
        }
        if (!StringUtil.isEmpty(stderr)) {
          final String[] stderrLines = StringUtil.splitByLines(stderr);
          if (stderrLines.length > 0) {
            if (file == null) {
              errorTreeView.addMessage(MessageCategory.SIMPLE, stderrLines, stderrTitle, new FakeNavigatable(), null, null, null);
            }
            else {
              errorTreeView.addMessage(MessageCategory.SIMPLE, new String[]{stderrTitle}, file, -1, -1, null);
              errorTreeView.addMessage(MessageCategory.SIMPLE, ArrayUtil.EMPTY_STRING_ARRAY, file, -1, -1, null);
              errorTreeView.addMessage(MessageCategory.SIMPLE, stderrLines, file, -1, -1, null);
            }
          }
        }
        errorTreeView.addMessage(MessageCategory.SIMPLE, new String[]{"Process finished with exit code " + output.getExitCode()}, null, -1, -1, null);

        if (activateWindow) {
          ToolWindowManager.getInstance(myProject).getToolWindow(ToolWindowId.MESSAGES_WINDOW).activate(null);
        }
      }
    });
  }

  private static void openMessagesView(@Nonnull final NewErrorTreeViewPanel errorTreeView, @Nonnull final Project project, @Nonnull final String tabDisplayName) {
    CommandProcessor commandProcessor = CommandProcessor.getInstance();
    commandProcessor.executeCommand(project, new Runnable() {
      @Override
      public void run() {
        final MessageView messageView = MessageView.getInstance(project);
        final Content content = ContentFactory.getInstance().createContent(errorTreeView.getComponent(), tabDisplayName, true);
        messageView.getContentManager().addContent(content);
        Disposer.register(content, errorTreeView);
        messageView.getContentManager().setSelectedContent(content);
        removeContents(content, project, tabDisplayName);
      }
    }, "Open message view", null);
  }

  private static void removeContents(@Nullable final Content notToRemove, @Nonnull final Project project, @Nonnull final String tabDisplayName) {
    MessageView messageView = MessageView.getInstance(project);
    Content[] contents = messageView.getContentManager().getContents();
    for (Content content : contents) {
      LOG.assertTrue(content != null);
      if (content.isPinned()) continue;
      if (tabDisplayName.equals(content.getDisplayName()) && content != notToRemove) {
        ErrorTreeView listErrorView = (ErrorTreeView)content.getComponent();
        if (listErrorView != null) {
          if (messageView.getContentManager().removeContent(content, true)) {
            content.release();
          }
        }
      }
    }
  }

  public static Collection<RunContentDescriptor> findRunningConsoleByTitle(final Project project, @Nonnull final Function<String, Boolean> titleMatcher) {
    return findRunningConsole(project, selectedContent -> titleMatcher.apply(selectedContent.getDisplayName()));
  }

  public static Collection<RunContentDescriptor> findRunningConsole(final Project project, @Nonnull final Function<RunContentDescriptor, Boolean> descriptorMatcher) {
    final ExecutionManager executionManager = ExecutionManager.getInstance(project);

    final RunContentDescriptor selectedContent = executionManager.getContentManager().getSelectedContent();
    if (selectedContent != null) {
      final ToolWindow toolWindow = ExecutionManager.getInstance(project).getContentManager().getToolWindowByDescriptor(selectedContent);
      if (toolWindow != null && toolWindow.isVisible()) {
        if (descriptorMatcher.apply(selectedContent)) {
          return Collections.singletonList(selectedContent);
        }
      }
    }

    final ArrayList<RunContentDescriptor> result = ContainerUtil.newArrayList();
    for (RunContentDescriptor runContentDescriptor : executionManager.getContentManager().getAllDescriptors()) {
      if (descriptorMatcher.apply(runContentDescriptor)) {
        result.add(runContentDescriptor);
      }
    }
    return result;
  }

  public static List<RunContentDescriptor> collectConsolesByDisplayName(final Project project, @Nonnull Function<String, Boolean> titleMatcher) {
    List<RunContentDescriptor> result = new ArrayList<>();
    final ExecutionManager executionManager = ExecutionManager.getInstance(project);
    for (RunContentDescriptor runContentDescriptor : executionManager.getContentManager().getAllDescriptors()) {
      if (titleMatcher.apply(runContentDescriptor.getDisplayName())) {
        result.add(runContentDescriptor);
      }
    }
    return result;
  }

  public static void selectContentDescriptor(final @Nonnull DataContext dataContext,
                                             final @Nonnull Project project,
                                             @Nonnull Collection<RunContentDescriptor> consoles,
                                             String selectDialogTitle,
                                             final Consumer<RunContentDescriptor> descriptorConsumer) {
    if (consoles.size() == 1) {
      RunContentDescriptor descriptor = consoles.iterator().next();
      descriptorConsumer.accept(descriptor);
      descriptorToFront(project, descriptor);
    }
    else if (consoles.size() > 1) {
      final Image icon = DefaultRunExecutor.getRunExecutorInstance().getIcon();

      IPopupChooserBuilder<RunContentDescriptor> builder = JBPopupFactory.getInstance().createPopupChooserBuilder(new ArrayList<>(consoles));
      builder = builder.setTitle(selectDialogTitle);
      builder = builder.setRenderer(new ColoredListCellRenderer<RunContentDescriptor>() {
        @Override
        protected void customizeCellRenderer(@Nonnull JList list, RunContentDescriptor value, int index, boolean selected, boolean hasFocus) {
          append(value.getDisplayName());
          setIcon(icon);
        }
      });
      builder = builder.setItemSelectedCallback(descriptor -> {
        descriptorConsumer.accept(descriptor);
        descriptorToFront(project, descriptor);
      });

      builder.createPopup().showInBestPositionFor(dataContext);
    }
  }

  private static void descriptorToFront(final Project project, final RunContentDescriptor descriptor) {
    ApplicationManager.getApplication().invokeLater(() -> {
      RunContentManager manager = ExecutionManager.getInstance(project).getContentManager();
      ToolWindow toolWindow = manager.getToolWindowByDescriptor(descriptor);
      if (toolWindow != null) {
        toolWindow.show(null);
        manager.selectRunContent(descriptor);
      }
    }, project.getDisposed());
  }

  public static void executeExternalProcess(@Nullable final Project myProject,
                                            @Nonnull final ProcessHandler processHandler,
                                            @Nonnull final ExecutionMode mode,
                                            @Nonnull final GeneralCommandLine cmdline) {
    executeExternalProcess(myProject, processHandler, mode, cmdline.getCommandLineString());
  }

  public static void executeExternalProcess(@Nullable final Project myProject,
                                            @Nonnull final ProcessHandler processHandler,
                                            @Nonnull final ExecutionMode mode,
                                            @Nonnull final String presentableCmdline) {
    final String title = mode.getTitle() != null ? mode.getTitle() : "Please wait...";
    assert title != null;

    final Runnable process;
    if (mode.cancelable()) {
      process = createCancelableExecutionProcess(processHandler, mode.shouldCancelFun());
    }
    else {
      if (mode.getTimeout() <= 0) {
        process = new Runnable() {
          @Override
          public void run() {
            processHandler.waitFor();
          }
        };
      }
      else {
        process = createTimelimitedExecutionProcess(processHandler, mode.getTimeout(), presentableCmdline);
      }
    }
    if (mode.withModalProgress()) {
      ProgressManager.getInstance().runProcessWithProgressSynchronously(process, title, mode.cancelable(), myProject, mode.getProgressParentComponent());
    }
    else if (mode.inBackGround()) {
      final Task task = new Task.Backgroundable(myProject, title, mode.cancelable()) {
        @Override
        public void run(@Nonnull final ProgressIndicator indicator) {
          process.run();
        }
      };
      ProgressManager.getInstance().run(task);
    }
    else {
      final String title2 = mode.getTitle2();
      final ProgressIndicator indicator = ProgressManager.getInstance().getProgressIndicator();
      if (indicator != null && title2 != null) {
        indicator.setText2(title2);
      }
      process.run();
    }
  }

  private static Runnable createCancelableExecutionProcess(final ProcessHandler processHandler, final Function<Object, Boolean> cancelableFun) {
    return new Runnable() {
      private ProgressIndicator myProgressIndicator;
      private final Semaphore mySemaphore = new Semaphore();

      private final Runnable myWaitThread = () -> {
        try {
          processHandler.waitFor();
        }
        finally {
          mySemaphore.up();
        }
      };

      private final Runnable myCancelListener = new Runnable() {
        @Override
        public void run() {
          for (; ; ) {
            if ((myProgressIndicator != null && (myProgressIndicator.isCanceled() || !myProgressIndicator.isRunning())) ||
                (cancelableFun != null && cancelableFun.apply(null).booleanValue()) ||
                processHandler.isProcessTerminated()) {

              if (!processHandler.isProcessTerminated()) {
                try {
                  processHandler.destroyProcess();
                }
                finally {
                  mySemaphore.up();
                }
              }
              break;
            }
            try {
              synchronized (this) {
                wait(1000);
              }
            }
            catch (InterruptedException e) {
              //Do nothing
            }
          }
        }
      };

      @Override
      public void run() {
        myProgressIndicator = ProgressManager.getInstance().getProgressIndicator();
        if (myProgressIndicator != null && StringUtil.isEmpty(myProgressIndicator.getText())) {
          myProgressIndicator.setText("Please wait...");
        }

        LOG.assertTrue(myProgressIndicator != null || cancelableFun != null, "Cancelable process must have an opportunity to be canceled!");
        mySemaphore.down();
        ApplicationManager.getApplication().executeOnPooledThread(myWaitThread);
        ApplicationManager.getApplication().executeOnPooledThread(myCancelListener);

        mySemaphore.waitFor();
      }
    };
  }

  private static Runnable createTimelimitedExecutionProcess(final ProcessHandler processHandler, final int timeout, @Nonnull final String presentableCmdline) {
    return new Runnable() {
      private final Semaphore mySemaphore = new Semaphore();

      private final Runnable myProcessThread = new Runnable() {
        @Override
        public void run() {
          try {
            final boolean finished = processHandler.waitFor(1000 * timeout);
            if (!finished) {
              final String msg = "Timeout (" + timeout + " sec) on executing: " + presentableCmdline;
              LOG.error(msg);
              processHandler.destroyProcess();
            }
          }
          finally {
            mySemaphore.up();
          }
        }
      };

      @Override
      public void run() {
        mySemaphore.down();
        ApplicationManager.getApplication().executeOnPooledThread(myProcessThread);

        mySemaphore.waitFor();
      }
    };
  }

  public static class FakeNavigatable implements Navigatable {
    @Override
    public void navigate(boolean requestFocus) {
      // Do nothing
    }

    @Override
    public boolean canNavigate() {
      return false;
    }

    @Override
    public boolean canNavigateToSource() {
      return false;
    }
  }
}
