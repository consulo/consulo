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
package consulo.ui.layout;

import consulo.annotation.DeprecationInfo;
import consulo.disposer.Disposable;
import consulo.localize.LocalizeValue;
import consulo.ui.Component;
import consulo.ui.HasComponentStyle;
import consulo.ui.HasPrefixComponent;
import consulo.ui.HasSuffixComponent;
import consulo.ui.PseudoComponent;
import consulo.ui.StaticPosition;
import consulo.ui.Tab;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.event.ComponentEventListener;
import consulo.ui.event.TabSelectEvent;
import consulo.ui.internal.UIInternal;
import org.jspecify.annotations.Nullable;

import java.util.function.BiConsumer;
import java.util.function.Consumer;

/**
 * @author VISTALL
 * @since 2016-06-14
 */
public interface TabbedLayout extends Layout<LayoutConstraint>, HasPrefixComponent, HasSuffixComponent, HasComponentStyle<TabbedLayoutStyle> {
    /**
     * Creates a tab layout with the tabs above the content.
     *
     * @return new tab layout
     */
    static TabbedLayout create() {
        return create(StaticPosition.TOP);
    }

    /**
     * Creates a tab layout with the tabs placed at the given side of the content. The position is fixed for the lifetime
     * of the layout.
     * <p>
     * {@link StaticPosition#TOP} and {@link StaticPosition#BOTTOM} place the tabs in a horizontal row above or below the
     * content, {@link StaticPosition#LEFT} and {@link StaticPosition#RIGHT} place them in a vertical column at that side
     * of the content.
     *
     * @param tabPosition the side of the content the tabs are placed at
     * @return new tab layout
     * @throws IllegalArgumentException if {@code tabPosition} is {@link StaticPosition#CENTER}, which is not a side
     */
    static TabbedLayout create(StaticPosition tabPosition) {
        if (tabPosition == StaticPosition.CENTER) {
            throw new IllegalArgumentException("CENTER is not a valid tab position");
        }
        return UIInternal.get()._Layouts_tabbed(tabPosition);
    }

    /**
     * @return the side of the content the tabs are placed at, as given at creation
     */
    StaticPosition getTabPosition();

    /**
     * Create tab without adding to view
     *
     * @return new tab
     */
    Tab createTab();

    @RequiredUIAccess
    default Tab addTab(Tab tab, PseudoComponent component) {
        return addTab(tab, component.getComponent());
    }

    @Deprecated
    @DeprecationInfo("Use #addTab(LocalizeValue, PseudoComponent)")
    @RequiredUIAccess
    default Tab addTab(String tabName, PseudoComponent component) {
        return addTab(LocalizeValue.of(tabName), component.getComponent());
    }

    @RequiredUIAccess
    default Tab addTab(LocalizeValue tabName, PseudoComponent component) {
        return addTab(tabName, component.getComponent());
    }

    @RequiredUIAccess
    Tab addTab(Tab tab, Component component);

    @Deprecated
    @DeprecationInfo("Use #addTab(LocalizeValue, Component)")
    @RequiredUIAccess
    default Tab addTab(String tabName, Component component) {
        return addTab(LocalizeValue.of(tabName), component);
    }

    /**
     * Adds a tab whose caption is the given value, so the caption follows the locale and {@link Tab#getTitle()} answers
     * the value itself.
     */
    @RequiredUIAccess
    default Tab addTab(LocalizeValue tabName, Component component) {
        Tab tab = createTab();
        tab.setRenderer((t, presentation) -> presentation.append(tabName));
        return addTab(tab, component);
    }

    void removeTab(Tab tab);

    /**
     * Visits every tab with its content, in the order the tabs are shown. The content of a tab which was never
     * selected is visited as well.
     */
    void forEachTab(@RequiredUIAccess BiConsumer<Tab, Component> consumer);

    /**
     * The tab whose content is shown, also before the layout is on screen. Empty only while there are no tabs.
     */
    @Nullable Tab getSelectedTab();

    /**
     * Visits the content of every tab, in the order the tabs are shown.
     */
    @Override
    default void forEachChild(@RequiredUIAccess Consumer<Component> consumer) {
        forEachTab((tab, component) -> consumer.accept(component));
    }

    default Disposable addSelectListener(ComponentEventListener<Component, TabSelectEvent> listener) {
        return addListener(TabSelectEvent.class, listener);
    }
}
