package consulo.externalSystem.impl.internal.service.remote;

import consulo.externalSystem.model.task.ExternalSystemTaskId;
import consulo.externalSystem.model.task.ExternalSystemTaskNotificationEvent;

import java.rmi.Remote;
import java.rmi.RemoteException;

/**
 * Defines interface for the entity that manages notifications about progress of long-running operations performed at Gradle API side.
 * <p/>
 * Implementations of this interface are expected to be thread-safe.
 * 
 * @author Denis Zhdanov
 * @since 11/10/11 9:03 AM
 */
public interface RemoteExternalSystemProgressNotificationManager extends Remote {

  RemoteExternalSystemProgressNotificationManager NULL_OBJECT = new RemoteExternalSystemProgressNotificationManager() {
    @Override
    public void onQueued(ExternalSystemTaskId id) throws RemoteException {
    }

    @Override
    public void onStart(ExternalSystemTaskId id) {
    }

    @Override
    public void onStatusChange(ExternalSystemTaskNotificationEvent event) {
    }

    @Override
    public void onTaskOutput(ExternalSystemTaskId id, String text, boolean stdOut) {
    }

    @Override
    public void onEnd(ExternalSystemTaskId id) {
    }

    @Override
    public void onSuccess(ExternalSystemTaskId id) throws RemoteException {
    }

    @Override
    public void onFailure(ExternalSystemTaskId id, Exception e) throws RemoteException {
    }
  };

  void onQueued(ExternalSystemTaskId id) throws RemoteException;

  void onStart(ExternalSystemTaskId id) throws RemoteException;

  void onStatusChange(ExternalSystemTaskNotificationEvent event) throws RemoteException;

  void onTaskOutput(ExternalSystemTaskId id, String text, boolean stdOut) throws RemoteException;

  void onEnd(ExternalSystemTaskId id) throws RemoteException;

  void onSuccess(ExternalSystemTaskId id) throws RemoteException;

  void onFailure(ExternalSystemTaskId id, Exception e) throws RemoteException;
}
