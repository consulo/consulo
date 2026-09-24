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
package consulo.execution.impl.internal.ui.layout;

import consulo.execution.ui.layout.PlaceInGrid;
import consulo.ui.Component;
import consulo.ui.Tab;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.content.Content;
import consulo.ui.ex.content.ContentManager;
import consulo.ui.ex.content.ContentUI;
import consulo.ui.ex.content.event.ContentManagerEvent;
import consulo.ui.ex.content.event.ContentManagerListener;
import consulo.ui.ex.localize.UILocalize;
import consulo.ui.image.Image;
import consulo.ui.layout.DockLayout;
import consulo.ui.layout.SplitLayoutPosition;
import consulo.ui.layout.TabbedLayout;
import consulo.ui.layout.TwoComponentSplitLayout;
import org.jspecify.annotations.Nullable;

import java.util.*;

/**
 * The views of a run as the tabs of a {@link TabbedLayout}: the views a runner puts into the same tab are shown
 * together, placed by their {@link PlaceInGrid} - the frames and the variables of a debug session share "Threads &amp;
 * Variables" - and a tab of a single view is the view itself. The toolbar of the layout goes in front of the tabs, in
 * the same row, the way the awt layout puts it.
 *
 * @author VISTALL
 * @since 2026-09-24
 */
public class UnifiedRunnerContentUI implements ContentUI, ContentManagerListener {
    /**
     * The views of one tab of the layout.
     */
    private record TabGroup(int index, List<Content> contents) {
    }

    private final RunnerLayoutImpl myLayout;

    private final TabbedLayout myTabbedLayout = TabbedLayout.create();

    private @Nullable ContentManager myManager;

    /**
     * The tabs shown and the views in each - the tabs are built again only when the views change.
     */
    private final List<TabGroup> myShownGroups = new ArrayList<>();
    private final List<Tab> myShownTabs = new ArrayList<>();
    /**
     * Set while the tabs are changed from the views, so the tab selected along the way does not select a view.
     */
    private boolean myUpdating;

    /**
     * The grid a tab was shown with - it is built again only when the views of the tab change, so switching between
     * the tabs keeps the proportions the user dragged.
     */
    private final Map<Integer, Component> myGrids = new HashMap<>();
    private final Map<Integer, List<Content>> myGridContents = new HashMap<>();

    public UnifiedRunnerContentUI(RunnerLayoutImpl layout) {
        myLayout = layout;

        myTabbedLayout.addSelectListener(event -> {
            ContentManager manager = myManager;
            int index = myShownTabs.indexOf(event.getTab());
            if (myUpdating || manager == null || index < 0) {
                return;
            }

            TabGroup group = myShownGroups.get(index);
            Content current = manager.getSelectedContent();
            if (current == null || !group.contents().contains(current)) {
                manager.setSelectedContent(group.contents().get(0), true);
            }
        });
    }

    public void setToolbar(Component toolbar) {
        myTabbedLayout.setPrefixComponent(toolbar);
    }

    @Override
    public void setManager(ContentManager manager) {
        myManager = manager;
        manager.addContentManagerListener(this);
    }

    @Override
    @RequiredUIAccess
    public void selectionChanged(ContentManagerEvent event) {
        update();
    }

    @Override
    @RequiredUIAccess
    public void contentAdded(ContentManagerEvent event) {
        update();
    }

    @Override
    @RequiredUIAccess
    public void contentRemoved(ContentManagerEvent event) {
        update();
    }

    @RequiredUIAccess
    private void update() {
        ContentManager manager = myManager;
        if (manager == null) {
            return;
        }

        myUpdating = true;
        try {
            List<TabGroup> groups = groupByTab(manager);
            if (!groups.equals(myShownGroups)) {
                // a tab is added at the end of the row - one which goes between the others means building them again
                for (Tab tab : myShownTabs) {
                    myTabbedLayout.removeTab(tab);
                }
                myShownTabs.clear();
                myShownGroups.clear();

                for (TabGroup group : groups) {
                    Tab tab = myTabbedLayout.createTab();
                    tab.setRenderer((t, presentation) -> {
                        presentation.withIcon(getTabIcon(group));
                        presentation.append(getTabName(group));
                    });
                    myTabbedLayout.addTab(tab, getOrBuildGrid(group));

                    myShownTabs.add(tab);
                    myShownGroups.add(group);
                }
            }

            Content selected = manager.getSelectedContent();
            int selectedIndex = selected == null ? -1 : indexOfGroup(myShownGroups, selected);
            if (selectedIndex >= 0) {
                myShownTabs.get(selectedIndex).select();
            }
        }
        finally {
            myUpdating = false;
        }
    }

