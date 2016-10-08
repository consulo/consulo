/*
 * Copyright 2000-2013 JetBrains s.r.o.
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
package com.intellij.openapi.updateSettings.impl.pluginsAdvertisement;

import com.intellij.ide.plugins.IdeaPluginDescriptor;
import com.intellij.ide.plugins.PluginManagerCore;
import com.intellij.ide.plugins.RepositoryHelper;
import com.intellij.notification.NotificationDisplayType;
import com.intellij.notification.NotificationGroup;
import com.intellij.notification.NotificationType;
import com.intellij.openapi.fileTypes.FileNameMatcher;
import com.intellij.openapi.fileTypes.FileNameMatcherFactory;
import com.intellij.openapi.fileTypes.FileTypeFactory;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.startup.StartupActivity;
import com.intellij.openapi.util.Couple;
import consulo.ide.plugins.SimpleExtension;
import org.jetbrains.annotations.NotNull;

import javax.swing.event.HyperlinkEvent;
import java.util.*;
import java.util.stream.Collectors;

public class PluginsAdvertiser implements StartupActivity {
  private static NotificationGroup ourGroup = new NotificationGroup("Plugins Suggestion", NotificationDisplayType.STICKY_BALLOON, true);

  private static List<IdeaPluginDescriptor> ourLoadedPluginDescriptors = Collections.emptyList();

  @NotNull
  public static List<IdeaPluginDescriptor> getLoadedPluginDescriptors() {
    return ourLoadedPluginDescriptors;
  }

  @Override
  public void runActivity(@NotNull final Project project) {
    consulo.ide.updateSettings.UpdateSettings updateSettings = consulo.ide.updateSettings.UpdateSettings.getInstance();
    if (!updateSettings.isEnable()) {
      return;
    }

    Task.Backgroundable.queue(project, "Loading plugin list", false, indicator -> {
      List<IdeaPluginDescriptor> pluginDescriptors = Collections.emptyList();
      try {
        pluginDescriptors = RepositoryHelper.loadPluginsFromRepository(indicator, updateSettings.getChannel());
      }
      catch (Exception ignored) {
      }

      ourLoadedPluginDescriptors = pluginDescriptors;

      if (pluginDescriptors.isEmpty()) {
        return;
      }

      final UnknownFeaturesCollector collectorSuggester = UnknownFeaturesCollector.getInstance(project);
      final Set<UnknownExtension> unknownExtensions = collectorSuggester.getUnknownExtensions();
      if (unknownExtensions.isEmpty()) {
        return;
      }

      final Set<IdeaPluginDescriptor> ids = new HashSet<>();
      for (UnknownExtension feature : unknownExtensions) {
        final Set<IdeaPluginDescriptor> descriptors = findByFeature(pluginDescriptors, feature);
        //do not suggest to download disabled plugins
        final List<String> disabledPlugins = PluginManagerCore.getDisabledPlugins();
        for (IdeaPluginDescriptor id : descriptors) {
          if (!disabledPlugins.contains(id.getPluginId().getIdString())) {
            ids.add(id);
          }
        }
      }

      if (ids.isEmpty()) {
        return;
      }

      ourGroup.createNotification(ourGroup.getDisplayId(), "Features covered by non-installed plugins are detected.<br>" +
                                                           "<a href=\"configure\">Configure plugins...</a><br>" +
                                                           "<a href=\"ignore\">Ignore All</a>", NotificationType.INFORMATION, (notification, event) -> {
        if (event.getEventType() == HyperlinkEvent.EventType.ACTIVATED) {
          final String description = event.getDescription();
          if ("ignore".equals(description)) {
            for (UnknownExtension feature : unknownExtensions) {
              collectorSuggester.ignoreFeature(feature);
            }
            notification.expire();
          }
          else if ("configure".equals(description)) {
            notification.expire();

            new PluginsAdvertiserDialog(project, ids.stream().map(x -> Couple.of(x, x)).collect(Collectors.toList()), ourLoadedPluginDescriptors).show();
          }
        }
      }).notify(project);
    });
  }

  @NotNull
  public static Set<IdeaPluginDescriptor> findByFeature(List<IdeaPluginDescriptor> descriptors, UnknownExtension feature) {
    Set<IdeaPluginDescriptor> filter = new LinkedHashSet<>();
    for (IdeaPluginDescriptor descriptor : descriptors) {
      for (SimpleExtension simpleExtension : descriptor.getSimpleExtensions()) {
        // check is is my extension
        if (feature.getExtensionKey().equals(simpleExtension.getKey())) {
          for (String value : simpleExtension.getValues()) {
            if (isMyFeature(value, feature)) {
              filter.add(descriptor);
            }
          }
        }
      }
    }
    return filter;
  }

  private static boolean isMyFeature(String value, UnknownExtension feature) {
    if (feature.getExtensionKey().equals(FileTypeFactory.FILE_TYPE_FACTORY_EP.getName())) {
      FileNameMatcher matcher = FileNameMatcherFactory.getInstance().createMatcher(value);

      return matcher.accept(feature.getValue());
    }
    else {
      return value.equals(feature.getValue());
    }
  }
}

