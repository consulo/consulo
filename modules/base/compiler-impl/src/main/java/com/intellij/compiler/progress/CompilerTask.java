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

/*
 * @author: Eugene Zhuravlev
 * Date: Jan 22, 2003
 * Time: 2:25:31 PM
 */
package com.intellij.compiler.progress;

import com.intellij.compiler.ProblemsView;
import com.intellij.ide.impl.ProjectUtil;
import com.intellij.openapi.application.Application;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.ModalityState;
import com.intellij.openapi.compiler.*;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.fileEditor.OpenFileDescriptor;
import com.intellij.openapi.progress.EmptyProgressIndicator;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.progress.util.AbstractProgressIndicatorExBase;
import com.intellij.openapi.project.DumbModeAction;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectManager;
import com.intellij.openapi.project.ProjectManagerListener;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.util.TextRange;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.wm.AppIconScheme;
import com.intellij.openapi.wm.ex.ProgressIndicatorEx;
import com.intellij.pom.Navigatable;
import com.intellij.problems.WolfTheProblemSolver;
import com.intellij.ui.AppIcon;
import com.intellij.ui.content.Content;
import com.intellij.ui.content.ContentManager;
import com.intellij.ui.content.ContentManagerAdapter;
import com.intellij.ui.content.ContentManagerEvent;
import com.intellij.util.messages.MessageBusConnection;
import com.intellij.util.ui.MessageCategory;
import com.intellij.util.ui.UIUtil;
import consulo.compiler.impl.CompilerManagerImpl;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

public class CompilerTask extends Task.Backgroundable {
  private static final Logger LOG = Logger.getInstance(CompilerTask.class);
  private static final String APP_ICON_ID = "compiler";

  private final Object myMessageViewLock = new Object();
  private final boolean myHeadlessMode;
  private final boolean myForceAsyncExecution;
  private final boolean myWaitForPreviousSession;
  private int myErrorCount = 0;
  private int myWarningCount = 0;
  private boolean myMessagesAutoActivated = false;

  private volatile ProgressIndicator myIndicator = new EmptyProgressIndicator();
  private Runnable myCompileWork;
  private final boolean myCompilationStartedAutomatically;

  public CompilerTask(@Nonnull Project project,
                      String contentName,
                      final boolean headlessMode,
                      boolean forceAsync,
                      boolean waitForPreviousSession,
                      boolean compilationStartedAutomatically) {
    super(project, contentName);
    myHeadlessMode = headlessMode;
    myForceAsyncExecution = forceAsync;
    myWaitForPreviousSession = waitForPreviousSession;
    myCompilationStartedAutomatically = compilationStartedAutomatically;
  }

  @Override
  public DumbModeAction getDumbModeAction() {
    return DumbModeAction.WAIT;
  }

  @Override
  public boolean shouldStartInBackground() {
    return true;
  }

  public ProgressIndicator getIndicator() {
    return myIndicator;
  }

  @Override
  @Nullable
  public NotificationInfo getNotificationInfo() {
    return new NotificationInfo(myErrorCount > 0 ? "Compiler (errors)" : "Compiler (success)", "Compilation Finished",
                                myErrorCount + " Errors, " + myWarningCount + " Warnings", true);
  }

  @Override
  public void run(@Nonnull final ProgressIndicator indicator) {
    myIndicator = indicator;

    final ProjectManager projectManager = ProjectManager.getInstance();
    CloseListener closeListener;
    projectManager.addProjectManagerListener(myProject, closeListener = new CloseListener());

    final Semaphore semaphore = ((CompilerManagerImpl)CompilerManager.getInstance(myProject)).getCompilationSemaphore();
    boolean acquired = false;
    try {

      try {
        while (!acquired) {
          acquired = semaphore.tryAcquire(300, TimeUnit.MILLISECONDS);
          if (!acquired && !myWaitForPreviousSession) {
            return;
          }
          if (indicator.isCanceled()) {
            // give up obtaining the semaphore,
            // let compile work begin in order to stop gracefuly on cancel event
            break;
          }
        }
      }
      catch (InterruptedException ignored) {
      }

      if (!isHeadless()) {
        addIndicatorDelegate();
      }
      myCompileWork.run();
    }
    finally {
      try {
        indicator.stop();
        projectManager.removeProjectManagerListener(myProject, closeListener);
      }
      finally {
        if (acquired) {
          semaphore.release();
        }
      }
    }
  }

