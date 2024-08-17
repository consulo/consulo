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

import consulo.annotation.access.RequiredReadAction;
import consulo.annotation.component.ExtensionImpl;
import consulo.application.dumb.DumbAware;
import consulo.component.extension.preview.ExtensionPreview;
import consulo.container.plugin.PluginDescriptor;
import consulo.container.plugin.PluginId;
import consulo.container.plugin.PluginManager;
import consulo.fileEditor.EditorNotificationBuilder;
import consulo.fileEditor.EditorNotificationProvider;
import consulo.fileEditor.EditorNotifications;
import consulo.fileEditor.FileEditor;
import consulo.ide.IdeBundle;
import consulo.ide.impl.idea.ide.plugins.PluginManagerMain;
import consulo.ide.impl.plugins.pluginsAdvertisement.PluginsAdvertiserDialog;
import consulo.ide.impl.plugins.pluginsAdvertisement.PluginsAdvertiserHolder;
import consulo.language.plain.PlainTextFileType;
import consulo.localize.LocalizeValue;
import consulo.project.UnknownFeaturesCollector;
import consulo.virtualFileSystem.VirtualFile;
import consulo.virtualFileSystem.fileType.FileType;
import consulo.virtualFileSystem.fileType.FileTypeFactory;
import consulo.virtualFileSystem.fileType.UnknownFileType;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import jakarta.inject.Inject;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Supplier;
import java.util.stream.Collectors;

/**
 * User: anna
 * Date: 10/11/13
 */
@ExtensionImpl
public class PluginAdvertiserEditorNotificationProvider implements EditorNotificationProvider, DumbAware {
  private final EditorNotifications myNotifications;
  private final Set<String> myEnabledExtensions = new HashSet<>();
  private final UnknownFeaturesCollector myUnknownFeaturesCollector;

  @Inject
  public PluginAdvertiserEditorNotificationProvider(UnknownFeaturesCollector unknownFeaturesCollector, final EditorNotifications notifications) {
    myUnknownFeaturesCollector = unknownFeaturesCollector;
    myNotifications = notifications;
  }

  @Nonnull
  @Override
  public String getId() {
    return "plugin-advertiser";
  }

  @RequiredReadAction
  @Nullable
  @Override
  public EditorNotificationBuilder buildNotification(@Nonnull VirtualFile file, @Nonnull FileEditor fileEditor, @Nonnull Supplier<EditorNotificationBuilder> builderFactory) {
    if (!isValidFile(file)) {
      return null;
    }

    final String extension = file.getExtension();
    if (extension == null) {
      return null;
    }

    if (myEnabledExtensions.contains(extension) || isIgnoredFile(file)) return null;

    ExtensionPreview fileFeatureForChecking = ExtensionPreview.of(FileTypeFactory.class, file.getName());

    List<PluginDescriptor> allPlugins = PluginsAdvertiserHolder.getLoadedPluginDescriptors();

    Set<PluginDescriptor> byFeature = PluginsAdvertiser.findImpl(allPlugins, fileFeatureForChecking);
    if (!byFeature.isEmpty()) {
      return build(file, byFeature, allPlugins, builderFactory.get());

    }
    return null;
  }

  private static boolean isValidFile(VirtualFile file) {
    FileType fileType = file.getFileType();

    // for all unknown files - we suggestion
    if (fileType == UnknownFileType.INSTANCE) {
      return true;
    }

    // also for plain - try search it
    return file.getFileType() == PlainTextFileType.INSTANCE;
  }

  @RequiredReadAction
  @Nonnull
  private EditorNotificationBuilder build(VirtualFile virtualFile, Set<PluginDescriptor> plugins, List<PluginDescriptor> allPlugins, EditorNotificationBuilder builder) {
    String extension = virtualFile.getExtension();

    builder.withText(LocalizeValue.localizeTODO(IdeBundle.message("plugin.advestiser.notification.text", plugins.size())));
    final PluginDescriptor disabledPlugin = getDisabledPlugin(plugins.stream().map(PluginDescriptor::getPluginId).collect(Collectors.toSet()));
    if (disabledPlugin != null) {
      builder.withAction(LocalizeValue.localizeTODO("Enable " + disabledPlugin.getName() + " plugin"), (i) -> {
        myEnabledExtensions.add(extension);
        PluginManager.enablePlugin(disabledPlugin.getPluginId());
        myNotifications.updateAllNotifications();
        PluginManagerMain.notifyPluginsWereUpdated("Plugin was successfully enabled", null);
      });
    }
    else {
      builder.withAction(LocalizeValue.localizeTODO(IdeBundle.message("plugin.advestiser.notification.install.link", plugins.size())), (i) -> {
        final PluginsAdvertiserDialog advertiserDialog = new PluginsAdvertiserDialog(null, allPlugins, new ArrayList<>(plugins));
        advertiserDialog.show();
        if (advertiserDialog.isUserInstalledPlugins()) {
          myEnabledExtensions.add(extension);
          myNotifications.updateAllNotifications();
        }
      });
    }

    builder.withAction(LocalizeValue.localizeTODO("Ignore by file name"), (i) -> {
      myUnknownFeaturesCollector.ignoreFeature(ExtensionPreview.of(FileTypeFactory.class, virtualFile.getName()));
      myNotifications.updateAllNotifications();
    });

    builder.withAction(LocalizeValue.localizeTODO("Ignore by extension"), (i) -> {
      myUnknownFeaturesCollector.ignoreFeature(ExtensionPreview.of(FileTypeFactory.class, "*." + virtualFile.getExtension()));
      myNotifications.updateAllNotifications();
    });

    return builder;
  }

  private boolean isIgnoredFile(@Nonnull VirtualFile virtualFile) {
    ExtensionPreview extension = ExtensionPreview.of(FileTypeFactory.class, "*." + virtualFile.getExtension());

    if(myUnknownFeaturesCollector.isIgnored(extension)) {
      return true;
    }

    extension = ExtensionPreview.of(FileTypeFactory.class, virtualFile.getName());
    if(myUnknownFeaturesCollector.isIgnored(extension)) {
      return true;
    }

    return false;
  }

  @Nullable
  private static PluginDescriptor getDisabledPlugin(Set<PluginId> plugins) {
    final List<PluginId> disabledPlugins = new ArrayList<>(PluginManager.getDisabledPlugins());
    disabledPlugins.retainAll(plugins);
    if (disabledPlugins.size() == 1) {
      return PluginManager.findPlugin(disabledPlugins.get(0));
    }
    return null;
  }
}
