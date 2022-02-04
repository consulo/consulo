package com.intellij.openapi.externalSystem.service.remote;

import javax.annotation.Nonnull;
import com.intellij.openapi.externalSystem.model.task.ExternalSystemTaskId;
import com.intellij.openapi.externalSystem.model.task.ExternalSystemTaskNotificationEvent;

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
    public void onQueued(@Nonnull ExternalSystemTaskId id) throws RemoteException {
    }

    @Override
    public void onStart(@Nonnull ExternalSystemTaskId id) {
    }

    @Override
    public void onStatusChange(@Nonnull ExternalSystemTaskNotificationEvent event) {
    }

    @Override
    public void onTaskOutput(@Nonnull ExternalSystemTaskId id, @Nonnull String text, boolean stdOut) {
    }

    @Override
    public void onEnd(@Nonnull ExternalSystemTaskId id) {
    }

    @Override
    public void onSuccess(@Nonnull ExternalSystemTaskId id) throws RemoteException {
    }

    @Override
    public void onFailure(@Nonnull ExternalSystemTaskId id, @Nonnull Exception e) throws RemoteException {
    }
  };

  void onQueued(@Nonnull ExternalSystemTaskId id) throws RemoteException;

  void onStart(@Nonnull ExternalSystemTaskId id) throws RemoteException;

  void onStatusChange(@Nonnull ExternalSystemTaskNotificationEvent event) throws RemoteException;

  void onTaskOutput(@Nonnull ExternalSystemTaskId id, @Nonnull String text, boolean stdOut) throws RemoteException;

  void onEnd(@Nonnull ExternalSystemTaskId id) throws RemoteException;

  void onSuccess(@Nonnull ExternalSystemTaskId id) throws RemoteException;

  void onFailure(@Nonnull ExternalSystemTaskId id, @Nonnull Exception e) throws RemoteException;
}
