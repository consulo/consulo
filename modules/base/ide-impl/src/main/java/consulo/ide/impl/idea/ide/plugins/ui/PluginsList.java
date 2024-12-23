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
package consulo.ide.impl.idea.ide.plugins.ui;

import consulo.container.plugin.PluginDescriptor;
import consulo.container.plugin.PluginId;
import consulo.ide.impl.idea.ide.ui.search.SearchableOptionsRegistrar;
import consulo.ui.ex.awt.CollectionListModel;
import consulo.ui.ex.awt.JBList;
import consulo.ui.ex.awt.StatusText;
import consulo.ui.ex.awt.speedSearch.FilteringListModel;
import consulo.ui.ex.awt.speedSearch.ListSpeedSearch;
import consulo.util.lang.StringUtil;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.util.*;
import java.util.function.Predicate;

/**
 * @author VISTALL
 * @since 2024-12-23
 */
public class PluginsList {
    private final JBList<PluginDescriptor> myPluginList;

    private PluginSorter mySorter = PluginSorter.DEFAULT_SORTER;

    private String myTextFilter;
    private String myTag;

    private Map<String, Predicate<PluginDescriptor>> myFilters = new LinkedHashMap<>();

    public PluginsList(@Nullable PluginsPanel pluginsPanel) {
        myPluginList = new JBList<>();
        // just init empty model
        myPluginList.setModel(new FilteringListModel<>(new CollectionListModel<>(List.of())));
        myPluginList.setCellRenderer(createRender(pluginsPanel));

        new ListSpeedSearch(myPluginList, o -> ((PluginDescriptor) o).getName());
    }

    @Nonnull
    protected PluginsListRender createRender(PluginsPanel pluginsPanel) {
        return new PluginsListRender(pluginsPanel);
    }

    public StatusText getEmptyText() {
        return myPluginList.getEmptyText();
    }

    public JBList<PluginDescriptor> getComponent() {
        return myPluginList;
    }

    public List<PluginDescriptor> getAll() {
        FilteringListModel<PluginDescriptor> model = (FilteringListModel<PluginDescriptor>) myPluginList.getModel();
        CollectionListModel<PluginDescriptor> originalModel = (CollectionListModel<PluginDescriptor>) model.getOriginalModel();
        return originalModel.getItems();
    }

    public void modifyPluginsList(List<PluginDescriptor> list) {
        PluginDescriptor selected = myPluginList.getSelectedValue();
        FilteringListModel<PluginDescriptor> model = new FilteringListModel<>(new CollectionListModel<>(list));

        myPluginList.setModel(model);

        reSort();

        model.setFilter(pluginDescriptor -> {
            for (Predicate<PluginDescriptor> value : myFilters.values()) {
                if (!value.test(pluginDescriptor)) {
                    return false;
                }
            }

            return true;
        });

        if (selected != null) {
            select(selected.getPluginId());
        }
    }

    public PluginSorter getSorter() {
        return mySorter;
    }

    public String getTag() {
        return myTag;
    }

    @SuppressWarnings("unchecked")
    private void reSort() {
        FilteringListModel<PluginDescriptor> model = (FilteringListModel<PluginDescriptor>) myPluginList.getModel();
        CollectionListModel<PluginDescriptor> originalModel = (CollectionListModel<PluginDescriptor>) model.getOriginalModel();

        if (mySorter != PluginSorter.AS_IS) {
            originalModel.sort(Comparator.comparing(t -> mySorter.getValueGetter().apply(t)));
        }

        model.refilter();
    }

    public void setTextFilter(String textFilter) {
        myTextFilter = StringUtil.nullize(textFilter, true);

        if (myTextFilter != null) {
            final SearchableOptionsRegistrar optionsRegistrar = SearchableOptionsRegistrar.getInstance();
            final Set<String> search = optionsRegistrar.getProcessedWords(textFilter);

            myFilters.put("text", p -> PluginTab.isAccepted(myTextFilter, search, p));
        }
        else {
            myFilters.remove("text");
        }

        modifyPluginsList(getAll());
    }

    public void setTagFilter(String tag) {
        myTag = tag;

        if (tag != null) {
            myFilters.put("tag", pluginDescriptor -> pluginDescriptor.getTags().contains(myTag));
        }
        else {
            myFilters.remove("tag");
        }

        modifyPluginsList(getAll());
    }

    public void reSort(PluginSorter pluginSorter) {
        PluginDescriptor selected = myPluginList.getSelectedValue();

        mySorter = pluginSorter;

        reSort();

        if (selected != null) {
            select(selected.getPluginId());
        }
    }

    public void select(PluginId pluginId) {
        FilteringListModel<PluginDescriptor> model = (FilteringListModel<PluginDescriptor>) myPluginList.getModel();

        CollectionListModel<PluginDescriptor> originalModel = (CollectionListModel<PluginDescriptor>) model.getOriginalModel();

        originalModel.getItems().stream().filter(it -> Objects.equals(it.getPluginId(), pluginId)).findFirst().ifPresent(pl -> {
            myPluginList.setSelectedValue(pl, true);
        });
    }

    public void reset() {
        myTag = null;

        mySorter = PluginSorter.DEFAULT_SORTER;

        myFilters.clear();

        modifyPluginsList(getAll());
    }

    public void update() {
        modifyPluginsList(getAll());
    }
}
