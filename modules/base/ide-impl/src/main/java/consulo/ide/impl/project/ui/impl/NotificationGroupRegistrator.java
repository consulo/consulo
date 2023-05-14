/*
 * Copyright 2013-2022 consulo.io
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
package consulo.ide.impl.project.ui.impl;

import consulo.application.Application;
import consulo.component.extension.ExtensionPointCacheKey;
import consulo.logging.Logger;
import consulo.project.ui.notification.NotificationGroup;
import consulo.project.ui.notification.NotificationGroupContributor;
import consulo.util.collection.impl.map.ConcurrentHashMap;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import java.util.Map;
import java.util.function.Consumer;

/**
 * @author VISTALL
 * @since 08-Aug-22
 */
public class NotificationGroupRegistrator implements Consumer<NotificationGroup> {
  private static final ExtensionPointCacheKey<NotificationGroupContributor, NotificationGroupRegistrator> KEY =
    ExtensionPointCacheKey.create("NotificationGroupRegistrator", walker -> {
      NotificationGroupRegistrator registrator = new NotificationGroupRegistrator();
      walker.walk(contributor -> contributor.contribute(registrator));
      return registrator;
    });

  @Nonnull
  public static NotificationGroupRegistrator last() {
    Application application = Application.get();
    return application.getExtensionPoint(NotificationGroupContributor.class).getOrBuildCache(KEY);
  }

  private static final Logger LOG = Logger.getInstance(NotificationGroupRegistrator.class);

  private final Map<String, NotificationGroup> myGroups = new ConcurrentHashMap<>();

  @Override
  public void accept(NotificationGroup notificationGroup) {
    if (myGroups.containsKey(notificationGroup.getId())) {
      LOG.error("NotificationGroup: " + notificationGroup.getId() + " is already registered. Skipped this registration");
      return;
    }

    myGroups.put(notificationGroup.getId(), notificationGroup);
  }

  @Nullable
  public NotificationGroup get(String groupId) {
    return myGroups.get(groupId);
  }

  @Nonnull
  public Iterable<NotificationGroup> all() {
    return myGroups.values();
  }
}
