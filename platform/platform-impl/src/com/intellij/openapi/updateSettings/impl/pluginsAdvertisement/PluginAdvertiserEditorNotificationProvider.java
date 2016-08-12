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

import com.intellij.ide.plugins.*;
import com.intellij.openapi.extensions.PluginId;
import com.intellij.openapi.fileEditor.FileEditor;
import com.intellij.openapi.fileTypes.FileTypeFactory;
import com.intellij.openapi.fileTypes.PlainTextFileType;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.DumbAware;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Couple;
import com.intellij.openapi.util.Key;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.ui.EditorNotificationPanel;
import com.intellij.ui.EditorNotifications;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.mustbe.consulo.RequiredDispatchThread;
import org.mustbe.consulo.RequiredReadAction;
import consulo.editor.notifications.EditorNotificationProvider;

import java.util.*;

/**
 * User: anna
 * Date: 10/11/13
 */
public class PluginAdvertiserEditorNotificationProvider implements EditorNotificationProvider<EditorNotificationPanel>, DumbAware {
  private static final Key<EditorNotificationPanel> KEY = Key.create("file.type.associations.detected");
  private final Project myProject;
  private final EditorNotifications myNotifications;
  private final Set<String> myEnabledExtensions = new HashSet<String>();

  public PluginAdvertiserEditorNotificationProvider(Project project, final EditorNotifications notifications) {
    myProject = project;
    myNotifications = notifications;
  }

  @NotNull
  @Override
  public Key<EditorNotificationPanel> getKey() {
    return KEY;
  }

  @RequiredReadAction
  @Nullable
  @Override
  public EditorNotificationPanel createNotificationPanel(VirtualFile file, FileEditor fileEditor) {
    if (file.getFileType() != PlainTextFileType.INSTANCE) return null;

    final String extension = file.getExtension();
    if(extension == null) {
      return null;
    }
    if (myEnabledExtensions.contains(extension) ||
        UnknownFeaturesCollector.getInstance(myProject).isIgnored(createExtensionFeature(extension))) return null;

    final PluginsAdvertiser.KnownExtensions knownExtensions = PluginsAdvertiser.loadExtensions();
    if (knownExtensions != null) {
      final Set<String> plugins = knownExtensions.find(extension);
      if (plugins != null && !plugins.isEmpty()) {
        return createPanel(extension, plugins);
      }
    }
    return null;
  }

  @NotNull
  private EditorNotificationPanel createPanel(final String extension, final Set<String> plugins) {
    final EditorNotificationPanel panel = new EditorNotificationPanel();
    panel.setText("Plugins supporting *." + extension + " are found");
    final IdeaPluginDescriptor disabledPlugin = getDisabledPlugin(plugins);
    if (disabledPlugin != null) {
      panel.createActionLabel("Enable " + disabledPlugin.getName() + " plugin", new Runnable() {
        @Override
        public void run() {
          myEnabledExtensions.add(extension);
          PluginManagerCore.enablePlugin(disabledPlugin.getPluginId().getIdString());
          myNotifications.updateAllNotifications();
          PluginManagerMain.notifyPluginsWereUpdated("Plugin was successfully enabled", null);
        }
      });
    } else {
      panel.createActionLabel("Install plugins", new Runnable() {
        @Override
        public void run() {
          ProgressManager.getInstance().run(new Task.Modal(null, "Search for plugins in repository", true) {
            private final Set<Couple<IdeaPluginDescriptor>> myPlugins = new LinkedHashSet<Couple<IdeaPluginDescriptor>>();
            private List<IdeaPluginDescriptor> myAllPlugins;

            @Override
            public void run(@NotNull ProgressIndicator indicator) {
              try {
                myAllPlugins = RepositoryHelper.loadPluginsFromRepository(indicator);
                for (IdeaPluginDescriptor loadedPlugin : myAllPlugins) {
                  if (plugins.contains(loadedPlugin.getPluginId().getIdString())) {
                    myPlugins.add(Couple.of(null, loadedPlugin));
                  }
                }
              }
              catch (Exception ignore) {
              }
            }

            @RequiredDispatchThread
            @Override
            public void onSuccess() {
              final PluginsAdvertiserDialog advertiserDialog = new PluginsAdvertiserDialog(null, myPlugins, myAllPlugins);
              advertiserDialog.show();
              if (advertiserDialog.isOK()) {
                myEnabledExtensions.add(extension);
                myNotifications.updateAllNotifications();
              }
            }
          });
        }
      });
    }
    panel.createActionLabel("Ignore extension", new Runnable() {
      @Override
      public void run() {
        final UnknownFeaturesCollector collectorSuggester = UnknownFeaturesCollector.getInstance(myProject);
        collectorSuggester.ignoreFeature(createExtensionFeature(extension));
        myNotifications.updateAllNotifications();
      }
    });
    return panel;
  }

  @Nullable
  private static IdeaPluginDescriptor getDisabledPlugin(Set<String> plugins) {
    final List<String> disabledPlugins = new ArrayList<String>(PluginManagerCore.getDisabledPlugins());
    disabledPlugins.retainAll(plugins);
    if (disabledPlugins.size() == 1) {
      return PluginManager.getPlugin(PluginId.getId(disabledPlugins.get(0)));
    }
    return null;
  }

  private static UnknownFeature createExtensionFeature(String extension) {
    return new UnknownFeature(FileTypeFactory.FILE_TYPE_FACTORY_EP.getName(), extension);
  }
}
