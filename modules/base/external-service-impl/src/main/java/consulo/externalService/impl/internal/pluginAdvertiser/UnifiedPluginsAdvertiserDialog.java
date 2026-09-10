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
package consulo.externalService.impl.internal.pluginAdvertiser;

import consulo.container.plugin.PluginDescriptor;
import consulo.container.plugin.PluginId;
import consulo.externalService.impl.internal.plugin.ui.unified.UnifiedPluginDescriptionPanel;
import consulo.externalService.impl.internal.plugin.ui.unified.UnifiedPluginRowRender;
import consulo.externalService.pluginAdvertiser.PluginAdvertiserHelper;
import consulo.localize.LocalizeValue;
import consulo.ui.ListBox;
import consulo.ui.Size2D;
import consulo.ui.Space;
import consulo.ui.Window;
import consulo.ui.WindowOptions;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.layout.DockLayout;
import consulo.ui.layout.LoadingLayout;
import consulo.ui.layout.ScrollableLayout;
import consulo.ui.layout.SplitLayoutPosition;
import consulo.ui.layout.TwoComponentSplitLayout;
import consulo.util.collection.ContainerUtil;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

/**
 * Unified counterpart of the advertiser dialogs - the plugins suggested for what a project holds, each
 * installable from its description. The set is loaded through a {@link LoadingLayout}, so the dialog is the
 * one of a preview which is still being resolved as much as of a set already at hand.
 *
 * @author VISTALL
 * @since 2026-08-13
 */
public class UnifiedPluginsAdvertiserDialog {
    private final Supplier<PluginAdvertiserHelper.PluginsInfo> myLoader;

    private final Window myWindow;
    private final LoadingLayout<DockLayout> myLoadingLayout;

    private List<PluginDescriptor> myAllPlugins = List.of();
    private ListBox<PluginDescriptor> myList;

    @RequiredUIAccess
    public UnifiedPluginsAdvertiserDialog(Supplier<PluginAdvertiserHelper.PluginsInfo> loader) {
        myLoader = loader;

        myWindow = Window.create(LocalizeValue.localizeTODO("Choose Plugins to Install").get(), WindowOptions.builder().build());
        myWindow.setSize(new Size2D(600, 500));

        myLoadingLayout = LoadingLayout.create(DockLayout.create(Space.NONE), myWindow);
        myWindow.setContent(myLoadingLayout);
    }

    @RequiredUIAccess
    public void show() {
        myLoadingLayout.startLoading(myLoader, (layout, info) -> {
            myAllPlugins = info.allPlugins();

            layout.center(buildContent(new ArrayList<>(info.featurePlugins())));
        });

        myWindow.show();
    }

    @RequiredUIAccess
    private TwoComponentSplitLayout buildContent(List<PluginDescriptor> toInstallPlugins) {
        myList = ListBox.create(toInstallPlugins);
        myList.setRender(UnifiedPluginRowRender::render);
        myList.setItemHeightGetter(item -> UnifiedPluginRowRender.ROW_HEIGHT);

        UnifiedPluginDescriptionPanel descriptionPanel = new UnifiedPluginDescriptionPanel(false, this::select, () -> myAllPlugins);

        myList.addValueListener(event -> descriptionPanel.update(event.getValue(), myAllPlugins));

        TwoComponentSplitLayout split = TwoComponentSplitLayout.create(SplitLayoutPosition.HORIZONTAL);
        split.setProportion(50);
        split.setFirstComponent(ScrollableLayout.create(myList));
        split.setSecondComponent(descriptionPanel.getComponent());

        PluginDescriptor first = ContainerUtil.getFirstItem(toInstallPlugins);
        if (first != null) {
            myList.setValue(first);
        }

        return split;
    }

    @RequiredUIAccess
    private void select(PluginId pluginId) {
        PluginDescriptor descriptor = ContainerUtil.find(myAllPlugins, it -> it.getPluginId().equals(pluginId));
        if (descriptor != null) {
            myList.setValue(descriptor);
        }
    }
}
