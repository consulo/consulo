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
package consulo.ide.impl.idea.ide.plugins.pluginsAdvertisement;

import consulo.annotation.component.ExtensionImpl;
import consulo.application.dumb.DumbAware;
import consulo.container.plugin.PluginDescriptor;
import consulo.container.plugin.PluginManager;
import consulo.container.plugin.SimpleExtension;
import consulo.ide.impl.idea.notification.NotificationAction;
import consulo.ide.impl.idea.openapi.util.text.StringUtil;
import consulo.fileEditor.EditorNotifications;
import consulo.ide.impl.plugins.pluginsAdvertisement.PluginsAdvertiserDialog;
import consulo.ide.impl.plugins.pluginsAdvertisement.PluginsAdvertiserHolder;
import consulo.ide.impl.updateSettings.UpdateSettings;
import consulo.project.Project;
import consulo.project.UnknownExtension;
import consulo.project.UnknownFeaturesCollector;
import consulo.project.startup.BackgroundStartupActivity;
import consulo.project.ui.notification.Notification;
import consulo.project.ui.notification.NotificationDisplayType;
import consulo.project.ui.notification.NotificationGroup;
import consulo.project.ui.notification.NotificationType;
import consulo.ui.UIAccess;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.action.AnActionEvent;
import consulo.ui.ex.awt.UIUtil;
import consulo.virtualFileSystem.fileType.FileNameMatcher;
import consulo.virtualFileSystem.fileType.FileNameMatcherFactory;
import consulo.virtualFileSystem.fileType.FileTypeFactory;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.*;

@ExtensionImpl
public class PluginsAdvertiser implements BackgroundStartupActivity, DumbAware {
  private static NotificationGroup ourGroup = new NotificationGroup("Plugins Suggestion", NotificationDisplayType.STICKY_BALLOON, true);

  @Override
  public void runActivity(@Nonnull final Project project, @Nonnull UIAccess uiAccess) {
    UpdateSettings updateSettings = UpdateSettings.getInstance();
    if (!updateSettings.isEnable()) {
      return;
    }

    PluginsAdvertiserHolder.initialize(pluginDescriptors -> {
      UIUtil.invokeLaterIfNeeded(() -> {
        if (!project.isDisposed()) {
          EditorNotifications.getInstance(project).updateAllNotifications();
        }
      });

      if (project.isDisposed()) {
        return;
      }

      final UnknownFeaturesCollector collectorSuggester = UnknownFeaturesCollector.getInstance(project);
      final Set<UnknownExtension> unknownExtensions = collectorSuggester.getUnknownExtensions();
      if (unknownExtensions.isEmpty()) {
        return;
      }

      final Set<PluginDescriptor> ids = new HashSet<>();
      for (UnknownExtension feature : unknownExtensions) {
        final Set<PluginDescriptor> descriptors = findByFeature(pluginDescriptors, feature);
        //do not suggest to download disabled plugins
        final List<String> disabledPlugins = PluginManager.getDisabledPlugins();
        for (PluginDescriptor id : descriptors) {
          if (!disabledPlugins.contains(id.getPluginId().getIdString())) {
            ids.add(id);
          }
        }
      }

      if (ids.isEmpty()) {
        return;
      }

      Notification notification =
              ourGroup.createNotification("Features covered by non-installed plugins are detected.", NotificationType.INFORMATION);
      notification.addAction(new NotificationAction("Install plugins...") {
        @RequiredUIAccess
        @Override
        public void actionPerformed(@Nonnull AnActionEvent e, @Nonnull Notification notification) {
          notification.expire();

          new PluginsAdvertiserDialog(project, pluginDescriptors, new ArrayList<>(ids)).showAsync();
        }
      });
      notification.addAction(new NotificationAction("Ignore") {
        @RequiredUIAccess
        @Override
        public void actionPerformed(@Nonnull AnActionEvent e, @Nonnull Notification notification) {
          notification.expire();

          for (UnknownExtension feature : unknownExtensions) {
            collectorSuggester.ignoreFeature(feature);
          }
        }
      });
      notification.notify(project);
    });
  }

  @Nonnull
  public static Set<PluginDescriptor> findByFeature(List<PluginDescriptor> descriptors, UnknownExtension feature) {
    Set<PluginDescriptor> filter = new LinkedHashSet<>();
    for (PluginDescriptor descriptor : descriptors) {
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

  private static boolean isMyFeature(String extensionValue, UnknownExtension feature) {
    if (feature.getExtensionKey().equals(FileTypeFactory.FILE_TYPE_FACTORY_EP.getName())) {
      FileNameMatcher matcher = createMatcher(extensionValue);
      return matcher != null && matcher.acceptsCharSequence(feature.getValue());
    }
    else {
      return extensionValue.equals(feature.getValue());
    }
  }

  /**
   * for correct specification - see hub impl
   */
  @Nullable
  public static FileNameMatcher createMatcher(@Nonnull String extensionValue) {
    if (extensionValue.length() < 2) {
      return null;
    }

    List<String> values = StringUtil.split(extensionValue, "|");

    String id = values.get(0);
    if (id.length() != 1) {
      return null;
    }


    FileNameMatcherFactory factory = FileNameMatcherFactory.getInstance();
    String value = values.get(1);

    char idChar = id.charAt(0);

    switch (idChar) {
      case '?':
        return factory.createWildcardFileNameMatcher(value);
      case '*':
        return factory.createExtensionFileNameMatcher(value);
      case '!':
        return factory.createExactFileNameMatcher(value, true);
      case '¡':
        return factory.createExactFileNameMatcher(value, false);
      default:
        return null;
    }
  }
}

