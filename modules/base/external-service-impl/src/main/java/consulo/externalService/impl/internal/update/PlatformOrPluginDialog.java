/*
 * Copyright 2013-2016 consulo.io
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
package consulo.externalService.impl.internal.update;

import consulo.application.Application;
import consulo.application.internal.ApplicationInfo;
import consulo.application.plugin.PluginActionListener;
import consulo.application.progress.Task;
import consulo.container.plugin.PluginDescriptor;
import consulo.container.plugin.PluginId;
import consulo.container.plugin.PluginIds;
import consulo.container.plugin.PluginManager;
import consulo.externalService.impl.internal.plugin.InstalledPluginsState;
import consulo.externalService.impl.internal.plugin.PluginInstallUtil;
import consulo.externalService.impl.internal.plugin.PluginManagerUISettings;
import consulo.externalService.impl.internal.plugin.PluginNode;
import consulo.externalService.impl.internal.plugin.ui.PluginSorter;
import consulo.externalService.impl.internal.plugin.ui.PluginsList;
import consulo.externalService.impl.internal.plugin.ui.PluginsListRender;
import consulo.externalService.impl.internal.plugin.ui.PluginsPanel;
import consulo.externalService.impl.internal.pluginHistory.UpdateHistory;
import consulo.externalService.internal.PlatformOrPluginUpdateResultType;
import consulo.externalService.localize.ExternalServiceLocalize;
import consulo.localize.LocalizeValue;
import consulo.project.Project;
import consulo.ui.Alerts;
import consulo.ui.UIAccess;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.awt.*;
import consulo.ui.ex.awtUnsafe.TargetAWT;
import consulo.util.collection.ContainerUtil;
import consulo.util.collection.Lists;
import consulo.util.lang.StringUtil;
import consulo.virtualFileSystem.status.FileStatus;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import javax.swing.*;
import javax.swing.border.Border;
import java.awt.*;
import java.io.IOException;
import java.util.List;
import java.util.*;
import java.util.function.Consumer;
import java.util.function.Predicate;

/**
 * @author VISTALL
 * @since 07-Nov-16
 */
public class PlatformOrPluginDialog extends DialogWrapper {
    @Nonnull
    private JComponent myRoot;
    @Nonnull
    private List<PlatformOrPluginNode> myNodes;
    @Nullable
    private Project myProject;
    @Nullable
    private Consumer<Collection<PluginDescriptor>> myAfterCallback;
    @Nullable
    private String myPlatformVersion;
    @Nonnull
    private Predicate<PluginId> myGreenStrategy;
    @Nonnull
    private PlatformOrPluginUpdateResultType myType;

