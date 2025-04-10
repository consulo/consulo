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
package consulo.externalSystem.impl.internal.service.notification;

import consulo.externalSystem.model.ProjectSystemId;
import consulo.externalSystem.service.notification.NotificationCategory;
import consulo.externalSystem.service.notification.NotificationSource;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.util.HashMap;
import java.util.Map;

/**
 * @author Vladislav.Soroka
 * @since 4/4/14
 */
public class MessageCounter {

    private final Map<ProjectSystemId, Map<String/* group */, Map<NotificationSource, Object2IntMap<NotificationCategory>>>> map = new HashMap<>();

    public synchronized void increment(
        @Nonnull String groupName,
        @Nonnull NotificationSource source,
        @Nonnull NotificationCategory category,
        @Nonnull ProjectSystemId projectSystemId
    ) {

        Map<String, Map<NotificationSource, Object2IntMap<NotificationCategory>>> groupMap =
            map.computeIfAbsent(projectSystemId, p -> new HashMap<>());

        Map<NotificationSource, Object2IntMap<NotificationCategory>> m = groupMap.computeIfAbsent(groupName, string -> new HashMap<>());

        Object2IntMap<NotificationCategory> counter = m.computeIfAbsent(source, n -> new Object2IntOpenHashMap<>());

        counter.put(category, counter.getInt(category) + 1);
    }

    public synchronized void remove(
        @Nullable final String groupName,
        @Nonnull final NotificationSource notificationSource,
        @Nonnull final ProjectSystemId projectSystemId
    ) {
        Map<String, Map<NotificationSource, Object2IntMap<NotificationCategory>>> groupMap =
            map.computeIfAbsent(projectSystemId, p -> new HashMap<>());

        if (groupName != null) {
            Map<NotificationSource, Object2IntMap<NotificationCategory>> m = groupMap.computeIfAbsent(groupName, string -> new HashMap<>());

            Object2IntMap<NotificationCategory> counter = m.computeIfAbsent(notificationSource, n -> new Object2IntOpenHashMap<>());

            counter.clear();
        }
        else {
            for (Map<NotificationSource, Object2IntMap<NotificationCategory>> sourceMap : groupMap.values()) {
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
        Map<String, Map<NotificationSource, Object2IntMap<NotificationCategory>>> groupMap = map.getOrDefault(projectSystemId, Map.of());

        int count = 0;

        for (Map.Entry<String, Map<NotificationSource, Object2IntMap<NotificationCategory>>> entry : groupMap.entrySet()) {
            if (groupName == null || groupName.equals(entry.getKey())) {
                final Object2IntMap<NotificationCategory> counter = entry.getValue().get(notificationSource);
                if (counter == null) {
                    continue;
                }

                if (notificationCategory == null) {
                    for (int aCount : counter.values()) {
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


