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
import consulo.application.Application;
import consulo.application.dumb.DumbAware;
import consulo.component.extension.ExtensionPoint;
import consulo.component.extension.preview.ExtensionPreview;
import consulo.component.extension.preview.ExtensionPreviewAcceptor;
import consulo.component.internal.PluginDescritorWithExtensionPreview;
import consulo.container.plugin.PluginDescriptor;
import consulo.container.plugin.PluginId;
import consulo.container.plugin.PluginManager;
import consulo.externalService.update.UpdateSettings;
import consulo.fileEditor.EditorNotifications;
import consulo.ide.impl.plugins.pluginsAdvertisement.PluginsAdvertiserDialog;
import consulo.ide.impl.plugins.pluginsAdvertisement.PluginsAdvertiserHolder;
import consulo.logging.Logger;
import consulo.project.Project;
import consulo.project.UnknownFeaturesCollector;
import consulo.project.startup.BackgroundStartupActivity;
import consulo.project.ui.notification.*;
import consulo.ui.UIAccess;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.action.AnActionEvent;
import consulo.ui.ex.awt.UIUtil;
import consulo.util.lang.ObjectUtil;
import jakarta.annotation.Nonnull;

import java.util.*;

@ExtensionImpl
public class PluginsAdvertiser implements BackgroundStartupActivity, DumbAware {
    private static final Logger LOG = Logger.getInstance(PluginsAdvertiser.class);

    public static NotificationGroup ourGroup = new NotificationGroup("Plugins Suggestion", NotificationDisplayType.STICKY_BALLOON, true);

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
            final Set<ExtensionPreview> unknownExtensions = collectorSuggester.getUnknownExtensions();
            if (unknownExtensions.isEmpty()) {
                return;
            }

            final Set<PluginDescriptor> ids = new HashSet<>();
            for (ExtensionPreview feature : unknownExtensions) {
                final Set<PluginDescriptor> descriptors = findImpl(pluginDescriptors, feature);
                //do not suggest to download disabled plugins
                final Set<PluginId> disabledPlugins = PluginManager.getDisabledPlugins();
                for (PluginDescriptor id : descriptors) {
                    if (!disabledPlugins.contains(id.getPluginId())) {
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

                    for (ExtensionPreview feature : unknownExtensions) {
                        collectorSuggester.ignoreFeature(feature);
                    }
                }
            });
            notification.notify(project);
        });
    }

    @Nonnull
    public static Set<PluginDescriptor> findImpl(List<PluginDescriptor> descriptors, ExtensionPreview feature) {
        ExtensionPreviewAcceptor<?> acceptor = findAcceptor(feature);

        Set<PluginDescriptor> filter = new LinkedHashSet<>();
        for (PluginDescriptor descriptor : descriptors) {
            if (!(descriptor instanceof PluginDescritorWithExtensionPreview pluginDescritorWithExtensionPreview)) {
                continue;
            }

            List<ExtensionPreview> extensionPreviews = pluginDescritorWithExtensionPreview.getExtensionPreviews();
            if (extensionPreviews.isEmpty()) {
                continue;
            }

            for (ExtensionPreview extensionPreview : extensionPreviews) {
                try {
                    if (acceptor.accept(extensionPreview, feature)) {
                        filter.add(descriptor);
                    }
                }
                catch (Exception e) {
                    LOG.error(e);
                }
            }
        }
        return filter;
    }

    @Nonnull
    private static ExtensionPreviewAcceptor<?> findAcceptor(ExtensionPreview feature) {
        ExtensionPoint<ExtensionPreviewAcceptor> extensionPoint = Application.get().getExtensionPoint(ExtensionPreviewAcceptor.class);

        ExtensionPreviewAcceptor acceptor = extensionPoint.findFirstSafe(extensionPreviewAcceptor -> {
            return Objects.equals(extensionPreviewAcceptor.getApiClass().getName(), feature.apiClassName());
        });
        return ObjectUtil.notNull(acceptor, ExtensionPreviewAcceptor.DEFAULT);
    }
}

