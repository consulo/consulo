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

import consulo.application.Application;
import consulo.application.eap.EarlyAccessProgramManager;
import consulo.application.plugin.DynamicPluginChange;
import consulo.application.plugin.DynamicPluginListener;
import consulo.application.plugin.PluginActionListener;
import consulo.application.util.concurrent.AppExecutorUtil;
import consulo.configurable.search.SearchableOptionsRegistrar;
import consulo.container.plugin.PluginDescriptor;
import consulo.container.plugin.PluginId;
import consulo.container.plugin.PluginIds;
import consulo.container.plugin.PluginManager;
import consulo.dataContext.UiDataProvider;
import consulo.disposer.Disposable;
import consulo.externalService.impl.internal.plugin.InstalledPluginsState;
import consulo.externalService.impl.internal.plugin.ui.PluginFilterHost;
import consulo.externalService.impl.internal.plugin.ui.PluginSorter;
import consulo.externalService.impl.internal.plugin.ui.PluginTagUtil;
import consulo.externalService.impl.internal.plugin.ui.PluginView;
import consulo.externalService.impl.internal.plugin.ui.action.PluginSortFilterGroup;
import consulo.externalService.impl.internal.plugin.ui.action.PluginsOptionGroup;
import consulo.externalService.impl.internal.repository.RepositoryHelper;
import consulo.externalService.update.UpdateSettings;
import consulo.localize.LocalizeValue;
import consulo.logging.Logger;
import consulo.platform.base.icon.PlatformIconGroup;
import consulo.ui.Component;
import consulo.ui.ImageBox;
import consulo.ui.ListBox;
import consulo.ui.Space;
import consulo.ui.TextBox;
import consulo.ui.TextBoxStyle;
import consulo.ui.UIAccess;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.action.ActionGroup;
import consulo.ui.ex.action.ActionToolbar;
import consulo.ui.ex.action.ActionToolbarFactory;
import consulo.ui.ex.action.AnSeparator;
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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

/**
 * The plugins page: the searchable list of installed and repository plugins (filter and options buttons in the search box),
 * the information of the selected plugin with its install/uninstall button and its details.
 *
 * @author VISTALL
 * @since 2026-11-14
 */
public class UnifiedPluginsPage implements Disposable, PluginFilterHost {
    private static final Logger LOG = Logger.getInstance(UnifiedPluginsPage.class);

    private final Application myApplication;
    private final DockLayout myContent;
    private final ListBox<PluginDescriptor> myList;
    private final TextBox mySearchBox;
    private final ActionToolbar myFilterToolbar;
    private final UnifiedPluginDescriptionPanel myDescriptionPanel;

    private List<PluginDescriptor> myRepositoryPlugins = List.of();
    /**
     * Repository entries by id: an installed plugin is listed by its installed descriptor, the filters also look at
     * what the repository knows of it, like the extensions it provides.
     */
    private Map<PluginId, PluginDescriptor> myRepositoryPluginsById = Map.of();
    private List<PluginDescriptor> myPlugins = List.of();
    private PluginSorter mySorter = PluginSorter.DEFAULT_SORTER;
    private PluginView myView = PluginView.ALL;

