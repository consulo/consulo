package com.intellij.openapi.externalSystem.service.remote.wrapper;

import com.intellij.openapi.externalSystem.model.settings.ExternalSystemExecutionSettings;
import com.intellij.openapi.externalSystem.model.task.ExternalSystemTaskId;
import com.intellij.openapi.externalSystem.model.task.ExternalSystemTaskType;
import com.intellij.openapi.externalSystem.service.RemoteExternalSystemFacade;
import com.intellij.openapi.externalSystem.service.remote.RemoteExternalSystemProgressNotificationManager;
import com.intellij.openapi.externalSystem.service.remote.RemoteExternalSystemProjectResolver;
import com.intellij.openapi.externalSystem.service.remote.RemoteExternalSystemTaskManager;
import javax.annotation.Nonnull;

import java.rmi.RemoteException;
import java.util.Map;
import java.util.Set;

/**
 * This class acts as a point where target remote gradle services are proxied.
 * <p/>
 * Check service wrapper contracts for more details.
 * <p/>
 * Thread-safe.
 * 
 * @author Denis Zhdanov
 * @since 2/8/12 7:21 PM
 */
public class ExternalSystemFacadeWrapper<S extends ExternalSystemExecutionSettings> implements RemoteExternalSystemFacade<S> {

  @Nonnull
  private final RemoteExternalSystemFacade<S>                   myDelegate;
  @Nonnull
  private final RemoteExternalSystemProgressNotificationManager myProgressManager;

  public ExternalSystemFacadeWrapper(@Nonnull RemoteExternalSystemFacade<S> delegate,
                                     @Nonnull RemoteExternalSystemProgressNotificationManager progressManager)
  {
    myDelegate = delegate;
    myProgressManager = progressManager;
  }

  @Nonnull
  public RemoteExternalSystemFacade<S> getDelegate() {
    return myDelegate;
  }

  @Nonnull
  @Override
  public RemoteExternalSystemProjectResolver<S> getResolver() throws RemoteException, IllegalStateException {
    return new ExternalSystemProjectResolverWrapper<S>(myDelegate.getResolver(), myProgressManager);
  }

  @Nonnull
  @Override
  public RemoteExternalSystemTaskManager<S> getTaskManager() throws RemoteException {
    return new ExternalSystemTaskManagerWrapper<S>(myDelegate.getTaskManager(), myProgressManager);
  }

  @Override
  public void applySettings(@Nonnull S settings) throws RemoteException {
    myDelegate.applySettings(settings);
  }

  @Override
  public void applyProgressManager(@Nonnull RemoteExternalSystemProgressNotificationManager progressManager) throws RemoteException {
    myDelegate.applyProgressManager(progressManager);
  }

  @Override
  public boolean isTaskInProgress(@Nonnull ExternalSystemTaskId id) throws RemoteException {
    return myDelegate.isTaskInProgress(id);
  }

  @Nonnull
  @Override
  public Map<ExternalSystemTaskType, Set<ExternalSystemTaskId>> getTasksInProgress() throws RemoteException {
    return myDelegate.getTasksInProgress();
  }

  @Override
  public boolean cancelTask(@Nonnull ExternalSystemTaskId id) throws RemoteException {
    return myDelegate.cancelTask(id);
  }
}
