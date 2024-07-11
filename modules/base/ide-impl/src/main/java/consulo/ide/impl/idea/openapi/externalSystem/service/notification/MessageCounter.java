/*
 * Copyright 2000-2014 JetBrains s.r.o.
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
package consulo.ide.impl.idea.openapi.externalSystem.service.notification;

import consulo.externalSystem.model.ProjectSystemId;
import consulo.externalSystem.service.notification.NotificationCategory;
import consulo.externalSystem.service.notification.NotificationSource;
import consulo.ide.impl.idea.util.containers.ContainerUtil;
import gnu.trove.TObjectIntHashMap;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * @author Vladislav.Soroka
 * @since 4/4/14
 */
public class MessageCounter {

  private final Map<ProjectSystemId, Map<String/* group */, Map<NotificationSource, TObjectIntHashMap<NotificationCategory>>>> map = new HashMap<>();

  public synchronized void increment(
    @Nonnull String groupName,
    @Nonnull NotificationSource source,
    @Nonnull NotificationCategory category,
    @Nonnull ProjectSystemId projectSystemId
  ) {
    final TObjectIntHashMap<NotificationCategory> counter = ContainerUtil.getOrCreate(
      ContainerUtil.getOrCreate(ContainerUtil.getOrCreate(map, projectSystemId, new HashMap<>()), groupName, new HashMap<>()),
      source,
      new TObjectIntHashMap<>()
    );
    if (!counter.increment(category)) counter.put(category, 1);
  }

  public synchronized void remove(
    @Nullable final String groupName,
    @Nonnull final NotificationSource notificationSource,
    @Nonnull final ProjectSystemId projectSystemId
  ) {
    final Map<String, Map<NotificationSource, TObjectIntHashMap<NotificationCategory>>> groupMap =
      ContainerUtil.getOrCreate(map, projectSystemId, new HashMap<>());
    if (groupName != null) {
      final TObjectIntHashMap<NotificationCategory> counter = ContainerUtil.getOrCreate(
        ContainerUtil.getOrCreate(groupMap, groupName, new HashMap<>()),
        notificationSource,
        new TObjectIntHashMap<>()
      );
      counter.clear();
    }
    else {
      for (Map<NotificationSource, TObjectIntHashMap<NotificationCategory>> sourceMap : groupMap.values()) {
        sourceMap.remove(notificationSource);
      }
    }
  }

  public synchronized int getCount(
    @Nullable final String groupName,
    @Nonnull final NotificationSource notificationSource,
    @Nullable final NotificationCategory notificationCategory,
    @Nonnull final ProjectSystemId projectSystemId
  ) {
    int count = 0;
    final Map<String, Map<NotificationSource, TObjectIntHashMap<NotificationCategory>>> groupMap = ContainerUtil.getOrElse(
      map,
      projectSystemId,
      Collections.<String, Map<NotificationSource, TObjectIntHashMap<NotificationCategory>>>emptyMap()
    );

    for (Map.Entry<String, Map<NotificationSource, TObjectIntHashMap<NotificationCategory>>> entry : groupMap.entrySet()) {
      if (groupName == null || groupName.equals(entry.getKey())) {
        final TObjectIntHashMap<NotificationCategory> counter = entry.getValue().get(notificationSource);
        if (counter == null) continue;

        if (notificationCategory == null) {
          for (int aCount : counter.getValues()) {
            count += aCount;
          }
        }
        else {
          count = counter.get(notificationCategory);
        }
      }
    }

    return count;
  }
}


