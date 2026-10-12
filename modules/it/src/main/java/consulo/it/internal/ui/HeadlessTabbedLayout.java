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
package consulo.it.internal.ui;

import consulo.ui.Component;
import consulo.ui.StaticPosition;
import consulo.ui.Tab;
import consulo.ui.TransferHandler;
import consulo.ui.event.TabSelectEvent;
import consulo.ui.layout.LayoutConstraint;
import consulo.ui.layout.TabbedLayout;
import consulo.ui.layout.TabbedLayoutStyle;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

/**
 * Headless {@link TabbedLayout}. It keeps its tabs with their content in the order they were added, and remembers
 * which one is selected.
 *
 * @author VISTALL
 */
public class HeadlessTabbedLayout extends HeadlessLayoutBase<LayoutConstraint> implements TabbedLayout {
    private final StaticPosition myTabPosition;

    private final Map<Tab, Component> myTabs = new LinkedHashMap<>();

    private @Nullable Tab mySelectedTab;

    private @Nullable Component myPrefixComponent;
    private @Nullable Component mySuffixComponent;

    private @Nullable TransferHandler<Tab> myTransferHandler;

    /**
     * @throws IllegalArgumentException if {@code tabPosition} is {@link StaticPosition#CENTER}, which is not a side
     */
    public HeadlessTabbedLayout(StaticPosition tabPosition) {
        if (tabPosition == StaticPosition.CENTER) {
            throw new IllegalArgumentException("CENTER is not a valid tab position");
        }
        myTabPosition = tabPosition;
    }

    @Override
    public StaticPosition getTabPosition() {
        return myTabPosition;
    }

    @Override
    public Tab createTab() {
        return new HeadlessTab(this);
    }

    @Override
    public void setPrefixComponent(@Nullable Component prefixComponent) {
        myPrefixComponent = prefixComponent;
    }

    @Override
    public @Nullable Component getPrefixComponent() {
        return myPrefixComponent;
    }

    @Override
    public void setSuffixComponent(@Nullable Component suffixComponent) {
        mySuffixComponent = suffixComponent;
    }

    @Override
    public @Nullable Component getSuffixComponent() {
        return mySuffixComponent;
    }

    @Override
    public void addStyle(TabbedLayoutStyle style) {
    }

    @Override
    public Tab addTab(Tab tab, Component component) {
        Component old = myTabs.put(tab, component);
        if (old != null) {
            super.remove(old);
        }

        addChild(component);

        if (mySelectedTab == null) {
            mySelectedTab = tab;
        }
        return tab;
    }

    @Override
    public Tab addTab(Tab tab, Component component, int index) {
        addTab(tab, component);
        moveTab(tab, index);
        return tab;
    }

    @Override
    public void moveTab(Tab tab, int index) {
        Component component = myTabs.get(tab);
        if (component == null) {
            return;
        }

        List<Map.Entry<Tab, Component>> entries = new ArrayList<>(myTabs.entrySet());
        entries.removeIf(entry -> entry.getKey() == tab);
        entries.add(Math.max(0, Math.min(index, entries.size())), Map.entry(tab, component));

        myTabs.clear();
        for (Map.Entry<Tab, Component> entry : entries) {
            myTabs.put(entry.getKey(), entry.getValue());
        }
    }

    @Override
    public void setTransferHandler(@Nullable TransferHandler<Tab> handler) {
        myTransferHandler = handler;
    }

    @Override
    public @Nullable TransferHandler<Tab> getTransferHandler() {
        return myTransferHandler;
    }

    @Override
    public void removeTab(Tab tab) {
        Component component = myTabs.remove(tab);
        if (component != null) {
            super.remove(component);
        }

        if (mySelectedTab == tab) {
            mySelectedTab = myTabs.isEmpty() ? null : myTabs.keySet().iterator().next();
        }
    }

    @Override
    public void remove(Component component) {
        List<Tab> tabs = new ArrayList<>();
        myTabs.forEach((tab, content) -> {
            if (content == component) {
                tabs.add(tab);
            }
        });
        tabs.forEach(this::removeTab);
    }

    @Override
    public void removeAll() {
        myTabs.clear();
        mySelectedTab = null;
        super.removeAll();
    }

    @Override
    public void forEachTab(BiConsumer<Tab, Component> consumer) {
        new ArrayList<>(myTabs.entrySet()).forEach(entry -> consumer.accept(entry.getKey(), entry.getValue()));
    }

    @Override
    public void forEachChild(Consumer<Component> consumer) {
        forEachTab((tab, component) -> consumer.accept(component));
    }

    @Override
    public @Nullable Tab getSelectedTab() {
        return mySelectedTab;
    }

    void select(Tab tab) {
        if (!myTabs.containsKey(tab) || mySelectedTab == tab) {
            return;
        }

        mySelectedTab = tab;

        getListenerDispatcher(TabSelectEvent.class).onEvent(new TabSelectEvent(this, tab));
    }
}
