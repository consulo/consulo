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
package com.intellij.ide.plugins.pluginsAdvertisement;

import com.intellij.ide.IdeBundle;
import com.intellij.ide.plugins.PluginManager;
import com.intellij.ide.plugins.PluginManagerCore;
import com.intellij.ide.plugins.PluginManagerMain;
import com.intellij.openapi.fileEditor.FileEditor;
import com.intellij.openapi.fileTypes.FileTypeFactory;
import com.intellij.openapi.fileTypes.PlainTextFileType;
import com.intellij.openapi.fileTypes.impl.AbstractFileType;
import com.intellij.openapi.project.DumbAware;
import com.intellij.openapi.updateSettings.impl.pluginsAdvertisement.UnknownExtension;
import com.intellij.openapi.updateSettings.impl.pluginsAdvertisement.UnknownFeaturesCollector;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.ui.EditorNotificationPanel;
import com.intellij.ui.EditorNotifications;
import consulo.annotation.access.RequiredReadAction;
import consulo.container.plugin.PluginDescriptor;
import consulo.container.plugin.PluginId;
import consulo.editor.notifications.EditorNotificationProvider;
import consulo.ide.plugins.pluginsAdvertisement.PluginsAdvertiserDialog;
import consulo.ide.plugins.pluginsAdvertisement.PluginsAdvertiserHolder;
import jakarta.inject.Inject;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
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
  private final EditorNotifications myNotifications;
  private final Set<String> myEnabledExtensions = new HashSet<>();
  private final UnknownFeaturesCollector myUnknownFeaturesCollector;

  @Inject
  public PluginAdvertiserEditorNotificationProvider(UnknownFeaturesCollector unknownFeaturesCollector, final EditorNotifications notifications) {
    myUnknownFeaturesCollector = unknownFeaturesCollector;
    myNotifications = notifications;
  }

  @RequiredReadAction
  @Nullable
  @Override
  public EditorNotificationPanel createNotificationPanel(@Nonnull VirtualFile file, @Nonnull FileEditor fileEditor) {
    if (file.getFileType() != PlainTextFileType.INSTANCE && !(file.getFileType() instanceof AbstractFileType)) return null;

    final String extension = file.getExtension();
    if (extension == null) {
      return null;
    }

    if (myEnabledExtensions.contains(extension) || isIgnoredFile(file)) return null;

    UnknownExtension fileFeatureForChecking = new UnknownExtension(FileTypeFactory.FILE_TYPE_FACTORY_EP.getName(), file.getName());

    List<PluginDescriptor> allPlugins = PluginsAdvertiserHolder.getLoadedPluginDescriptors();

    Set<PluginDescriptor> byFeature = PluginsAdvertiser.findByFeature(allPlugins, fileFeatureForChecking);
    if (!byFeature.isEmpty()) {
      return createPanel(file, byFeature, allPlugins);

    }
    return null;
  }

  @Nonnull
  private EditorNotificationPanel createPanel(VirtualFile virtualFile, Set<PluginDescriptor> plugins, List<PluginDescriptor> allPlugins) {
    String extension = virtualFile.getExtension();

    final EditorNotificationPanel panel = new EditorNotificationPanel();
    panel.setText(IdeBundle.message("plugin.advestiser.notification.text", plugins.size()));
    final PluginDescriptor disabledPlugin = getDisabledPlugin(plugins.stream().map(x -> x.getPluginId().getIdString()).collect(Collectors.toSet()));
    if (disabledPlugin != null) {
      panel.createActionLabel("Enable " + disabledPlugin.getName() + " plugin", () -> {
        myEnabledExtensions.add(extension);
        consulo.container.plugin.PluginManager.enablePlugin(disabledPlugin.getPluginId().getIdString());
        myNotifications.updateAllNotifications();
        PluginManagerMain.notifyPluginsWereUpdated("Plugin was successfully enabled", null);
      });
    }
    else {
      panel.createActionLabel(IdeBundle.message("plugin.advestiser.notification.install.link", plugins.size()), () -> {
        final PluginsAdvertiserDialog advertiserDialog = new PluginsAdvertiserDialog(null, allPlugins, new ArrayList<>(plugins));
        advertiserDialog.show();
        if (advertiserDialog.isUserInstalledPlugins()) {
          myEnabledExtensions.add(extension);
          myNotifications.updateAllNotifications();
        }
      });
    }

    panel.createActionLabel("Ignore by file name", () -> {
      myUnknownFeaturesCollector.ignoreFeature(new UnknownExtension(FileTypeFactory.FILE_TYPE_FACTORY_EP, virtualFile.getName()));
      myNotifications.updateAllNotifications();
    });

    panel.createActionLabel("Ignore by extension", () -> {
      myUnknownFeaturesCollector.ignoreFeature(new UnknownExtension(FileTypeFactory.FILE_TYPE_FACTORY_EP, "*." + virtualFile.getExtension()));
      myNotifications.updateAllNotifications();
    });

    return panel;
  }

  private boolean isIgnoredFile(@Nonnull VirtualFile virtualFile) {
    UnknownExtension extension = new UnknownExtension(FileTypeFactory.FILE_TYPE_FACTORY_EP, "*." + virtualFile.getExtension());

    if(myUnknownFeaturesCollector.isIgnored(extension)) {
      return true;
    }

    extension = new UnknownExtension(FileTypeFactory.FILE_TYPE_FACTORY_EP, virtualFile.getName());
    if(myUnknownFeaturesCollector.isIgnored(extension)) {
      return true;
    }

    return false;
  }

  @Nullable
  private static PluginDescriptor getDisabledPlugin(Set<String> plugins) {
    final List<String> disabledPlugins = new ArrayList<>(PluginManagerCore.getDisabledPlugins());
    disabledPlugins.retainAll(plugins);
    if (disabledPlugins.size() == 1) {
      return PluginManager.getPlugin(PluginId.getId(disabledPlugins.get(0)));
    }
    return null;
  }
}
