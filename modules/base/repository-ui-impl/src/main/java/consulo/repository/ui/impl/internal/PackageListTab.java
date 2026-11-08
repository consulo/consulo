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
package consulo.repository.ui.impl.internal;

import consulo.disposer.Disposable;
import consulo.logging.Logger;
import consulo.platform.base.icon.PlatformIconGroup;
import consulo.repository.ui.localize.RepositoryUILocalize;
import consulo.ui.Component;
import consulo.ui.ImageBox;
import consulo.ui.ListBox;
import consulo.ui.Space;
import consulo.ui.TextBox;
import consulo.ui.TextBoxStyle;
import consulo.ui.UIAccess;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.layout.DockLayout;
import consulo.ui.layout.LoadingLayout;
import consulo.ui.layout.ScrollableLayout;
import consulo.ui.layout.SplitLayoutPosition;
import consulo.ui.layout.TwoComponentSplitLayout;
import consulo.ui.model.FlatDataModel;
import consulo.ui.model.MutableFlatDataModel;
import consulo.util.lang.StringUtil;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

/**
 * A list of packages with a search, and the details of the selected one. The list is loaded in the background - by the search text
 * when the search is done by the repository, otherwise once, and filtered by the name here.
 */
public class PackageListTab implements Disposable {
    private static final Logger LOG = Logger.getInstance(PackageListTab.class);

    private final UIAccess myUIAccess;
    private final PackagesPanel myPanel;
    private final Function<String, List<PackageRow>> myLoader;
    private final boolean myRemoteSearch;

    private final LoadingLayout<DockLayout> myLoadingLayout;
    private final DockLayout myContent;
    private final TextBox mySearchBox;
    private final ListBox<PackageRow> myList;
    private final PackageDetailsPanel myDetailsPanel;

    private List<PackageRow> myRows = List.of();
    private Future<?> mySearchFuture = CompletableFuture.completedFuture(null);
    private Future<?> myLoadFuture = CompletableFuture.completedFuture(null);

    /**
     * @param loader       loads the rows in a background thread, by the search text if the search is remote
     * @param remoteSearch whether the loader searches by the text, otherwise the rows are filtered here
     */
    @RequiredUIAccess
    public PackageListTab(PackagesPanel panel, Function<String, List<PackageRow>> loader, boolean remoteSearch) {
        myUIAccess = UIAccess.current();
        myPanel = panel;
        myLoader = loader;
        myRemoteSearch = remoteSearch;

        myList = ListBox.create(FlatDataModel.lazyOf(List.of()));
        myList.setRenderer(PackageRowRenderer.reusable());
        myList.setItemHeightGetter(item -> PackageRowRenderer.ROW_HEIGHT);

        myDetailsPanel = new PackageDetailsPanel(panel);
        myList.addValueListener(event -> myDetailsPanel.update(event.getValue()));

        mySearchBox = TextBox.create("");
        mySearchBox.setPlaceholder(RepositoryUILocalize.searchPlaceholder());
        mySearchBox.addStyle(TextBoxStyle.INPLACE);
        mySearchBox.setPrefixComponent(ImageBox.create(PlatformIconGroup.actionsSearch()));
        mySearchBox.addValueListener(event -> onSearchChanged());

        DockLayout searchRow = DockLayout.create(Space.NONE);
        searchRow.center(mySearchBox);
        searchRow.borderBuilder().bottomSet().apply();

        // the awt and qt lists only scroll inside a scroll layout; the web list scrolls itself
        DockLayout listSide = DockLayout.create(Space.NONE);
        listSide.top(searchRow);
        listSide.center(ScrollableLayout.create(myList));

        TwoComponentSplitLayout split = TwoComponentSplitLayout.create(SplitLayoutPosition.HORIZONTAL);
        split.setProportion(40);
        split.setFirstComponent(listSide);
        split.setSecondComponent(myDetailsPanel.getComponent());

        myContent = DockLayout.create(Space.NONE);
        myContent.center(split);

        myLoadingLayout = LoadingLayout.create(DockLayout.create(Space.NONE), this);
        myLoadingLayout.setLoadingText(RepositoryUILocalize.loadingPackages());
    }

    public Component getComponent() {
        return myLoadingLayout;
    }

    public Component getPreferredFocusedComponent() {
        return mySearchBox;
    }

    @RequiredUIAccess
    public void reload() {
        String query = StringUtil.notNullize(mySearchBox.getValue());
        myLoadFuture.cancel(true);
        myLoadFuture = myLoadingLayout.startLoading(
            () -> {
                try {
                    return myLoader.apply(myRemoteSearch ? query : "");
                }
                catch (Exception e) {
                    LOG.warn(e);
                    return List.<PackageRow>of();
                }
            },
            // startLoading empties the host layout, so the content is put back rather than assumed to still hang there
            (layout, rows) -> {
                layout.center(myContent);
                setRows(rows);
            }
        );
    }

    /**
     * Rebuilds the rows from the state of the panel - after the installed packages or their latest versions changed - keeping the selection.
     */
    @RequiredUIAccess
    public void updateRows() {
        setRows(myRows);
    }

    @RequiredUIAccess
    public void selectPackage(String name) {
        for (PackageRow row : myList.getDataModel()) {
            if (row.name().equals(name)) {
                myList.setValue(row);
                return;
            }
        }
    }

    @RequiredUIAccess
    private void setRows(List<PackageRow> rows) {
        List<PackageRow> decorated = new ArrayList<>(rows.size());
        for (PackageRow row : rows) {
            decorated.add(myPanel.decorate(row));
        }
        myRows = List.copyOf(decorated);
        applyFilter();
    }

    @RequiredUIAccess
    private void onSearchChanged() {
        if (!myRemoteSearch) {
            applyFilter();
            return;
        }
        // the repository is asked when the typing stops
        mySearchFuture.cancel(false);
        mySearchFuture = myUIAccess.getScheduler().schedule(this::reload, 300, TimeUnit.MILLISECONDS);
    }

    @RequiredUIAccess
    private void applyFilter() {
        String filter = myRemoteSearch ? null : StringUtil.nullize(mySearchBox.getValue(), true);

        List<PackageRow> filtered = new ArrayList<>();
        for (PackageRow row : myRows) {
            if (filter == null || StringUtil.containsIgnoreCase(row.name(), filter)) {
                filtered.add(row);
            }
        }

        PackageRow selected = myList.getValue();
        ((MutableFlatDataModel<PackageRow>) myList.getDataModel()).replaceAll(filtered);

        @Nullable PackageRow toSelect = null;
        if (selected != null) {
            for (PackageRow row : filtered) {
                if (row.name().equals(selected.name())) {
                    toSelect = row;
                    break;
                }
            }
        }
        if (toSelect == null && !filtered.isEmpty()) {
            toSelect = filtered.get(0);
        }
        myList.setValue(toSelect);
        myDetailsPanel.update(toSelect);
    }

    @Override
    public void dispose() {
        mySearchFuture.cancel(false);
        myLoadFuture.cancel(true);
    }
}
