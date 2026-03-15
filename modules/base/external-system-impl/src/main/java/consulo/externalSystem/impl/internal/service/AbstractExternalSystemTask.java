package consulo.externalSystem.impl.internal.service;

import consulo.application.Application;
import consulo.application.progress.ProgressIndicator;
import consulo.externalSystem.ExternalSystemBundle;
import consulo.externalSystem.model.ProjectSystemId;
import consulo.externalSystem.model.task.*;
import consulo.externalSystem.service.notification.ExternalSystemProgressNotificationManager;
import consulo.logging.Logger;
import consulo.project.Project;
import consulo.util.collection.ArrayUtil;

import java.util.concurrent.atomic.AtomicReference;

/**
 * Encapsulates particular task performed by external system integration.
 * <p/>
 * Thread-safe.
 *
 * @author Denis Zhdanov
 * @since 1/24/12 7:03 AM
 */
public abstract class AbstractExternalSystemTask implements ExternalSystemTask {

  private static final Logger LOG = Logger.getInstance(AbstractExternalSystemTask.class);

  private final AtomicReference<ExternalSystemTaskState> myState =
    new AtomicReference<ExternalSystemTaskState>(ExternalSystemTaskState.NOT_STARTED);
  private final AtomicReference<Throwable> myError = new AtomicReference<Throwable>();

  
  private final transient Project myIdeProject;

  
  private final ExternalSystemTaskId myId;
  
  private final ProjectSystemId myExternalSystemId;
  
  private final String myExternalProjectPath;

  protected AbstractExternalSystemTask(ProjectSystemId id,
                                       ExternalSystemTaskType type,
                                       Project project,
                                       String externalProjectPath) {
    myExternalSystemId = id;
    myIdeProject = project;
    myId = ExternalSystemTaskId.create(id, type, myIdeProject);
    myExternalProjectPath = externalProjectPath;
  }

  
  public ProjectSystemId getExternalSystemId() {
    return myExternalSystemId;
  }

  
  public ExternalSystemTaskId getId() {
    return myId;
  }

  
  public ExternalSystemTaskState getState() {
    return myState.get();
  }

  protected void setState(ExternalSystemTaskState state) {
    myState.set(state);
  }

  protected boolean compareAndSetState(ExternalSystemTaskState expect, ExternalSystemTaskState update) {
    return myState.compareAndSet(expect, update);
  }

  @Override
  public Throwable getError() {
    return myError.get();
  }

  
  public Project getIdeProject() {
    return myIdeProject;
  }

  
  public String getExternalProjectPath() {
    return myExternalProjectPath;
  }

  public void refreshState() {
    if (getState() != ExternalSystemTaskState.IN_PROGRESS) {
      return;
    }
    ExternalSystemFacadeManager manager = Application.get().getInstance(ExternalSystemFacadeManager.class);
    try {
      RemoteExternalSystemFacade facade = manager.getFacade(myIdeProject, myExternalProjectPath, myExternalSystemId);
      setState(facade.isTaskInProgress(getId()) ? ExternalSystemTaskState.IN_PROGRESS : ExternalSystemTaskState.FAILED);
    }
    catch (Throwable e) {
      setState(ExternalSystemTaskState.FAILED);
      myError.set(e);
      if (!myIdeProject.isDisposed()) {
        LOG.warn(e);
      }
    }
  }

  @Override
  public void execute(final ProgressIndicator indicator, ExternalSystemTaskNotificationListener... listeners) {
    indicator.setIndeterminate(true);
    ExternalSystemTaskNotificationListenerAdapter adapter = new ExternalSystemTaskNotificationListenerAdapter() {
      @Override
      public void onStatusChange(ExternalSystemTaskNotificationEvent event) {
        indicator.setText(wrapProgressText(event.getDescription()));
      }
    };
    ExternalSystemTaskNotificationListener[] ls;
    if (listeners.length > 0) {
      ls = ArrayUtil.append(listeners, adapter);
    }
    else {
      ls = new ExternalSystemTaskNotificationListener[]{adapter};
    }

    execute(ls);
  }

  @Override
  public void execute(ExternalSystemTaskNotificationListener... listeners) {
    if (!compareAndSetState(ExternalSystemTaskState.NOT_STARTED, ExternalSystemTaskState.IN_PROGRESS)) return;

    ExternalSystemProgressNotificationManager progressManager = Application.get().getInstance(ExternalSystemProgressNotificationManager.class);
    for (ExternalSystemTaskNotificationListener listener : listeners) {
      progressManager.addNotificationListener(getId(), listener);
    }
    ExternalSystemProcessingManager processingManager = Application.get().getInstance(ExternalSystemProcessingManager.class);
    try {
      processingManager.add(this);
      doExecute();
      setState(ExternalSystemTaskState.FINISHED);
    }
    catch (Throwable e) {
      setState(ExternalSystemTaskState.FAILED);
      myError.set(e);
      LOG.warn(e);
    }
    finally {
      for (ExternalSystemTaskNotificationListener listener : listeners) {
        progressManager.removeNotificationListener(listener);
      }
      processingManager.release(getId());
    }
  }

  protected abstract void doExecute() throws Exception;

  @Override
  public boolean cancel(final ProgressIndicator indicator, ExternalSystemTaskNotificationListener... listeners) {
    indicator.setIndeterminate(true);
    ExternalSystemTaskNotificationListenerAdapter adapter = new ExternalSystemTaskNotificationListenerAdapter() {
      @Override
      public void onStatusChange(ExternalSystemTaskNotificationEvent event) {
        indicator.setText(wrapProgressText(event.getDescription()));
      }
    };
    ExternalSystemTaskNotificationListener[] ls;
    if (listeners.length > 0) {
      ls = ArrayUtil.append(listeners, adapter);
    }
    else {
      ls = new ExternalSystemTaskNotificationListener[]{adapter};
    }

    return cancel(ls);
  }

  @Override
  public boolean cancel(ExternalSystemTaskNotificationListener... listeners) {
    ExternalSystemTaskState currentTaskState = getState();
    if (currentTaskState.isStopped()) return true;

    ExternalSystemProgressNotificationManager progressManager = Application.get().getInstance(ExternalSystemProgressNotificationManager.class);
    for (ExternalSystemTaskNotificationListener listener : listeners) {
      progressManager.addNotificationListener(getId(), listener);
    }

    if (!compareAndSetState(currentTaskState, ExternalSystemTaskState.CANCELING)) return false;

    boolean result = false;
    try {
      result = doCancel();
      setState(result ? ExternalSystemTaskState.CANCELED : ExternalSystemTaskState.CANCELLATION_FAILED);
      return result;
    }
    catch (Throwable e) {
      setState(ExternalSystemTaskState.CANCELLATION_FAILED);
      myError.set(e);
      LOG.warn(e);
    }
    finally {
      for (ExternalSystemTaskNotificationListener listener : listeners) {
        progressManager.removeNotificationListener(listener);
      }
    }
    return result;
  }

  protected abstract boolean doCancel() throws Exception;


  
  protected String wrapProgressText(String text) {
    return ExternalSystemBundle.message("progress.update.text", getExternalSystemId(), text);
  }

  @Override
  public int hashCode() {
    return myId.hashCode() + myExternalSystemId.hashCode();
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    AbstractExternalSystemTask task = (AbstractExternalSystemTask)o;
    return myId.equals(task.myId) && myExternalSystemId.equals(task.myExternalSystemId);
  }

  @Override
  public String toString() {
    return String.format("%s task %s: %s", myExternalSystemId.getReadableName(), myId, myState);
  }
}
