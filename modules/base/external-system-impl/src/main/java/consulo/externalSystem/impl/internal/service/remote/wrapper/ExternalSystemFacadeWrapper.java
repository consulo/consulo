package consulo.externalSystem.impl.internal.service.remote.wrapper;

import consulo.externalSystem.impl.internal.service.RemoteExternalSystemFacade;
import consulo.externalSystem.impl.internal.service.remote.RemoteExternalSystemProgressNotificationManager;
import consulo.externalSystem.impl.internal.service.remote.RemoteExternalSystemProjectResolver;
import consulo.externalSystem.impl.internal.service.remote.RemoteExternalSystemTaskManager;
import consulo.externalSystem.model.setting.ExternalSystemExecutionSettings;
import consulo.externalSystem.model.task.ExternalSystemTaskId;
import consulo.externalSystem.model.task.ExternalSystemTaskType;

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

  private final RemoteExternalSystemFacade<S>                   myDelegate;
  
  private final RemoteExternalSystemProgressNotificationManager myProgressManager;

  public ExternalSystemFacadeWrapper(RemoteExternalSystemFacade<S> delegate,
                                     RemoteExternalSystemProgressNotificationManager progressManager)
  {
    myDelegate = delegate;
    myProgressManager = progressManager;
  }

  public RemoteExternalSystemFacade<S> getDelegate() {
    return myDelegate;
  }

  @Override
  public RemoteExternalSystemProjectResolver<S> getResolver() throws IllegalStateException {
    return new ExternalSystemProjectResolverWrapper<S>(myDelegate.getResolver(), myProgressManager);
  }

  @Override
  public RemoteExternalSystemTaskManager<S> getTaskManager() {
    return new ExternalSystemTaskManagerWrapper<S>(myDelegate.getTaskManager(), myProgressManager);
  }

  @Override
  public void applySettings(S settings) {
    myDelegate.applySettings(settings);
  }

  @Override
  public void applyProgressManager(RemoteExternalSystemProgressNotificationManager progressManager) {
    myDelegate.applyProgressManager(progressManager);
  }

  @Override
  public boolean isTaskInProgress(ExternalSystemTaskId id) {
    return myDelegate.isTaskInProgress(id);
  }

  @Override
  public Map<ExternalSystemTaskType, Set<ExternalSystemTaskId>> getTasksInProgress() {
    return myDelegate.getTasksInProgress();
  }

  @Override
  public boolean cancelTask(ExternalSystemTaskId id) {
    return myDelegate.cancelTask(id);
  }
}
