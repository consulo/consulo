/*
 * Copyright 2000-2011 JetBrains s.r.o.
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
package consulo.ide.impl.idea.openapi.vcs.changes;

import consulo.application.ApplicationManager;
import consulo.application.progress.ProgressIndicator;
import consulo.application.progress.Task;
import consulo.application.util.BackgroundTaskQueue;
import consulo.util.lang.Comparing;
import consulo.ide.impl.idea.openapi.vcs.Details;
import consulo.ide.impl.idea.openapi.vcs.GenericDetailsLoader;
import consulo.versionControlSystem.ui.VcsBalloonProblemNotifier;
import consulo.ide.impl.idea.util.Ticket;
import consulo.ide.impl.idea.util.continuation.ModalityIgnorantBackgroundableTask;
import consulo.logging.Logger;
import consulo.project.Project;
import consulo.project.ui.notification.NotificationType;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.versionControlSystem.VcsException;
import consulo.versionControlSystem.change.Change;

import jakarta.annotation.Nonnull;
import javax.swing.*;
import java.util.function.Consumer;

/**
 * For presentation, which is itself in GenericDetails (not necessarily) - shown from time to time, but cached, and
 * which is a listener to some intensive changes (a group of invalidating changes should provoke a reload, but "outdated" (loaded but already not actual) results should be thrown away)
 *
 *
 * User: Irina.Chernushina
 * Date: 9/7/11
 * Time: 3:13 PM
 */
public abstract class AbstractRefreshablePanel<T> implements RefreshablePanel<Change> {
  private static final Logger LOG = Logger.getInstance(AbstractRefreshablePanel.class);
  protected final Ticket myTicket;
  private final DetailsPanel myDetailsPanel;
  private final GenericDetailsLoader<Ticket, T> myDetailsLoader;
  private final BackgroundTaskQueue myQueue;
  private boolean myDisposed;

  protected AbstractRefreshablePanel(final Project project, final String loadingTitle, final BackgroundTaskQueue queue) {
    myQueue = queue;
    myTicket = new Ticket();
    myDetailsPanel = new DetailsPanel();
    myDetailsPanel.loading();
    myDetailsPanel.layout();
    
    myDetailsLoader = new GenericDetailsLoader<Ticket, T>(ticket -> {
      final Loader loader = new Loader(project, loadingTitle, myTicket.copy());
      loader.runSteadily(new Consumer<>() {
        @Override
        public void accept(Task.Backgroundable backgroundable) {
          myQueue.run(backgroundable);
        }
      });
    }, (ticket, t) -> acceptData(t));
  }

  @Override
  public boolean refreshDataSynch() {
    return false;
  }

  @RequiredUIAccess
  @Override
  public void dataChanged() {
    ApplicationManager.getApplication().assertIsDispatchThread();
    myTicket.increment();
  }

  @RequiredUIAccess
  @Override
  public void refresh() {
    ApplicationManager.getApplication().assertIsDispatchThread();
    
    if (! Comparing.equal(myDetailsLoader.getCurrentlySelected(), myTicket)) {
      final Ticket copy = myTicket.copy();
      myDetailsLoader.updateSelection(copy, false);
      myDetailsPanel.loading();
      myDetailsPanel.layout();
    } else {
      refreshPresentation();
    }
  }

  protected abstract void refreshPresentation();

  protected abstract T loadImpl() throws VcsException;

  @RequiredUIAccess
  protected abstract JPanel dataToPresentation(final T t);

  protected abstract void disposeImpl();
  
  @RequiredUIAccess
  private void acceptData(final T t) {
    final JPanel panel = dataToPresentation(t);
    myDetailsPanel.data(panel);
    myDetailsPanel.layout();
  }

  @Override
  public JPanel getPanel() {
    return myDetailsPanel.getPanel();
  }

  @Override
  public boolean isStillValid(Change data) {
    return true;
  }

  private class Loader extends ModalityIgnorantBackgroundableTask {
    private final Ticket myTicketCopy;
    private T myT;

    private Loader(@jakarta.annotation.Nullable Project project, @Nonnull String title, final Ticket ticketCopy) {
      super(project, title, false, BackgroundFromStartOption.getInstance());
      myTicketCopy = ticketCopy;
    }

    @Override
    @RequiredUIAccess
    protected void doInAwtIfFail(Exception e) {
      final Exception cause;
      if (e instanceof MyRuntime && e.getCause() != null) {
        cause = (Exception) e.getCause();
      } else {
        cause = e;
      }
      LOG.info(e);
      String message = cause.getMessage() == null ? e.getMessage() : cause.getMessage();
      message = message == null ? "Unknown error" : message;
      VcsBalloonProblemNotifier.showOverChangesView((Project)myProject, message, NotificationType.ERROR);
    }

    @Override
    @RequiredUIAccess
    protected void doInAwtIfCancel() {
    }

    @Override
    @RequiredUIAccess
    protected void doInAwtIfSuccess() {
      if (myDisposed) return;
      try {
        myDetailsLoader.take(myTicketCopy, myT);
      }
      catch (Details.AlreadyDisposedException e) {
        // t is not disposable
      }
    }

    @Override
    protected void runImpl(@Nonnull ProgressIndicator indicator) {
      if (myDisposed) return;
      try {
        myT = loadImpl();
      }
      catch (VcsException e) {
        throw new MyRuntime(e);
      }
    }
  }

  private static class MyRuntime extends RuntimeException {
    private MyRuntime(Throwable cause) {
      super(cause);
    }
  }

  @Override
  public void dispose() {
    myDisposed = true;
    disposeImpl();
  }
}