    public PlatformOrPluginDialog(
        @Nullable Project project,
        @Nonnull PlatformOrPluginUpdateResult updateResult,
        @Nullable Predicate<PluginId> greenStrategy,
        @Nullable Consumer<Collection<PluginDescriptor>> afterCallback
    ) {
        super(project);
        myProject = project;
        myAfterCallback = afterCallback;
        myType = updateResult.getType();
        setTitle(
            updateResult.getType() == PlatformOrPluginUpdateResultType.PLUGIN_INSTALL
                ? ExternalServiceLocalize.pluginInstallDialogTitle()
                : ExternalServiceLocalize.updateAvailableGroup()
        );

        myNodes = updateResult.getPlugins();

        if (greenStrategy != null) {
            myGreenStrategy = greenStrategy;
        }
        else {
            myGreenStrategy = pluginId -> {
                PluginDescriptor plugin = PluginManager.findPlugin(pluginId);
                boolean platform = PlatformOrPluginUpdateChecker.isPlatform(pluginId);
                return plugin == null && !platform;
            };
        }

        Set<PluginId> brokenPlugins = new HashSet<>();
        List<PluginDescriptor> toShowPluginList = new ArrayList<>();
        for (PlatformOrPluginNode node : myNodes) {
            PluginDescriptor futureDescriptor = node.getFutureDescriptor();
            if (futureDescriptor != null) {
                toShowPluginList.add(futureDescriptor);
            }
            else {
                brokenPlugins.add(node.getPluginId());

                toShowPluginList.add(node.getCurrentDescriptor());
            }

            if (PlatformOrPluginUpdateChecker.isPlatform(node.getPluginId())) {
                assert futureDescriptor != null;

                myPlatformVersion = futureDescriptor.getVersion();
            }
        }

        Lists.quickSort(toShowPluginList, (o1, o2) -> o1.getName().compareTo(o2.getName()));

        Lists.weightSort(toShowPluginList, pluginDescriptor -> {
            if (PlatformOrPluginUpdateChecker.isPlatform(pluginDescriptor.getPluginId())) {
                return 100;
            }

            if (brokenPlugins.contains(pluginDescriptor.getPluginId())) {
                return 200;
            }

            return 0;
        });

        PluginsList pluginsList = new PluginsList(null) {
            @Nonnull
            @Override
            protected PluginsListRender createRender(PluginsPanel pluginsPanel) {
                return new PluginsListRender(null) {
                    @Override
                    protected void updatePresentation(boolean isSelected, @Nonnull PluginDescriptor pluginNode) {
                        PlatformOrPluginNode node = ContainerUtil.find(myNodes, it -> it.getPluginId().equals(pluginNode.getPluginId()));
                        assert node != null;

                        PluginDescriptor currentDescriptor = node.getCurrentDescriptor();
                        if (currentDescriptor != null) {
                            myCategory.setText(
                                currentDescriptor.getVersion() + " " + UIUtil.rightArrow() + " " +
                                    (node.getFutureDescriptor() == null ? "??" : pluginNode.getVersion())
                            );
                        }
                        else {
                            myCategory.setText(pluginNode.getVersion());
                        }

                        FileStatus status = FileStatus.MODIFIED;
                        if (myGreenStrategy.test(pluginNode.getPluginId())) {
                            status = FileStatus.ADDED;
                        }
                        if (node.getFutureDescriptor() == null) {
                            status = FileStatus.UNKNOWN;
                        }

                        myDownloads.setVisible(false);

                        if (!isSelected) {
                            myName.setForeground(TargetAWT.to(status.getColor()));
                        }
                    }
                };
            }
        };

        // change default sort to as is
        pluginsList.reSort(PluginSorter.AS_IS);
        pluginsList.modifyPluginsList(toShowPluginList);

        myRoot = JBUI.Panels.simplePanel().addToCenter(ScrollPaneFactory.createScrollPane(pluginsList.getComponent(), true));
        setResizable(false);
        init();
    }

