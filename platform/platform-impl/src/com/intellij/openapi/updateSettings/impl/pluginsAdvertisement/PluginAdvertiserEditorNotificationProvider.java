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
import com.intellij.ide.plugins.PluginManager;
import com.intellij.ide.plugins.PluginManagerCore;
import com.intellij.ide.plugins.PluginManagerMain;
import com.intellij.openapi.extensions.PluginId;
import com.intellij.openapi.fileEditor.FileEditor;
import com.intellij.openapi.fileTypes.FileTypeFactory;
import com.intellij.openapi.fileTypes.PlainTextFileType;
import com.intellij.openapi.fileTypes.impl.AbstractFileType;
import com.intellij.openapi.project.DumbAware;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Couple;
import com.intellij.openapi.util.Key;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.ui.EditorNotificationPanel;
import com.intellij.ui.EditorNotifications;
import consulo.annotations.RequiredReadAction;
import consulo.editor.notifications.EditorNotificationProvider;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

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
  public EditorNotificationPanel createNotificationPanel(@NotNull VirtualFile file, @NotNull FileEditor fileEditor) {
    if (file.getFileType() != PlainTextFileType.INSTANCE && !(file.getFileType() instanceof AbstractFileType)) return null;

    final String extension = file.getExtension();
    if (extension == null) {
      return null;
    }

    if (myEnabledExtensions.contains(extension) || UnknownFeaturesCollector.getInstance(myProject).isIgnored(createFileFeatureForIgnoring(file))) return null;

    UnknownExtension fileFeatureForChecking = new UnknownExtension(FileTypeFactory.FILE_TYPE_FACTORY_EP.getName(), file.getName());

    List<IdeaPluginDescriptor> allPlugins = PluginsAdvertiser.getLoadedPluginDescriptors();

    Set<IdeaPluginDescriptor> byFeature = PluginsAdvertiser.findByFeature(allPlugins, fileFeatureForChecking);
    if (!byFeature.isEmpty()) {
      return createPanel(file, byFeature, allPlugins);

    }
    return null;
  }

  @NotNull
  private EditorNotificationPanel createPanel(VirtualFile virtualFile, final Set<IdeaPluginDescriptor> plugins, List<IdeaPluginDescriptor> allPlugins) {
    String extension = virtualFile.getExtension();

    final EditorNotificationPanel panel = new EditorNotificationPanel();
    panel.setText("Plugins supporting *." + extension + " are found");
    final IdeaPluginDescriptor disabledPlugin = getDisabledPlugin(plugins.stream().map(x -> x.getPluginId().getIdString()).collect(Collectors.toSet()));
    if (disabledPlugin != null) {
      panel.createActionLabel("Enable " + disabledPlugin.getName() + " plugin", () -> {
        myEnabledExtensions.add(extension);
        PluginManagerCore.enablePlugin(disabledPlugin.getPluginId().getIdString());
        myNotifications.updateAllNotifications();
        PluginManagerMain.notifyPluginsWereUpdated("Plugin was successfully enabled", null);
      });
    }
    else {
      panel.createActionLabel("Install plugins", () -> {
        final PluginsAdvertiserDialog advertiserDialog =
                new PluginsAdvertiserDialog(null, plugins.stream().map(x -> Couple.of(x, x)).collect(Collectors.toList()), allPlugins);
        advertiserDialog.show();
        if (advertiserDialog.isOK()) {
          myEnabledExtensions.add(extension);
          myNotifications.updateAllNotifications();
        }
      });
    }

    panel.createActionLabel("Ignore extension", () -> {
      final UnknownFeaturesCollector collectorSuggester = UnknownFeaturesCollector.getInstance(myProject);
      collectorSuggester.ignoreFeature(createFileFeatureForIgnoring(virtualFile));
      myNotifications.updateAllNotifications();
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

  @NotNull
  private static UnknownExtension createFileFeatureForIgnoring(VirtualFile virtualFile) {
    return new UnknownExtension(FileTypeFactory.FILE_TYPE_FACTORY_EP.getName(), "*." + virtualFile.getExtension());
  }
}
