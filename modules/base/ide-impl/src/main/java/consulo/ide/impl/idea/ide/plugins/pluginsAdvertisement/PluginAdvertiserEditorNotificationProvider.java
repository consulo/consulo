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
import consulo.container.plugin.PluginDescriptor;
import consulo.container.plugin.PluginId;
import consulo.fileEditor.EditorNotificationBuilder;
import consulo.fileEditor.EditorNotificationProvider;
import consulo.fileEditor.EditorNotifications;
import consulo.fileEditor.FileEditor;
import consulo.ide.IdeBundle;
import consulo.ide.impl.idea.ide.plugins.PluginManager;
import consulo.ide.impl.idea.ide.plugins.PluginManagerCore;
import consulo.ide.impl.idea.ide.plugins.PluginManagerMain;
import consulo.ide.impl.idea.openapi.fileTypes.impl.AbstractFileType;
import consulo.ide.impl.plugins.pluginsAdvertisement.PluginsAdvertiserDialog;
import consulo.ide.impl.plugins.pluginsAdvertisement.PluginsAdvertiserHolder;
import consulo.language.plain.PlainTextFileType;
import consulo.localize.LocalizeValue;
import consulo.project.UnknownExtension;
import consulo.project.UnknownFeaturesCollector;
import consulo.virtualFileSystem.VirtualFile;
import consulo.virtualFileSystem.fileType.FileTypeFactory;
import jakarta.inject.Inject;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.*;
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
      return build(file, byFeature, allPlugins, builderFactory.get());

    }
    return null;
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
        consulo.container.plugin.PluginManager.enablePlugin(disabledPlugin.getPluginId());
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
      myUnknownFeaturesCollector.ignoreFeature(new UnknownExtension(FileTypeFactory.FILE_TYPE_FACTORY_EP, virtualFile.getName()));
      myNotifications.updateAllNotifications();
    });

    builder.withAction(LocalizeValue.localizeTODO("Ignore by extension"), (i) -> {
      myUnknownFeaturesCollector.ignoreFeature(new UnknownExtension(FileTypeFactory.FILE_TYPE_FACTORY_EP, "*." + virtualFile.getExtension()));
      myNotifications.updateAllNotifications();
    });

    return builder;
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
  private static PluginDescriptor getDisabledPlugin(Set<PluginId> plugins) {
    final List<PluginId> disabledPlugins = new ArrayList<>(PluginManagerCore.getDisabledPlugins());
    disabledPlugins.retainAll(plugins);
    if (disabledPlugins.size() == 1) {
      return PluginManager.getPlugin(disabledPlugins.get(0));
    }
    return null;
  }
}
