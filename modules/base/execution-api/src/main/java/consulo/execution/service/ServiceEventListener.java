// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.execution.service;

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.TopicAPI;
import consulo.annotation.component.TopicBroadcastDirection;
import org.jspecify.annotations.Nullable;

@TopicAPI(value = ComponentScope.PROJECT, direction = TopicBroadcastDirection.TO_CHILDREN)
public interface ServiceEventListener {
  Class<ServiceEventListener> TOPIC = ServiceEventListener.class;

  void handle(ServiceEvent event);

  final class ServiceEvent {
    public final EventType type;
    public final Object target;
    public final Class<?> contributorClass;

    public final Object parent;

    private ServiceEvent(EventType type,
                         Object target,
                         Class<?> contributorClass) {
      this(type, target, contributorClass, null);
    }

    private ServiceEvent(EventType type,
                         Object target,
                         Class<?> contributorClass,
                         @Nullable Object parent) {
      this.type = type;
      this.target = target;
      this.contributorClass = contributorClass;
      this.parent = parent;
    }

    @Override
    public String toString() {
      return type + ": " + target.toString() + "; from contributor: " + contributorClass +
        (parent == null ? "" : "; parent: " + parent);
    }

    public static ServiceEvent createEvent(EventType type,
                                           Object target,
                                           Class<?> rootContributorClass) {
      return new ServiceEvent(type, target, rootContributorClass);
    }

    public static ServiceEvent createResetEvent(Class<?> rootContributorClass) {
      return new ServiceEvent(EventType.RESET, rootContributorClass, rootContributorClass);
    }

    public static ServiceEvent createUnloadSyncResetEvent(Class<?> rootContributorClass) {
      return new ServiceEvent(EventType.UNLOAD_SYNC_RESET, rootContributorClass, rootContributorClass);
    }

    public static ServiceEvent createServiceAddedEvent(Object target,
                                                       Class<?> contributorClass,
                                                       @Nullable Object parent) {
      return new ServiceEvent(EventType.SERVICE_ADDED, target, contributorClass, parent);
    }
  }

  enum EventType {
    RESET,
    UNLOAD_SYNC_RESET,
    SERVICE_ADDED,
    SERVICE_REMOVED,
    SERVICE_CHANGED,
    SERVICE_STRUCTURE_CHANGED,
    SERVICE_CHILDREN_CHANGED,
    SERVICE_GROUP_CHANGED,
    GROUP_CHANGED
  }
}