  private void addIndicatorDelegate() {
    ProgressIndicator indicator = myIndicator;
    if (!(indicator instanceof ProgressIndicatorEx)) return;
    ((ProgressIndicatorEx)indicator).addStateDelegate(new AbstractProgressIndicatorExBase() {

      @Override
      public void cancel() {
        super.cancel();
        closeUI();
        stopAppIconProgress();
      }

      @Override
      public void stop() {
        super.stop();
        if (!isCanceled()) {
          closeUI();
        }
        stopAppIconProgress();
      }

      private void stopAppIconProgress() {
        UIUtil.invokeLaterIfNeeded(new Runnable() {
          @Override
          public void run() {
            AppIcon appIcon = AppIcon.getInstance();
            if (appIcon.hideProgress(myProject, APP_ICON_ID)) {
              if (myErrorCount > 0) {
                appIcon.setErrorBadge(myProject, String.valueOf(myErrorCount));
                appIcon.requestAttention(myProject, true);
              }
              else if (!myCompilationStartedAutomatically) {
                appIcon.setOkBadge(myProject, true);
                appIcon.requestAttention(myProject, false);
              }
            }
          }
        });
      }
      @Override
      public void setFraction(final double fraction) {
        super.setFraction(fraction);
        UIUtil.invokeLaterIfNeeded(new Runnable() {
          @Override
          public void run() {
            AppIcon.getInstance().setProgress(myProject, APP_ICON_ID, AppIconScheme.Progress.BUILD, fraction, true);
          }
        });
      }
    });
  }

  public void cancel() {
    if (!myIndicator.isCanceled()) {
      myIndicator.cancel();
    }
  }

  public void addMessage(final CompilerMessage message) {
    final CompilerMessageCategory messageCategory = message.getCategory();
    if (CompilerMessageCategory.WARNING.equals(messageCategory)) {
      myWarningCount += 1;
    }
    else if (CompilerMessageCategory.ERROR.equals(messageCategory)) {
      myErrorCount += 1;
      informWolf(message);
    }

    if (ApplicationManager.getApplication().isDispatchThread()) {
      doAddMessage(message);
    }
    else {
      ApplicationManager.getApplication().invokeLater(new Runnable() {
        @Override
        public void run() {
          if (!myProject.isDisposed()) {
            doAddMessage(message);
          }
        }
      }, ModalityState.NON_MODAL);
    }
  }

  private void informWolf(final CompilerMessage message) {
    WolfTheProblemSolver wolf = WolfTheProblemSolver.getInstance(myProject);
    VirtualFile file = getVirtualFile(message);
    wolf.queue(file);
  }

  private void doAddMessage(final CompilerMessage message) {
    synchronized (myMessageViewLock) {
      final CompilerMessageCategory category = message.getCategory();


      final boolean shouldAutoActivate = !myMessagesAutoActivated &&
                                         (CompilerMessageCategory.ERROR.equals(category) ||
                                          (CompilerMessageCategory.WARNING.equals(category) &&
                                           !ProblemsView.getInstance(myProject).isHideWarnings()));
      if (shouldAutoActivate) {
        myMessagesAutoActivated = true;
        activateMessageView();
      }
    }
  }


  public static int translateCategory(CompilerMessageCategory category) {
    if (CompilerMessageCategory.ERROR.equals(category)) {
      return MessageCategory.ERROR;
    }
    if (CompilerMessageCategory.WARNING.equals(category)) {
      return MessageCategory.WARNING;
    }
    if (CompilerMessageCategory.STATISTICS.equals(category)) {
      return MessageCategory.STATISTICS;
    }
    if (CompilerMessageCategory.INFORMATION.equals(category)) {
      return MessageCategory.INFORMATION;
    }
    LOG.error("Unknown message category: " + category);
    return 0;
  }

  public void start(Runnable compileWork) {
    myCompileWork = compileWork;
    queue();
  }

  private void activateMessageView() {
    synchronized (myMessageViewLock) {
      ProblemsView.getInstance(myProject).showOrHide(false);
    }
  }

