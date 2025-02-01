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
package consulo.externalService.impl.internal.pluginAdvertiser;

import consulo.annotation.access.RequiredReadAction;
import consulo.annotation.component.ExtensionImpl;
import consulo.application.Application;
import consulo.application.dumb.DumbAware;
import consulo.component.extension.preview.ExtensionPreview;
import consulo.container.plugin.PluginDescriptor;
import consulo.externalService.impl.internal.plugin.PluginInstallUtil;
import consulo.externalService.impl.internal.plugin.ui.action.InstallPluginAction;
import consulo.externalService.localize.ExternalServiceLocalize;
import consulo.fileEditor.EditorNotificationBuilder;
import consulo.fileEditor.EditorNotificationProvider;
import consulo.fileEditor.EditorNotifications;
import consulo.fileEditor.FileEditor;
import consulo.localize.LocalizeValue;
import consulo.project.Project;
import consulo.project.internal.UnknownFeaturesCollector;
import consulo.ui.ex.awt.Messages;
import consulo.util.collection.ContainerUtil;
import consulo.virtualFileSystem.VirtualFile;
import consulo.virtualFileSystem.fileType.FileType;
import consulo.virtualFileSystem.fileType.FileTypeFactory;
import consulo.virtualFileSystem.fileType.PlainTextLikeFileType;
import consulo.virtualFileSystem.fileType.UnknownFileType;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import jakarta.inject.Inject;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Supplier;

/**
 * User: anna
 * Date: 10/11/13
 */
@ExtensionImpl
public class PluginAdvertiserEditorNotificationProvider implements EditorNotificationProvider, DumbAware {
    private final EditorNotifications myNotifications;
    private final Set<String> myEnabledExtensions = new HashSet<>();
    private final Project myProject;
    private final UnknownFeaturesCollector myUnknownFeaturesCollector;
    private final PluginAdvertiserRequester myPluginAdvertiserRequester;

    @Inject
    public PluginAdvertiserEditorNotificationProvider(Project project,
                                                      UnknownFeaturesCollector unknownFeaturesCollector,
                                                      EditorNotifications notifications,
                                                      PluginAdvertiserRequester pluginAdvertiserRequester) {
        myProject = project;
        myUnknownFeaturesCollector = unknownFeaturesCollector;
        myNotifications = notifications;
        myPluginAdvertiserRequester = pluginAdvertiserRequester;
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

        if (myEnabledExtensions.contains(extension) || isIgnoredFile(file)) {
            return null;
        }

        ExtensionPreview fileFeatureForChecking = ExtensionPreview.of(FileTypeFactory.class, file.getName());

        List<PluginDescriptor> allPlugins = myPluginAdvertiserRequester.getLoadedPluginDescriptors();

        Set<PluginDescriptor> byFeature = PluginAdvertiserImpl.findImpl(allPlugins, fileFeatureForChecking);
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
        return file.getFileType() instanceof PlainTextLikeFileType;
    }

    @RequiredReadAction
    @Nonnull
    private EditorNotificationBuilder build(VirtualFile virtualFile,
                                            Set<PluginDescriptor> plugins,
                                            List<PluginDescriptor> allPlugins,
                                            EditorNotificationBuilder builder) {
        String extension = virtualFile.getExtension();

        builder.withText(ExternalServiceLocalize.pluginAdvestiserNotificationText(plugins.size()));
        if (plugins.size() == 1) {
            PluginDescriptor item = ContainerUtil.getFirstItem(plugins);

            builder.withAction(ExternalServiceLocalize.pluginAdvestiserNotificationInstallPluginLink(item.getName()), (i) -> {
                InstallPluginAction.downloadAndInstallPlugins(myProject, List.of(item), allPlugins, p -> myProject.getUIAccess().give(() -> {
                    if (PluginInstallUtil.showRestartIDEADialog() == Messages.YES) {
                        Application.get().restart(true);
                    }
                }));
            });
        }
        else {
            builder.withAction(ExternalServiceLocalize.pluginAdvestiserNotificationInstallLink(plugins.size()), (i) -> {
                final PluginsAdvertiserDialog advertiserDialog = new PluginsAdvertiserDialog(null, allPlugins, new ArrayList<>(plugins));
                advertiserDialog.show();
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

        if (myUnknownFeaturesCollector.isIgnored(extension)) {
            return true;
        }

        extension = ExtensionPreview.of(FileTypeFactory.class, virtualFile.getName());
        if (myUnknownFeaturesCollector.isIgnored(extension)) {
            return true;
        }

        return false;
    }
}
