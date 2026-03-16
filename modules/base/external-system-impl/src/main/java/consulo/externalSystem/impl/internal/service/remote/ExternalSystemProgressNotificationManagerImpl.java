package consulo.externalSystem.impl.internal.service.remote;

import consulo.annotation.component.ServiceImpl;
import consulo.externalSystem.model.task.ExternalSystemTaskId;
import consulo.externalSystem.model.task.ExternalSystemTaskNotificationEvent;
import consulo.externalSystem.model.task.ExternalSystemTaskNotificationListener;
import consulo.externalSystem.service.notification.ExternalSystemProgressNotificationManager;
import consulo.util.collection.ContainerUtil;
import consulo.util.rmi.RemoteObject;
import jakarta.inject.Singleton;

import java.rmi.RemoteException;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentMap;

/**
 * @author Denis Zhdanov
 * @since 11/10/11 11:56 AM
 */
@Singleton
@ServiceImpl
public class ExternalSystemProgressNotificationManagerImpl extends RemoteObject implements ExternalSystemProgressNotificationManager, RemoteExternalSystemProgressNotificationManager {

  private final ConcurrentMap<ExternalSystemTaskNotificationListener, Set<ExternalSystemTaskId>/* EMPTY_SET as a sign of 'all ids' */> myListeners = ContainerUtil.newConcurrentMap();

  @Override
  public boolean addNotificationListener(ExternalSystemTaskNotificationListener listener) {
    Set<ExternalSystemTaskId> dummy = Collections.emptySet();
    return myListeners.put(listener, dummy) == null;
  }

  @Override
  public boolean addNotificationListener(ExternalSystemTaskId taskId, ExternalSystemTaskNotificationListener listener) {
    Set<ExternalSystemTaskId> ids = null;
    while (ids == null) {
      if (myListeners.containsKey(listener)) {
        ids = myListeners.get(listener);
      }
      else {
        ids = myListeners.putIfAbsent(listener, ContainerUtil.newConcurrentSet());
      }
    }
    return ids.add(taskId);
  }

  @Override
  public boolean removeNotificationListener(ExternalSystemTaskNotificationListener listener) {
    return myListeners.remove(listener) != null;
  }

  @Override
  public void onQueued(ExternalSystemTaskId id) {
    for (Map.Entry<ExternalSystemTaskNotificationListener, Set<ExternalSystemTaskId>> entry : myListeners.entrySet()) {
      Set<ExternalSystemTaskId> ids = entry.getValue();
      if (Collections.EMPTY_SET == ids || ids.contains(id)) {
        entry.getKey().onQueued(id);
      }
    }

  }

  @Override
  public void onStart(ExternalSystemTaskId id) {
    for (Map.Entry<ExternalSystemTaskNotificationListener, Set<ExternalSystemTaskId>> entry : myListeners.entrySet()) {
      Set<ExternalSystemTaskId> ids = entry.getValue();
      if (Collections.EMPTY_SET == ids || ids.contains(id)) {
        entry.getKey().onStart(id);
      }
    }
  }

  @Override
  public void onStatusChange(ExternalSystemTaskNotificationEvent event) {
    for (Map.Entry<ExternalSystemTaskNotificationListener, Set<ExternalSystemTaskId>> entry : myListeners.entrySet()) {
      Set<ExternalSystemTaskId> ids = entry.getValue();
      if (Collections.EMPTY_SET == ids || ids.contains(event.getId())) {
        entry.getKey().onStatusChange(event);
      }
    }
  }

  @Override
  public void onTaskOutput(ExternalSystemTaskId id, String text, boolean stdOut) throws RemoteException {
    for (Map.Entry<ExternalSystemTaskNotificationListener, Set<ExternalSystemTaskId>> entry : myListeners.entrySet()) {
      Set<ExternalSystemTaskId> ids = entry.getValue();
      if (Collections.EMPTY_SET == ids || ids.contains(id)) {
        entry.getKey().onTaskOutput(id, text, stdOut);
      }
    }
  }

  @Override
  public void onEnd(ExternalSystemTaskId id) {
    for (Map.Entry<ExternalSystemTaskNotificationListener, Set<ExternalSystemTaskId>> entry : myListeners.entrySet()) {
      Set<ExternalSystemTaskId> ids = entry.getValue();
      if (Collections.EMPTY_SET == ids || ids.contains(id)) {
        entry.getKey().onEnd(id);
      }
    }
  }

  @Override
  public void onSuccess(ExternalSystemTaskId id) throws RemoteException {
    for (Map.Entry<ExternalSystemTaskNotificationListener, Set<ExternalSystemTaskId>> entry : myListeners.entrySet()) {
      Set<ExternalSystemTaskId> ids = entry.getValue();
      if (Collections.EMPTY_SET == ids || ids.contains(id)) {
        entry.getKey().onSuccess(id);
      }
    }
  }

  @Override
  public void onFailure(ExternalSystemTaskId id, Exception e) throws RemoteException {
    for (Map.Entry<ExternalSystemTaskNotificationListener, Set<ExternalSystemTaskId>> entry : myListeners.entrySet()) {
      Set<ExternalSystemTaskId> ids = entry.getValue();
      if (Collections.EMPTY_SET == ids || ids.contains(id)) {
        entry.getKey().onFailure(id, e);
      }
    }
  }
}
