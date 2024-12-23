/*
 * Copyright 2013-2024 consulo.io
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

import consulo.annotation.component.ServiceImpl;
import consulo.application.Application;
import consulo.application.concurrent.ApplicationConcurrency;
import consulo.component.extension.ExtensionPoint;
import consulo.component.extension.preview.ExtensionPreview;
import consulo.component.extension.preview.ExtensionPreviewAcceptor;
import consulo.component.internal.PluginDescritorWithExtensionPreview;
import consulo.container.plugin.PluginDescriptor;
import consulo.fileEditor.EditorNotifications;
import consulo.ide.impl.plugins.pluginsAdvertisement.PluginsAdvertiserDialog;
import consulo.logging.Logger;
import consulo.project.PluginAdvertiser;
import consulo.project.PluginAdvertiserExtension;
import consulo.project.Project;
import consulo.project.internal.UnknownFeaturesCollector;
import consulo.project.ui.notification.*;
import consulo.ui.UIAccess;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.action.AnActionEvent;
import consulo.util.lang.ObjectUtil;
import jakarta.annotation.Nonnull;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;

/**
 * @author VISTALL
 * @since 2024-08-17
 */
@ServiceImpl
@Singleton
public class PluginAdvertiserImpl implements PluginAdvertiser {
    private static final Logger LOG = Logger.getInstance(PluginAdvertiserImpl.class);

    public static final NotificationGroup ourGroup = new NotificationGroup("Plugins Suggestion", NotificationDisplayType.STICKY_BALLOON, true);

    private final ApplicationConcurrency myApplicationConcurrency;
    private final Project myProject;
    private final PluginAdvertiserRequester myPluginAdvertiserRequester;

    private UUID myTaskUUID;
    private Future<?> myTaskFuture = CompletableFuture.completedFuture(null);

    @Inject
    public PluginAdvertiserImpl(ApplicationConcurrency applicationConcurrency,
                                Project project,
                                PluginAdvertiserRequester pluginAdvertiserRequester) {
        myApplicationConcurrency = applicationConcurrency;
        myProject = project;
        myPluginAdvertiserRequester = pluginAdvertiserRequester;
    }

    @Override
    public void scheduleSuggestion() {
        UIAccess.assetIsNotUIThread();

        myTaskFuture.cancel(false);
        myTaskUUID = UUID.randomUUID();

        myTaskFuture = myPluginAdvertiserRequester.doRequest().whenCompleteAsync((pluginDescriptors, throwable) -> {
            checkAndNotify(pluginDescriptors);
        }, myApplicationConcurrency.getExecutorService());
    }

    private void checkAndNotify(List<PluginDescriptor> allDescriptors) {
        if (allDescriptors == null || allDescriptors.isEmpty()) {
            return;
        }

        if (myProject.isDisposed()) {
            return;
        }

        UIAccess uiAccess = myProject.getUIAccess();

        uiAccess.give(() -> {
            if (!myProject.isDisposed()) {
                EditorNotifications.getInstance(myProject).updateAllNotifications();
            }
        });

        final UnknownFeaturesCollector collectorSuggester = UnknownFeaturesCollector.getInstance(myProject);

        List<ExtensionPreview> previews = new ArrayList<>();

        final Set<PluginDescriptor> ids = new HashSet<>();
        myProject.getExtensionPoint(PluginAdvertiserExtension.class).forEach(extender -> {
            extender.extend(feature -> {
                previews.add(feature);

                final Set<PluginDescriptor> descriptors = findImpl(allDescriptors, feature);

                for (PluginDescriptor id : descriptors) {
                    ids.add(id);
                }
            });
        });

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

                new PluginsAdvertiserDialog(myProject, allDescriptors, new ArrayList<>(ids)).showAsync();
            }
        });
        notification.addAction(new NotificationAction("Ignore") {
            @RequiredUIAccess
            @Override
            public void actionPerformed(@Nonnull AnActionEvent e, @Nonnull Notification notification) {
                notification.expire();

                for (ExtensionPreview feature : previews) {
                    collectorSuggester.ignoreFeature(feature);
                }
            }
        });
        notification.notify(myProject);
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
