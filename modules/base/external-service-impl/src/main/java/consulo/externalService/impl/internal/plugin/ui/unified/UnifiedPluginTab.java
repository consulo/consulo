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

import consulo.configurable.SearchableOptionsRegistrar;
import consulo.container.plugin.PluginDescriptor;
import consulo.container.plugin.PluginId;
import consulo.dataContext.UiDataProvider;
import consulo.disposer.Disposable;
import consulo.externalService.impl.internal.plugin.ui.PluginFilterHost;
import consulo.externalService.impl.internal.plugin.ui.PluginSorter;
import consulo.externalService.impl.internal.plugin.ui.PluginTagUtil;
import consulo.externalService.impl.internal.plugin.ui.action.PluginSortFilterGroup;
import consulo.localize.LocalizeValue;
import consulo.platform.base.icon.PlatformIconGroup;
import consulo.ui.Component;
import consulo.ui.ImageBox;
import consulo.ui.ListBox;
import consulo.ui.Space;
import consulo.ui.TextBox;
import consulo.ui.TextBoxStyle;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.action.ActionGroup;
import consulo.ui.ex.action.ActionToolbar;
import consulo.ui.ex.action.ActionToolbarFactory;
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
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * @author VISTALL
 * @since 2026-08-13
 */
public abstract class UnifiedPluginTab implements Disposable, PluginFilterHost {
    private final DockLayout myContent;
    private final ListBox<PluginDescriptor> myList;
    private final TextBox mySearchBox;
    private final ActionToolbar myFilterToolbar;
    private final UnifiedPluginDescriptionPanel myDescriptionPanel;

    private List<PluginDescriptor> myPlugins = List.of();
    private PluginSorter mySorter = PluginSorter.DEFAULT_SORTER;
    private @Nullable String myTagFilter;

    @RequiredUIAccess
    protected UnifiedPluginTab(boolean installedTab) {
        myContent = DockLayout.create(Space.NONE);
        myContent.putUserData(UiDataProvider.KEY, sink -> sink.set(PluginFilterHost.KEY, this));

        myList = ListBox.create(FlatDataModel.lazyOf(List.of()));
        myList.setRenderer(UnifiedPluginRowRenderer.reusable());
        myList.setItemHeightGetter(item -> UnifiedPluginRowRenderer.ROW_HEIGHT);

        myDescriptionPanel = new UnifiedPluginDescriptionPanel(installedTab, this::select, this::getPlugins);

        myList.addValueListener(event -> myDescriptionPanel.update(event.getValue(), myPlugins));

        myFilterToolbar = ActionToolbarFactory.getInstance().createActionToolbar(
            getClass().getSimpleName() + "Toolbar",
            ActionGroup.newImmutableBuilder().add(PluginSortFilterGroup.create(getSorters())).build(),
            ActionToolbar.Style.INPLACE
        );
        myFilterToolbar.setTargetUIComponent(myContent);

        mySearchBox = TextBox.create("");
        mySearchBox.addStyle(TextBoxStyle.INPLACE);
        mySearchBox.setPrefixComponent(ImageBox.create(PlatformIconGroup.actionsSearch()));
        mySearchBox.setSuffixComponent(myFilterToolbar.getUIComponent());
        mySearchBox.addValueListener(event -> {
            applyFilter();
            ensureSelection();
        });

        DockLayout searchRow = DockLayout.create(Space.NONE);
        searchRow.center(mySearchBox);
        searchRow.borderBuilder().bottomSet().apply();

        // the awt and qt lists only scroll inside a scroll layout; the web list scrolls itself and fills the
        // layout, which then never overflows
        DockLayout listSide = DockLayout.create(Space.NONE);
        listSide.top(searchRow);
        listSide.center(ScrollableLayout.create(myList));

        TwoComponentSplitLayout split = TwoComponentSplitLayout.create(SplitLayoutPosition.HORIZONTAL);
        split.setProportion(50);
        split.setFirstComponent(listSide);
        split.setSecondComponent(myDescriptionPanel.getComponent());

        myContent.center(split);

        myFilterToolbar.updateActionsAsync();
    }

    public abstract List<PluginSorter> getSorters();

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
        myPlugins = List.copyOf(plugins);
        applyFilter();
    }

    @RequiredUIAccess
    public void filter(String text) {
        mySearchBox.setValue(text);
    }

    @Override
    public List<PluginDescriptor> getAllPlugins() {
        return myPlugins;
    }

    @Override
    public PluginSorter getSorter() {
        return mySorter;
    }

    @RequiredUIAccess
    @Override
    public void reSort(PluginSorter sorter) {
        mySorter = sorter;
        applyFilter();
    }

    @Override
    public @Nullable String getTagFilter() {
        return myTagFilter;
    }

    @RequiredUIAccess
    @Override
    public void setTagFilter(@Nullable String tag) {
        myTagFilter = tag;
        applyFilter();
    }

    @RequiredUIAccess
    @SuppressWarnings("unchecked")
    private void applyFilter() {
        String text = StringUtil.nullize(mySearchBox.getValue(), true);
        String filter = text == null ? null : text.toLowerCase();
        Set<String> search = filter == null ? Set.of() : SearchableOptionsRegistrar.getInstance().getProcessedWords(filter);
        String tag = myTagFilter;

        List<PluginDescriptor> filtered = new ArrayList<>();
        for (PluginDescriptor descriptor : myPlugins) {
            if (filter != null && !isAccepted(filter, search, descriptor)) {
                continue;
            }
            if (tag != null && !descriptor.getTags().contains(tag)) {
                continue;
            }
            filtered.add(descriptor);
        }

        PluginSorter sorter = mySorter;
        if (sorter != PluginSorter.AS_IS) {
            filtered.sort(Comparator.comparing(descriptor -> sorter.getValueGetter().apply(descriptor)));
        }

        PluginDescriptor selected = myList.getValue();

        ((MutableFlatDataModel<PluginDescriptor>) myList.getDataModel()).replaceAll(filtered);

        if (selected != null && filtered.contains(selected)) {
            myList.setValue(selected);
        }

        myFilterToolbar.updateActionsAsync();
    }

    @RequiredUIAccess
    protected void ensureSelection() {
        if (myList.getValue() == null && myList.getDataModel().getSize() > 0) {
            myList.setValue(myList.getDataModel().get(0));
        }
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

    private static boolean isAccepted(String filter, Set<String> search, PluginDescriptor descriptor) {
        if (isAccepted(search, filter, descriptor.getName())) {
            return true;
        }

        String description = descriptor.getDescription();
        if (description != null && isAccepted(search, filter, description)) {
            return true;
        }

        for (LocalizeValue tag : PluginTagUtil.getLocalizedTags(descriptor)) {
            if (isAccepted(search, filter, tag.get())) {
                return true;
            }
        }

        String changeNotes = descriptor.getChangeNotes();
        return changeNotes != null && isAccepted(search, filter, changeNotes);
    }

    private static boolean isAccepted(Set<String> search, String filter, String text) {
        if (StringUtil.containsIgnoreCase(text, filter)) {
            return true;
        }

        Set<String> words = new HashSet<>(search);
        words.removeAll(SearchableOptionsRegistrar.getInstance().getProcessedWords(text));
        return words.isEmpty();
    }
}