  private void closeUI() {
    if (isHeadlessMode()) {
      return;
    }
    final Application application = ApplicationManager.getApplication();
    application.invokeLater(new Runnable() {
      @Override
      public void run() {
        synchronized (myMessageViewLock) {
          final boolean shouldRetainView = myErrorCount > 0 || myWarningCount > 0 && !ProblemsView.getInstance(myProject).isHideWarnings();
          if (shouldRetainView) {
            ProblemsView.getInstance(myProject).selectFirstMessage();
          }
          else {
            ProblemsView.getInstance(myProject).showOrHide(true);
          }
        }
      }
    }, ModalityState.NON_MODAL);
  }

  @Override
  public boolean isHeadless() {
    return myHeadlessMode && !myForceAsyncExecution;
  }

  private boolean isHeadlessMode() {
    return myHeadlessMode;
  }

  private static VirtualFile getVirtualFile(final CompilerMessage message) {
    VirtualFile virtualFile = message.getVirtualFile();
    if (virtualFile == null) {
      Navigatable navigatable = message.getNavigatable();
      if (navigatable instanceof OpenFileDescriptor) {
        virtualFile = ((OpenFileDescriptor)navigatable).getFile();
      }
    }
    return virtualFile;
  }

  public static TextRange getTextRange(final CompilerMessage message) {
    Navigatable navigatable = message.getNavigatable();
    if (navigatable instanceof OpenFileDescriptor) {
      int offset = ((OpenFileDescriptor)navigatable).getOffset();
      return new TextRange(offset, offset);
    }
    return TextRange.EMPTY_RANGE;
  }

  private class CloseListener extends ContentManagerAdapter implements ProjectManagerListener {
    private Content myContent;
    private ContentManager myContentManager;
    private boolean myIsApplicationExitingOrProjectClosing = false;
    private boolean myUserAcceptedCancel = false;

    @Override
    public boolean canCloseProject(final Project project) {
      assert project != null;
      if (!project.equals(myProject)) {
        return true;
      }
      if (shouldAskUser()) {
        int result = Messages.showOkCancelDialog(myProject, CompilerBundle.message("warning.compiler.running.on.project.close"),
                                                 CompilerBundle.message("compiler.running.dialog.title"), Messages.getQuestionIcon());
        if (result != 0) {
          return false; // veto closing
        }
        myUserAcceptedCancel = true;

        final MessageBusConnection connection = project.getMessageBus().connect();
        connection.subscribe(CompilerTopics.COMPILATION_STATUS, new CompilationStatusAdapter() {
          @Override
          public void compilationFinished(boolean aborted, int errors, int warnings, final CompileContext compileContext) {
            connection.disconnect();
            ProjectUtil.closeAndDispose(project);
          }
        });
        cancel();
        return false; // cancel compiler and let it finish, after compilation close the project, but currently - veto closing
      }
      return !myIndicator.isRunning();
    }

    public void setContent(Content content, ContentManager contentManager) {
      myContent = content;
      myContentManager = contentManager;
      contentManager.addContentManagerListener(this);
    }

    @Override
    public void contentRemoved(ContentManagerEvent event) {
      if (event.getContent() == myContent) {
        myContentManager.removeContentManagerListener(this);
        myContent.release();
        myContent = null;
      }
    }

    @Override
    public void contentRemoveQuery(ContentManagerEvent event) {
      if (event.getContent() == myContent) {
        if (!myIndicator.isCanceled() && shouldAskUser()) {
          int result = Messages.showOkCancelDialog(myProject, CompilerBundle.message("warning.compiler.running.on.toolwindow.close"),
                                                   CompilerBundle.message("compiler.running.dialog.title"), Messages.getQuestionIcon());
          if (result != 0) {
            event.consume(); // veto closing
          }
          myUserAcceptedCancel = true;
        }
      }
    }

    private boolean shouldAskUser() {
      // do not ask second time if user already accepted closing
      return !myUserAcceptedCancel && !myIsApplicationExitingOrProjectClosing && myIndicator.isRunning();
    }

    @Override
    public void projectClosed(Project project) {
      if (project.equals(myProject) && myContent != null) {
        myContentManager.removeContent(myContent, true);
      }
    }

    @Override
    public void projectClosing(Project project) {
      if (project.equals(myProject)) {
        myIsApplicationExitingOrProjectClosing = true;
      }
    }
  }
}
                                      