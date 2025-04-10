package consulo.externalSystem.impl.internal.service;

import consulo.externalSystem.impl.internal.service.remote.*;
import consulo.externalSystem.model.setting.ExternalSystemExecutionSettings;
import consulo.externalSystem.model.task.*;
import consulo.externalSystem.service.project.ExternalSystemProjectResolver;
import consulo.externalSystem.task.ExternalSystemTaskManager;
import consulo.util.collection.ContainerUtil;
import consulo.util.rmi.RemoteServer;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.rmi.RemoteException;
import java.util.*;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;

/**
 * @author Denis Zhdanov
 * @since 8/8/11 12:51 PM
 */
public abstract class AbstractExternalSystemFacadeImpl<S extends ExternalSystemExecutionSettings> extends RemoteServer
  implements RemoteExternalSystemFacade<S>
{

  private final ConcurrentMap<Class<?>, RemoteExternalSystemService<S>> myRemotes = ContainerUtil.newConcurrentMap();

  private final AtomicReference<S> mySettings = new AtomicReference<>();
  private final AtomicReference<ExternalSystemTaskNotificationListener> myNotificationListener =
    new AtomicReference<>(new ExternalSystemTaskNotificationListenerAdapter() {});

  @Nonnull
  private final RemoteExternalSystemProjectResolverImpl<S> myProjectResolver;
  @Nonnull
  private final RemoteExternalSystemTaskManagerImpl<S> myTaskManager;

  public AbstractExternalSystemFacadeImpl(
    @Nonnull Supplier<ExternalSystemProjectResolver<S>> projectResolverClass,
    @Nonnull Supplier<ExternalSystemTaskManager<S>> buildManagerClass
  ) throws IllegalAccessException, InstantiationException {
    myProjectResolver = new RemoteExternalSystemProjectResolverImpl<>(projectResolverClass.get());
    myTaskManager = new RemoteExternalSystemTaskManagerImpl<>(buildManagerClass.get());
  }

  protected void init() throws RemoteException {
    applyProgressManager(RemoteExternalSystemProgressNotificationManager.NULL_OBJECT);
  }

  @Nullable
  protected S getSettings() {
    return mySettings.get();
  }
  
  @Nonnull
  protected ExternalSystemTaskNotificationListener getNotificationListener() {
    return myNotificationListener.get();
  }
  
  @SuppressWarnings("unchecked")
  @Nonnull
  @Override
  public RemoteExternalSystemProjectResolver<S> getResolver() throws RemoteException, IllegalStateException {
    try {
      return getService(RemoteExternalSystemProjectResolver.class, myProjectResolver);
    }
    catch (Exception e) {
      throw new IllegalStateException(String.format("Can't create '%s' service", RemoteExternalSystemProjectResolverImpl.class.getName()),
                                      e);
    }
  }

  @SuppressWarnings("unchecked")
  @Nonnull
  @Override
  public RemoteExternalSystemTaskManager<S> getTaskManager() throws RemoteException {
    try {
      return getService(RemoteExternalSystemTaskManager.class, myTaskManager);
    }
    catch (Exception e) {
      throw new IllegalStateException(String.format("Can't create '%s' service", ExternalSystemTaskManager.class.getName()), e);
    }
  }

  @SuppressWarnings({"unchecked", "IOResourceOpenedButNotSafelyClosed", "UseOfSystemOutOrSystemErr"})
  private <I extends RemoteExternalSystemService<S>, C extends I> I getService(@Nonnull Class<I> interfaceClass,
                                                                               @Nonnull final C impl)
    throws ClassNotFoundException, IllegalAccessException, InstantiationException, RemoteException
  {
    Object cachedResult = myRemotes.get(interfaceClass);
    if (cachedResult != null) {
      return (I)cachedResult;
    }
    S settings = getSettings();
    if (settings != null) {
      impl.setNotificationListener(getNotificationListener());
      impl.setSettings(settings);
    }
    impl.setNotificationListener(getNotificationListener());
    try {
      I created = createService(interfaceClass, impl);
      I stored = (I)myRemotes.putIfAbsent(interfaceClass, created);
      return stored == null ? created : stored;
    }
    catch (RemoteException e) {
      Object raceResult = myRemotes.get(interfaceClass);
      if (raceResult != null) {
        // Race condition occurred
        return (I)raceResult;
      }
      else {
        throw new IllegalStateException(
          String.format("Can't prepare remote service for interface '%s', implementation '%s'", interfaceClass, impl),
          e
        );
      }
    }
  }

  /**
   * Generic method to retrieve exposed implementations of the target interface.
   * <p/>
   * Uses cached value if it's found; creates new and caches it otherwise.
   *
   * @param interfaceClass  target service interface class
   * @param impl            service implementation
   * @param <I>             service interface class
   * @param <C>             service implementation
   * @return implementation of the target service
   * @throws IllegalAccessException   in case of incorrect assumptions about server class interface
   * @throws InstantiationException   in case of incorrect assumptions about server class interface
   * @throws ClassNotFoundException   in case of incorrect assumptions about server class interface
   * @throws RemoteException
   */
  @SuppressWarnings({"unchecked", "IOResourceOpenedButNotSafelyClosed", "UseOfSystemOutOrSystemErr"})
  protected abstract  <I extends RemoteExternalSystemService<S>, C extends I> I createService(@Nonnull Class<I> interfaceClass,
                                                                                              @Nonnull final C impl)
  throws ClassNotFoundException, IllegalAccessException, InstantiationException, RemoteException;

  @Override
  public boolean isTaskInProgress(@Nonnull ExternalSystemTaskId id) throws RemoteException {
    for (RemoteExternalSystemService service : myRemotes.values()) {
      if (service.isTaskInProgress(id)) {
        return true;
      }
    }
    return false;
  }

  @Nonnull
  @Override
  public Map<ExternalSystemTaskType, Set<ExternalSystemTaskId>> getTasksInProgress() throws RemoteException {
    Map<ExternalSystemTaskType, Set<ExternalSystemTaskId>> result = null;
    for (RemoteExternalSystemService service : myRemotes.values()) {
      final Map<ExternalSystemTaskType, Set<ExternalSystemTaskId>> tasks = service.getTasksInProgress();
      if (tasks.isEmpty()) {
        continue;
      }
      if (result == null) {
        result = new HashMap<>();
      }
      for (Map.Entry<ExternalSystemTaskType, Set<ExternalSystemTaskId>> entry : tasks.entrySet()) {
        Set<ExternalSystemTaskId> ids = result.get(entry.getKey());
        if (ids == null) {
          result.put(entry.getKey(), ids = new HashSet<>());
        }
        ids.addAll(entry.getValue());
      }
    }
    if (result == null) {
      result = Collections.emptyMap();
    }
    return result;
  }

  @Override
  public void applySettings(@Nonnull S settings) throws RemoteException {
    mySettings.set(settings);
    List<RemoteExternalSystemService<S>> services = ContainerUtil.newArrayList(myRemotes.values());
    for (RemoteExternalSystemService<S> service : services) {
      service.setSettings(settings);
    }
  }

  @Override
  public void applyProgressManager(@Nonnull RemoteExternalSystemProgressNotificationManager progressManager) throws RemoteException {
    ExternalSystemTaskNotificationListener listener = new SwallowingNotificationListener(progressManager);
    myNotificationListener.set(listener);
    myProjectResolver.setNotificationListener(listener);
    myTaskManager.setNotificationListener(listener);
  }

  @Override
  public boolean cancelTask(@Nonnull ExternalSystemTaskId id) throws RemoteException {
    if (id.getType() == ExternalSystemTaskType.RESOLVE_PROJECT) {
      return myProjectResolver.cancelTask(id);
    } else {
      return myTaskManager.cancelTask(id);
    }
  }

  private static class SwallowingNotificationListener implements ExternalSystemTaskNotificationListener {
    @Nonnull
    private final RemoteExternalSystemProgressNotificationManager myManager;

    SwallowingNotificationListener(@Nonnull RemoteExternalSystemProgressNotificationManager manager) {
      myManager = manager;
    }

    @Override
    public void onQueued(@Nonnull ExternalSystemTaskId id) {
    }

    @Override
    public void onStart(@Nonnull ExternalSystemTaskId id) {
      try {
        myManager.onStart(id);
      }
      catch (RemoteException e) {
        // Ignore
      }
    }

    @Override
    public void onStatusChange(@Nonnull ExternalSystemTaskNotificationEvent event) {
      try {
        myManager.onStatusChange(event);
      }
      catch (RemoteException e) {
        // Ignore
      }
    }

    @Override
    public void onTaskOutput(@Nonnull ExternalSystemTaskId id, @Nonnull String text, boolean stdOut) {
      try {
        myManager.onTaskOutput(id, text, stdOut);
      }
      catch (RemoteException e) {
        // Ignore
      }
    }

    @Override
    public void onEnd(@Nonnull ExternalSystemTaskId id) {
      try {
        myManager.onEnd(id);
      }
      catch (RemoteException e) {
        // Ignore
      }
    }

    @Override
    public void onSuccess(@Nonnull ExternalSystemTaskId id) {
      try {
        myManager.onSuccess(id);
      }
      catch (RemoteException e) {
        // Ignore
      }
    }

    @Override
    public void onFailure(@Nonnull ExternalSystemTaskId id, @Nonnull Exception ex) {
      try {
        myManager.onFailure(id, ex);
      }
      catch (RemoteException e) {
        // Ignore
      }
    }
  }
}
