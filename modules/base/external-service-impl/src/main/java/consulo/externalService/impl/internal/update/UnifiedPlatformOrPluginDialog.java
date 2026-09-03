/*
 * Copyright 2013-2026 consulo.io
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

import consulo.container.plugin.PluginDescriptor;
import consulo.container.plugin.PluginId;
import consulo.container.plugin.PluginManager;
import consulo.externalService.impl.internal.plugin.ui.PluginSorter;
import consulo.externalService.impl.internal.plugin.ui.unified.UnifiedPluginRowRender;
import consulo.externalService.internal.PlatformOrPluginUpdateResultType;
import consulo.externalService.localize.ExternalServiceLocalize;
import consulo.localize.LocalizeValue;
import consulo.platform.base.localize.CommonLocalize;
import consulo.project.Project;
import consulo.ui.Alerts;
import consulo.ui.Button;
import consulo.ui.Component;
import consulo.ui.Label;
import consulo.ui.ListBox;
import consulo.ui.RenderItem;
import consulo.ui.Size2D;
import consulo.ui.Space;
import consulo.ui.Window;
import consulo.ui.WindowOptions;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.layout.DockLayout;
import consulo.ui.layout.HorizontalLayout;
import consulo.ui.layout.ScrollableLayout;
import consulo.util.collection.ContainerUtil;
import consulo.util.collection.Lists;
import consulo.util.lang.StringUtil;
import consulo.virtualFileSystem.status.FileStatus;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Predicate;

/**
 * Unified counterpart of {@link PlatformOrPluginDialog} - lists what is about to be downloaded and hands the
 * confirmed set to {@link PlatformOrPluginInstallProcess}.
 *
 * @author VISTALL
 * @since 2026-08-13
 */
public class UnifiedPlatformOrPluginDialog {
    private final @Nullable Project myProject;
    private final List<PlatformOrPluginNode> myNodes;
    private final PlatformOrPluginUpdateResultType myType;
    private final @Nullable Consumer<Collection<PluginDescriptor>> myAfterCallback;
    private final Predicate<PluginId> myGreenStrategy;
    private final boolean myModalProgress;

    private @Nullable String myPlatformVersion;

    private final Window myWindow;

    @RequiredUIAccess
    public UnifiedPlatformOrPluginDialog(
        @Nullable Project project,
        PlatformOrPluginUpdateResult updateResult,
        @Nullable Predicate<PluginId> greenStrategy,
        @Nullable Consumer<Collection<PluginDescriptor>> afterCallback,
        boolean modalProgress
    ) {
        myProject = project;
        myAfterCallback = afterCallback;
        myModalProgress = modalProgress;
        myType = updateResult.getType();
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

        toShowPluginList.sort(Comparator.comparing(t -> PluginSorter.NAME.getValueGetter().apply(t)));

        Lists.weightSort(toShowPluginList, pluginDescriptor -> {
            if (PlatformOrPluginUpdateChecker.isPlatform(pluginDescriptor.getPluginId())) {
                return 100;
            }

            if (brokenPlugins.contains(pluginDescriptor.getPluginId())) {
                return 200;
            }

            return 0;
        });

        ListBox<PluginDescriptor> list = ListBox.create(toShowPluginList);
        list.setRender(this::renderRow);
        list.setItemHeightGetter(item -> UnifiedPluginRowRender.ROW_HEIGHT);

        Button okButton = Button.create(CommonLocalize.buttonOk(), event -> {
            close();
            onOk();
        });
        Button cancelButton = Button.create(CommonLocalize.buttonCancel(), event -> close());

        HorizontalLayout buttons = HorizontalLayout.create();
        buttons.add(okButton);
        buttons.add(cancelButton);

        DockLayout south = DockLayout.create(Space.NONE);
        south.left(Label.create(ExternalServiceLocalize.pluginFollowingWillBeDownloadedLabel()));
        south.right(buttons);
        south.paddingBuilder().allSet(Space.LARGE).apply();

        DockLayout root = DockLayout.create(Space.NONE);
        root.center(ScrollableLayout.create(list));
        root.bottom(south);

        myWindow = Window.create(
            (myType == PlatformOrPluginUpdateResultType.PLUGIN_INSTALL
                ? ExternalServiceLocalize.pluginInstallDialogTitle()
                : ExternalServiceLocalize.updateAvailableGroup()).get(),
            WindowOptions.builder().build()
        );
        myWindow.setSize(new Size2D(600, 300));
        myWindow.setContent(root);
    }

    @RequiredUIAccess
    private Component renderRow(RenderItem<PluginDescriptor> item) {
        PluginDescriptor descriptor = item.getValue();
        if (descriptor == null) {
            return UnifiedPluginRowRender.render(item);
        }

        PlatformOrPluginNode node = ContainerUtil.find(myNodes, it -> it.getPluginId().equals(descriptor.getPluginId()));
        assert node != null;

        FileStatus status = FileStatus.MODIFIED;
        if (myGreenStrategy.test(descriptor.getPluginId())) {
            status = FileStatus.ADDED;
        }
        if (node.getFutureDescriptor() == null) {
            status = FileStatus.UNKNOWN;
        }

        PluginDescriptor currentDescriptor = node.getCurrentDescriptor();
        String versions = currentDescriptor != null
            ? currentDescriptor.getVersion() + " \u2192 " + (node.getFutureDescriptor() == null ? "??" : descriptor.getVersion())
            : StringUtil.notNullize(descriptor.getVersion());

        return UnifiedPluginRowRender.render(item, status.getColor(), LocalizeValue.of(versions));
    }

    @RequiredUIAccess
    public void show() {
        myWindow.show();
    }

    @RequiredUIAccess
    private void close() {
        myWindow.close();
    }

    /**
     * Runs the confirmed set without showing the window - what an install with no extra dependencies asks for.
     */
    @RequiredUIAccess
    public void doOKAction() {
        onOk();
    }

    @RequiredUIAccess
    private void onOk() {
        PlatformOrPluginNode brokenPlugin = myNodes.stream()
            .filter(c -> c.getFutureDescriptor() == null)
            .findFirst()
            .orElse(null);

        if (brokenPlugin != null) {
            Alerts.okCancel()
                .asWarning()
                .text(ExternalServiceLocalize.messageIdeaFewPluginsWillBeNotUpdated())
                .showAsync()
                .whenComplete((confirmed, error) -> {
                    if (Boolean.TRUE.equals(confirmed)) {
                        runProcess();
                    }
                });
            return;
        }

        runProcess();
    }

    @RequiredUIAccess
    private void runProcess() {
        PlatformOrPluginInstallProcess.run(myProject, myNodes, myType, myPlatformVersion, myAfterCallback, myModalProgress);
    }
}
