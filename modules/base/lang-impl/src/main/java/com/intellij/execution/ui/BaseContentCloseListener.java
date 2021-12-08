// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.execution.ui;

import com.intellij.execution.ExecutionBundle;
import com.intellij.execution.TerminateRemoteProcessDialog;
import com.intellij.execution.process.ProcessHandler;
import com.intellij.ide.GeneralSettings;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectManager;
import com.intellij.openapi.project.ProjectManagerListener;
import com.intellij.openapi.project.ex.ProjectManagerEx;
import com.intellij.ui.content.Content;
import com.intellij.ui.content.ContentManager;
import com.intellij.ui.content.ContentManagerEvent;
import com.intellij.ui.content.ContentManagerListener;
import com.intellij.util.concurrency.Semaphore;
import consulo.disposer.Disposable;
import consulo.disposer.Disposer;
import consulo.util.dataholder.Key;

import javax.annotation.Nonnull;

import javax.annotation.Nullable;
import java.util.Objects;

public abstract class BaseContentCloseListener implements ProjectManagerListener, ContentManagerListener {
  private static final Key<Boolean> PROJECT_DISPOSING = Key.create("Project disposing is in progress");
  private static final Logger LOG = Logger.getInstance(BaseContentCloseListener.class);

  private Content myContent;
  private final Project myProject;

  public BaseContentCloseListener(@Nonnull Content content, @Nonnull Project project) {
    myContent = content;
    myProject = project;
    final ContentManager contentManager = content.getManager();
    if (contentManager != null) {
      contentManager.addContentManagerListener(this);
    }
    ProjectManagerEx projectManager = (ProjectManagerEx)ProjectManager.getInstance();
    projectManager.addProjectManagerListener(myProject, this);
    Disposable vetoDisposable = projectManager.registerCloseProjectVeto(this::canClose);
    Disposer.register(myProject, vetoDisposable);
  }

  @Override
  public void contentRemoved(@Nonnull final ContentManagerEvent event) {
    final Content content = event.getContent();
    if (content == myContent) {
      dispose();
    }
  }

  public void dispose() {
    if (myContent == null) return;

    final Content content = myContent;
    myContent = null;
    final ContentManager contentManager = content.getManager();
    if (contentManager != null) {
      contentManager.removeContentManagerListener(this);
    }
    ProjectManager.getInstance().removeProjectManagerListener(myProject, this);
    disposeContent(content);
  }

  protected abstract void disposeContent(@Nonnull Content content);

  @Override
  public void contentRemoveQuery(@Nonnull ContentManagerEvent event) {
    if (event.getContent() == myContent) {
      boolean canClose = closeQuery(myContent, Boolean.TRUE.equals(myProject.getUserData(PROJECT_DISPOSING)));
      if (!canClose) {
        // Consume the event to reject the close request:
        //   com.intellij.ui.content.impl.ContentManagerImpl.fireContentRemoveQuery
        event.consume();
      }
    }
  }

  @Override
  public void projectClosed(@Nonnull Project project) {
    if (myContent == null || project != myProject) {
      return;
    }

    ContentManager contentManager = myContent.getManager();
    if (contentManager != null) {
      contentManager.removeContent(myContent, true);
    }
    // dispose content even if content manager refused to
    dispose();
  }

  @Override
  public void projectClosing(@Nonnull Project project) {
    project.putUserData(PROJECT_DISPOSING, true);
  }

  public boolean canClose(@Nonnull Project project) {
    if (myContent == null || project != myProject) {
      return true;
    }

    boolean canClose = closeQuery(myContent, true);
    // content could be removed during close query
    if (canClose && myContent != null) {
      Objects.requireNonNull(myContent.getManager()).removeContent(myContent, true);
      myContent = null;
    }
    return canClose;
  }

  protected boolean askUserAndWait(@Nonnull ProcessHandler processHandler, @Nonnull String sessionName, @Nonnull WaitForProcessTask task) {
    if (ApplicationManager.getApplication().isWriteAccessAllowed()) {
      // This might happens from Application.exit(force=true, ...) call.
      // Do not show any UI, destroy the process silently, do not wait for process termination.
      processHandler.destroyProcess();
      LOG.info("Destroying process under write action (name: " + sessionName + ", " + processHandler.getClass() + ", " + processHandler.toString() + ")");
      return true;
    }
    GeneralSettings.ProcessCloseConfirmation rc = TerminateRemoteProcessDialog.show(myProject, sessionName, processHandler);
    if (rc == null) { // cancel
      return false;
    }
    boolean destroyProcess = rc == GeneralSettings.ProcessCloseConfirmation.TERMINATE;
    if (destroyProcess) {
      processHandler.destroyProcess();
    }
    else {
      processHandler.detachProcess();
    }
    ProgressManager.getInstance().run(task);
    return true;
  }

  /**
   * Checks if the specified {@code Content} instance can be closed/removed.
   *
   * @param content        {@code Content} instance the closing operation was requested for
   * @param projectClosing true if the content's project is being closed
   * @return true if the content can be closed, otherwise false
   */
  protected abstract boolean closeQuery(@Nonnull Content content, boolean projectClosing);

  protected abstract static class WaitForProcessTask extends Task.Backgroundable {
    final ProcessHandler myProcessHandler;
    final boolean myModal;

    protected WaitForProcessTask(@Nonnull ProcessHandler processHandler, @Nonnull String processName, boolean modal, @Nullable Project project) {
      super(project, ExecutionBundle.message("terminating.process.progress.title", processName));
      myProcessHandler = processHandler;
      myModal = modal;
    }

    @Override
    public boolean isConditionalModal() {
      return myModal;
    }

    @Override
    public boolean shouldStartInBackground() {
      return !myModal;
    }

    @Override
    public void run(@Nonnull ProgressIndicator progressIndicator) {
      final Semaphore semaphore = new Semaphore();
      semaphore.down();

      ApplicationManager.getApplication().executeOnPooledThread(() -> {
        try {
          myProcessHandler.waitFor();
        }
        finally {
          semaphore.up();
        }
      });
      progressIndicator.setText(ExecutionBundle.message("waiting.for.vm.detach.progress.text"));
      ApplicationManager.getApplication().executeOnPooledThread(new Runnable() {
        @Override
        public void run() {
          while (true) {
            if (progressIndicator.isCanceled() || !progressIndicator.isRunning()) {
              semaphore.up();
              break;
            }
            try {
              //noinspection SynchronizeOnThis
              synchronized (this) {
                //noinspection SynchronizeOnThis
                wait(2000L);
              }
            }
            catch (InterruptedException ignore) {
            }
          }
        }
      });
      semaphore.waitFor();
    }

    @Override
    abstract public void onCancel(); //force user to override
  }
}