    private List<TabGroup> groupByTab(ContentManager manager) {
        Map<Integer, List<Content>> byTab = new TreeMap<>();
        for (Content content : manager.getContents()) {
            int index = myLayout.getStateFor(content).getTabIndex();
            byTab.computeIfAbsent(index, i -> new ArrayList<>()).add(content);
        }

        List<TabGroup> groups = new ArrayList<>(byTab.size());
        for (Map.Entry<Integer, List<Content>> entry : byTab.entrySet()) {
            groups.add(new TabGroup(entry.getKey(), entry.getValue()));
        }
        return groups;
    }

    private static int indexOfGroup(List<TabGroup> groups, Content content) {
        for (int i = 0; i < groups.size(); i++) {
            if (groups.get(i).contents().contains(content)) {
                return i;
            }
        }
        return -1;
    }

    private String getTabName(TabGroup group) {
        if (group.contents().size() == 1) {
            return group.contents().get(0).getTabName();
        }

        String displayName = myLayout.getOrCreateTab(group.index()).getDisplayName();
        if (displayName != null && !displayName.isEmpty()) {
            return displayName;
        }

        StringJoiner joiner = new StringJoiner(" & ");
        for (Content content : group.contents()) {
            joiner.add(content.getTabName());
        }
        return joiner.toString();
    }

    private @Nullable Image getTabIcon(TabGroup group) {
        if (group.contents().size() == 1) {
            return group.contents().get(0).getIcon();
        }
        return myLayout.getOrCreateTab(group.index()).getIcon();
    }

    @RequiredUIAccess
    private Component getOrBuildGrid(TabGroup group) {
        Component grid = myGrids.get(group.index());
        if (grid != null && group.contents().equals(myGridContents.get(group.index()))) {
            return grid;
        }

        grid = buildGrid(group);
        myGrids.put(group.index(), grid);
        myGridContents.put(group.index(), List.copyOf(group.contents()));
        return grid;
    }

    /**
     * The places of the awt grid: the left, the center and the right side by side, the bottom below them.
     */
    @RequiredUIAccess
    private Component buildGrid(TabGroup group) {
        EnumMap<PlaceInGrid, Component> places = new EnumMap<>(PlaceInGrid.class);
        for (Content content : group.contents()) {
            Component component = content.getUIComponent();
            if (component == null) {
                continue;
            }

            PlaceInGrid place = myLayout.getStateFor(content).getPlaceInGrid();
            // two views in one place - the later one goes where there is room left
            while (places.containsKey(place) && place != PlaceInGrid.bottom) {
                place = PlaceInGrid.values()[place.ordinal() + 1];
            }
            places.putIfAbsent(place, component);
        }

        TabImpl tab = myLayout.getOrCreateTab(group.index());

        Component row = places.get(PlaceInGrid.center);
        Component left = places.get(PlaceInGrid.left);
        Component right = places.get(PlaceInGrid.right);

        if (right != null) {
            row = row == null ? right : split(SplitLayoutPosition.HORIZONTAL, row, right, 100 - toPercent(tab.getRightProportion()));
        }
        if (left != null) {
            row = row == null ? left : split(SplitLayoutPosition.HORIZONTAL, left, row, Math.max(toPercent(tab.getLeftProportion()), 25));
        }

        Component bottom = places.get(PlaceInGrid.bottom);
        if (bottom != null) {
            row = row == null ? bottom : split(SplitLayoutPosition.VERTICAL, row, bottom, 100 - toPercent(tab.getBottomProportion()));
        }

        DockLayout grid = DockLayout.create();
        if (row != null) {
            grid.center(row);
        }
        return grid;
    }

    @RequiredUIAccess
    private static Component split(SplitLayoutPosition position, Component first, Component second, int proportion) {
        TwoComponentSplitLayout layout = TwoComponentSplitLayout.create(position);
        layout.setFirstComponent(first);
        layout.setSecondComponent(second);
        layout.setProportion(proportion);
        return layout;
    }

    private static int toPercent(float proportion) {
        return Math.round(proportion * 100);
    }

    @Override
    public Component getUIComponent() {
        return myTabbedLayout;
    }

    @Override
    public boolean isSingleSelection() {
        return true;
    }

    @Override
    public boolean isToSelectAddedContent() {
        return false;
    }

    @Override
    public boolean canBeEmptySelection() {
        return false;
    }

    @Override
    public void beforeDispose() {
    }

    @Override
    public boolean canChangeSelectionTo(Content content, boolean implicit) {
        return true;
    }

    @Override
    public String getCloseActionName() {
        return UILocalize.tabbedPaneCloseTabActionName().get();
    }

    @Override
    public String getCloseAllButThisActionName() {
        return UILocalize.tabbedPaneCloseAllTabsButThisActionName().get();
    }

    @Override
    public String getPreviousContentActionName() {
        return "Select Previous Tab";
    }

    @Override
    public String getNextContentActionName() {
        return "Select Next Tab";
    }

    @Override
    public void dispose() {
    }
}