    @Override
    @RequiredUIAccess
    public void doOKAction() {
        super.doOKAction();

        PlatformOrPluginNode brokenPlugin = myNodes.stream()
            .filter(c -> c.getFutureDescriptor() == null)
            .findFirst()
            .orElse(null);
        if (brokenPlugin != null) {
            if (Messages.showOkCancelDialog(
                myProject,
                "Few plugins will be not updated. Those plugins will be disabled after update. Are you sure?",
                Application.get().getName().get(),
                Messages.getErrorIcon()
            ) != Messages.OK) {
                return;
            }
        }

        UIAccess uiAccess = UIAccess.current();

        Task.Backgroundable.queue(
            myProject,
            ExternalServiceLocalize.progressDownloadPlugins(),
            true,
            PluginManagerUISettings.getInstance(),
            indicator -> {
                List<PluginDescriptor> installed = new ArrayList<>(myNodes.size());

                int installCount = (int) myNodes
                    .stream()
                    .filter(it -> it.getFutureDescriptor() != null)
                    .count();

                List<PluginDownloader> forInstall = new ArrayList<>(myNodes.size());
                int i = 0;
                for (PlatformOrPluginNode platformOrPluginNode : myNodes) {
                    PluginDescriptor pluginDescriptor = platformOrPluginNode.getFutureDescriptor();
                    // update list contains broken plugins
                    if (pluginDescriptor == null) {
                        continue;
                    }

                    try {
                        PluginDownloader downloader = PluginDownloader.createDownloader(
                            pluginDescriptor,
                            myPlatformVersion,
                            myType != PlatformOrPluginUpdateResultType.PLUGIN_INSTALL
                        );

                        forInstall.add(downloader);

                        downloader.download(new CompositePluginInstallIndicator(indicator, i++, installCount));
                    }
                    catch (PluginDownloadFailedException e) {
                        uiAccess.give(() -> Alerts.okError(e.getLocalizeMessage()).showAsync());
                        return;
                    }
                }

                indicator.setTextValue(ExternalServiceLocalize.progressInstallingPlugins());

                Application application = Application.get();
                UpdateHistory updateHistory = application.getInstance(UpdateHistory.class);

                InstalledPluginsState installedPluginsState = InstalledPluginsState.getInstance();
                for (PluginDownloader downloader : forInstall) {
                    try {
                        // already was installed
                        if (installedPluginsState.wasUpdated(downloader.getPluginId())) {
                            continue;
                        }

                        installedPluginsState.getUpdatedPlugins().add(downloader.getPluginId());

                        downloader.install(indicator, true);

                        PluginDescriptor pluginDescriptor = downloader.getPluginDescriptor();

                        if (pluginDescriptor instanceof PluginNode) {
                            ((PluginNode) pluginDescriptor).setInstallStatus(PluginNode.STATUS_DOWNLOADED);

                            if (myType == PlatformOrPluginUpdateResultType.PLUGIN_INSTALL && pluginDescriptor.isExperimental()) {
                                updateHistory.setShowExperimentalWarning(true);
                            }
                        }

                        installed.add(pluginDescriptor);
                    }
                    catch (IOException e) {
                        uiAccess.give(() -> Alerts.okError(LocalizeValue.of(e.getLocalizedMessage())).showAsync());
                        return;
                    }
                }

                application.getMessageBus().syncPublisher(PluginActionListener.class).pluginsInstalled(
                    installed.stream()
                        .filter(it -> it instanceof PluginNode)
                        .map(PluginDescriptor::getPluginId)
                        .toArray(PluginId[]::new)
                );

                Map<String, String> pluginHistory = new HashMap<>();
                for (PluginDescriptor descriptor : PluginManager.getPlugins()) {
                    if (PluginIds.isPlatformPlugin(descriptor.getPluginId())) {
                        continue;
                    }

                    pluginHistory.put(descriptor.getPluginId().getIdString(), StringUtil.notNullize(descriptor.getVersion()));
                }

                pluginHistory.put(
                    PlatformOrPluginUpdateChecker.getPlatformPluginId().getIdString(),
                    ApplicationInfo.getInstance().getBuild().toString()
                );

                updateHistory.replaceHistory(pluginHistory);

                if (myAfterCallback != null) {
                    myAfterCallback.accept(installed);
                }

                if (myType != PlatformOrPluginUpdateResultType.PLUGIN_INSTALL) {
                    SwingUtilities.invokeLater(() -> {
                        UpdateSettingsImpl updateSettings = UpdateSettingsImpl.getInstance();
                        updateSettings.setLastCheckResult(PlatformOrPluginUpdateResultType.RESTART_REQUIRED);

                        if (PluginInstallUtil.showRestartIDEADialog() == Messages.YES) {
                            Application.get().restart(true);
                        }
                    });
                }
            }
        );
    }

    @Nullable
    @Override
    protected String getDimensionServiceKey() {
        setScalableSize(600, 300);
        return getClass().getSimpleName();
    }

    @Nullable
    @Override
    protected Border createContentPaneBorder() {
        return null;
    }

    @Nullable
    @Override
    protected JComponent createSouthPanel() {
        JComponent southPanel = super.createSouthPanel();
        if (southPanel != null) {
            southPanel.add(new JBLabel("Following nodes will be downloaded & installed"), BorderLayout.WEST);
            southPanel.setBorder(JBUI.Borders.empty(DialogWrapper.ourDefaultBorderInsets));

            BorderLayoutPanel borderLayoutPanel = JBUI.Panels.simplePanel(southPanel);
            borderLayoutPanel.setBorder(new CustomLineBorder(JBUI.scale(1), 0, 0, 0));
            return borderLayoutPanel;
        }
        return null;
    }

    @Nullable
    @Override
    protected JComponent createCenterPanel() {
        return myRoot;
    }
}
