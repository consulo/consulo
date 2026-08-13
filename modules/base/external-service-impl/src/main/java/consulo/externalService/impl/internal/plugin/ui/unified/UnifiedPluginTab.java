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
package consulo.externalService.impl.internal.plugin.ui.unified;

import consulo.container.plugin.PluginDescriptor;
import consulo.container.plugin.PluginId;
import consulo.disposer.Disposable;
import consulo.ui.Component;
import consulo.ui.ListBox;
import consulo.ui.TextBox;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.layout.DockLayout;
import consulo.ui.layout.ScrollableLayout;
import consulo.ui.layout.SplitLayoutPosition;
import consulo.ui.layout.TwoComponentSplitLayout;
import consulo.ui.model.FlatDataModel;
import consulo.ui.model.MutableFlatDataModel;
import consulo.util.collection.ContainerUtil;
import consulo.util.lang.StringUtil;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

/**
 * @author VISTALL
 * @since 2026-08-13
 */
public abstract class UnifiedPluginTab implements Disposable {
    private final DockLayout myContent;
    private final ListBox<PluginDescriptor> myList;
    private final TextBox myFilterBox;
    private final UnifiedPluginDescriptionPanel myDescriptionPanel;

    private List<PluginDescriptor> myPlugins = List.of();

    @RequiredUIAccess
    protected UnifiedPluginTab(boolean installedTab) {
        myList = ListBox.create(FlatDataModel.lazyOf(List.of()));
        myList.setRender(UnifiedPluginRowRender::render);
        myList.setItemHeightGetter(item -> UnifiedPluginRowRender.ROW_HEIGHT);

        myDescriptionPanel = new UnifiedPluginDescriptionPanel(installedTab, this::select, this::getPlugins);

        myList.addValueListener(event -> myDescriptionPanel.update(event.getValue(), myPlugins));

        myFilterBox = TextBox.create("");
        myFilterBox.addValueListener(event -> applyFilter());

        // the awt and swt lists only scroll inside a scroll layout; the web list scrolls itself and fills the
        // layout, which then never overflows
        DockLayout listSide = DockLayout.create(0);
        listSide.top(myFilterBox);
        listSide.center(ScrollableLayout.create(myList));

        TwoComponentSplitLayout split = TwoComponentSplitLayout.create(SplitLayoutPosition.HORIZONTAL);
        split.setProportion(50);
        split.setFirstComponent(listSide);
        split.setSecondComponent(myDescriptionPanel.getComponent());

        myContent = DockLayout.create(0);
        myContent.center(split);
    }

    /**
     * The panel of the tab. A tab which loads its content asynchronously wraps this into a layout of its
     * own and puts it back when the loading ends.
     */
    public Component getContent() {
        return myContent;
    }

    public Component getComponent() {
        return myContent;
    }

    public List<PluginDescriptor> getPlugins() {
        return myPlugins;
    }

    @RequiredUIAccess
    public void setPlugins(List<PluginDescriptor> plugins) {
        List<PluginDescriptor> sorted = new ArrayList<>(plugins);
        sorted.sort(Comparator.comparing(descriptor -> StringUtil.notNullize(descriptor.getName()), String.CASE_INSENSITIVE_ORDER));

        myPlugins = sorted;
        applyFilter();
    }

    @RequiredUIAccess
    @SuppressWarnings("unchecked")
    private void applyFilter() {
        String filter = StringUtil.notNullize(myFilterBox.getValue()).trim().toLowerCase(Locale.ROOT);

        List<PluginDescriptor> filtered = filter.isEmpty()
            ? myPlugins
            : ContainerUtil.filter(
                myPlugins,
                descriptor -> StringUtil.notNullize(descriptor.getName()).toLowerCase(Locale.ROOT).contains(filter)
            );

        ((MutableFlatDataModel<PluginDescriptor>) myList.getDataModel()).replaceAll(filtered);
    }

    @RequiredUIAccess
    public void select(PluginId pluginId) {
        PluginDescriptor descriptor = findPlugin(pluginId);
        if (descriptor != null) {
            myList.setValue(descriptor);
        }
    }

    public @Nullable PluginDescriptor findPlugin(PluginId pluginId) {
        return ContainerUtil.find(myPlugins, descriptor -> descriptor.getPluginId().equals(pluginId));
    }

    @RequiredUIAccess
    public abstract void reload();

    @Override
    public void dispose() {
    }
}