    @RequiredUIAccess
    public UnifiedPluginsPage(Application application) {
        myApplication = application;

        myContent = DockLayout.create(Space.NONE);
        myContent.putUserData(UiDataProvider.KEY, sink -> sink.set(PluginFilterHost.KEY, this));

        myList = ListBox.create(FlatDataModel.lazyOf(List.of()));
        myList.setRenderer(UnifiedPluginRowRenderer.reusable());
        myList.setItemHeightGetter(item -> UnifiedPluginRowRenderer.ROW_HEIGHT);

        myDescriptionPanel = new UnifiedPluginDescriptionPanel(application, this::select, this::setTagFilter, this::getPlugins);

        myList.addValueListener(event -> myDescriptionPanel.update(event.getValue(), myPlugins));

        myFilterToolbar = ActionToolbarFactory.getInstance().createActionToolbar(
            "UnifiedPluginsPageFilterToolbar",
            ActionGroup.newImmutableBuilder()
                .add(PluginSortFilterGroup.create(List.of(
                    PluginSorter.NAME,
                    PluginSorter.STATUS,
                    PluginSorter.RATING,
                    PluginSorter.DOWNLOADS,
                    PluginSorter.LAST_UPDATED
                )))
                .add(createOptionGroup())
                .build(),
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

        TwoComponentSplitLayout infoSplit = TwoComponentSplitLayout.create(SplitLayoutPosition.HORIZONTAL);
        infoSplit.setProportion(65);
        infoSplit.setFirstComponent(myDescriptionPanel.getInfoComponent());
        infoSplit.setSecondComponent(myDescriptionPanel.getDetailsComponent());

        TwoComponentSplitLayout split = TwoComponentSplitLayout.create(SplitLayoutPosition.HORIZONTAL);
        split.setProportion(30);
        split.setFirstComponent(listSide);
        split.setSecondComponent(infoSplit);

        myContent.center(split);

        myFilterToolbar.updateActionsAsync();

        UIAccess uiAccess = UIAccess.current();
        application.getMessageBus().connect(this).subscribe(DynamicPluginListener.class, new DynamicPluginListener() {
            @Override
            public void pluginsChanged(DynamicPluginChange change) {
                uiAccess.give(() -> rebuild());
            }
        });
        application.getMessageBus().connect(this).subscribe(PluginActionListener.class, new PluginActionListener() {
            @Override
            public void pluginsInstalled(PluginId[] pluginIds) {
                uiAccess.give(() -> rebuild());
            }

            @Override
            public void pluginsUninstalled(PluginId[] pluginIds) {
                uiAccess.give(() -> rebuild());
            }
        });

        reload();
    }

    private PluginsOptionGroup createOptionGroup() {
        PluginsOptionGroup group = new PluginsOptionGroup();
        group.add(new UnifiedInstallPluginFromDiskAction(this));
        group.add(AnSeparator.create());
        group.add(new UnifiedReloadAllAction(this));
        return group;
    }

    public Component getComponent() {
        return myContent;
    }

    public List<PluginDescriptor> getPlugins() {
        return myPlugins;
    }

    @Override
    public PluginView getView() {
        return myView;
    }

    @Override
    @RequiredUIAccess
    public void setView(PluginView view) {
        myView = view;
        applyFilter();
        ensureSelection();
    }

    /**
     * Rebuilds the list from the installed plugins and loads the repository again.
     */
    @RequiredUIAccess
    public void reload() {
        rebuild();

        mySearchBox.setPlaceholder(LocalizeValue.localizeTODO("Loading the repository..."));
        UIAccess uiAccess = UIAccess.current();
        CompletableFuture.supplyAsync(this::loadRepositoryPlugins, AppExecutorUtil.getAppExecutorService())
            .whenCompleteAsync((plugins, error) -> {
                mySearchBox.setPlaceholder(LocalizeValue.empty());
                if (plugins != null) {
                    myRepositoryPlugins = plugins;
                    Map<PluginId, PluginDescriptor> byId = new LinkedHashMap<>();
                    for (PluginDescriptor plugin : plugins) {
                        byId.put(plugin.getPluginId(), plugin);
                    }
                    myRepositoryPluginsById = byId;
                    rebuild();
                }
            }, uiAccess);
    }

    private List<PluginDescriptor> loadRepositoryPlugins() {
        try {
            return RepositoryHelper.loadOnlyPluginsFromRepository(
                null,
                UpdateSettings.getInstance().getChannel(),
                EarlyAccessProgramManager.getInstance(),
                myContent
            );
        }
        catch (Exception e) {
            LOG.warn(e);
            return List.of();
        }
    }

    /**
     * The installed plugins win over their repository entries: their state is what the page acts on, a newer version
     * shows as an update.
     */
    @RequiredUIAccess
    private void rebuild() {
        Map<PluginId, PluginDescriptor> plugins = new LinkedHashMap<>();
        for (PluginDescriptor descriptor : PluginManager.getPlugins()) {
            plugins.put(descriptor.getPluginId(), descriptor);
        }
        for (PluginDescriptor descriptor : InstalledPluginsState.getInstance().getAllPlugins()) {
            plugins.putIfAbsent(descriptor.getPluginId(), descriptor);
        }
        for (PluginDescriptor descriptor : myRepositoryPlugins) {
            plugins.putIfAbsent(descriptor.getPluginId(), descriptor);
        }
        plugins.keySet().removeIf(PluginIds::isPlatformPlugin);

        myPlugins = List.copyOf(plugins.values());
        applyFilter();
        ensureSelection();
    }

    /**
     * Searches all plugins, the text may hold {@link PluginSearchQuery} filters.
     */
    @RequiredUIAccess
    public void search(String query) {
        myView = PluginView.ALL;
        mySearchBox.setValue(query);
        applyFilter();
        ensureSelection();
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
        return PluginSearchQuery.parse(mySearchBox.getValue()).getTag();
    }

    /**
     * The tag goes into the search text as a {@code /tag:} filter.
     */
    @RequiredUIAccess
    @Override
    public void setTagFilter(@Nullable String tag) {
        mySearchBox.setValue(PluginSearchQuery.withTag(mySearchBox.getValue(), tag));
    }

    @RequiredUIAccess
    @SuppressWarnings("unchecked")
    private void applyFilter() {
        PluginSearchQuery query = PluginSearchQuery.parse(mySearchBox.getValue());
        String text = query.getText();
        String filter = text == null ? null : text.toLowerCase();
        Set<String> search = filter == null ? Set.of() : SearchableOptionsRegistrar.getInstance().getProcessedWords(filter);

        List<PluginDescriptor> filtered = new ArrayList<>();
        for (PluginDescriptor descriptor : myPlugins) {
            if (!myView.accepts(descriptor)) {
                continue;
            }
            if (filter != null && !isAccepted(filter, search, descriptor)) {
                continue;
            }
            PluginDescriptor repositoryDescriptor = myRepositoryPluginsById.get(descriptor.getPluginId());
            if (!query.acceptsFilters(descriptor) && (repositoryDescriptor == null || !query.acceptsFilters(repositoryDescriptor))) {
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
        else {
            // the list may keep the selected row without a value event, the details must not show a hidden plugin
            PluginDescriptor first = filtered.isEmpty() ? null : filtered.get(0);
            myList.setValue(first);
            myDescriptionPanel.update(first, myPlugins);
        }

        myFilterToolbar.updateActionsAsync();
    }

    @RequiredUIAccess
    private void ensureSelection() {
        if (myList.getValue() == null && myList.getDataModel().getSize() > 0) {
            myList.setValue(myList.getDataModel().get(0));
        }
    }

    /**
     * Selects the plugin, showing all plugins when the current view hides it.
     */
    @RequiredUIAccess
    public void select(PluginId pluginId) {
        PluginDescriptor descriptor = findPlugin(pluginId);
        if (descriptor == null) {
            // a plugin installed a moment ago may not be listed yet
            rebuild();
            descriptor = findPlugin(pluginId);
        }
        if (descriptor == null) {
            return;
        }
        if (!myView.accepts(descriptor)) {
            setView(PluginView.ALL);
        }
        myList.setValue(descriptor);
    }

    public @Nullable PluginDescriptor findPlugin(PluginId pluginId) {
        return ContainerUtil.find(myPlugins, descriptor -> descriptor.getPluginId().equals(pluginId));
    }

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
