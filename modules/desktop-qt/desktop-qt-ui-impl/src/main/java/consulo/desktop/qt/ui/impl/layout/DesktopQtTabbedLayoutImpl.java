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
package consulo.desktop.qt.ui.impl.layout;

import consulo.desktop.qt.ui.impl.QtComponentDelegate;
import consulo.ui.Component;
import consulo.ui.StaticPosition;
import consulo.ui.Tab;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.event.TabSelectEvent;
import consulo.ui.layout.TabbedLayout;
import consulo.ui.layout.TabbedLayoutStyle;
import consulo.ui.Space;
import consulo.desktop.qt.ui.impl.DesktopQtSpace;
import consulo.dataContext.DataManager;
import consulo.desktop.qt.ui.impl.action.DesktopQtActionContextMenu;
import consulo.ui.ex.action.ActionGroup;
import consulo.ui.ex.action.ActionPlaces;
import consulo.ui.ex.action.CustomActionsSchema;
import io.qt.core.QPoint;
import io.qt.core.QSize;
import io.qt.core.Qt;
import io.qt.gui.QColor;
import io.qt.gui.QIcon;
import io.qt.gui.QPixmap;
import io.qt.widgets.QStackedWidget;
import io.qt.widgets.QTabBar;
import io.qt.widgets.QTabWidget;
import io.qt.widgets.QWidget;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

/**
 * @author VISTALL
 * @since 2026-08-16
 */
public class DesktopQtTabbedLayoutImpl extends QtComponentDelegate<QTabWidget> implements TabbedLayout {
    private final List<DesktopQtTabImpl> myTabs = new ArrayList<>();

    private final StaticPosition myTabPosition;

    private @Nullable Component myPrefixComponent;
    private @Nullable Component mySuffixComponent;

    private boolean myNoPadding;

    private @Nullable DesktopQtTabImpl mySelectedTab;

    private boolean myRestoringTabs;

    /**
     * @throws IllegalArgumentException if {@code tabPosition} is {@link StaticPosition#CENTER}, which is not a side
     */
    public DesktopQtTabbedLayoutImpl(StaticPosition tabPosition) {
        if (tabPosition == StaticPosition.CENTER) {
            throw new IllegalArgumentException("CENTER is not a valid tab position");
        }
        myTabPosition = tabPosition;
    }

    @RequiredUIAccess
    public static int tabRowHeight() {
        QTabBar tabBar = new QTabBar();
        try {
            tabBar.setDocumentMode(true);

            QSize iconSize = tabBar.iconSize();
            QPixmap pixmap = new QPixmap(iconSize);
            pixmap.fill(new QColor(Qt.GlobalColor.transparent));

            int index = tabBar.addTab(new QIcon(pixmap), "W");
            tabBar.setTabButton(index, QTabBar.ButtonPosition.RightSide, new DesktopQtTabCloseButton(tabBar));
            tabBar.ensurePolished();

            return tabBar.sizeHint().height();
        }
        finally {
            tabBar.dispose();
        }
    }

    @Override
    @RequiredUIAccess
    public void setPrefixComponent(@Nullable Component prefixComponent) {
        myPrefixComponent = prefixComponent;

        applyCornerComponent(prefixComponent, prefixCorner());
    }

    @Override
    public @Nullable Component getPrefixComponent() {
        return myPrefixComponent;
    }

    @Override
    @RequiredUIAccess
    public void setSuffixComponent(@Nullable Component suffixComponent) {
        mySuffixComponent = suffixComponent;

        applyCornerComponent(suffixComponent, suffixCorner());
    }

    @Override
    public @Nullable Component getSuffixComponent() {
        return mySuffixComponent;
    }

    @Override
    public StaticPosition getTabPosition() {
        return myTabPosition;
    }

    @Override
    protected QTabWidget createQt(QWidget parent) {
        QTabWidget tabWidget = new DesktopQtTabWidget(parent);
        tabWidget.setDocumentMode(true);
        tabWidget.tabBar().setDrawBase(false);
        tabWidget.setTabPosition(toQtTabPosition(myTabPosition));
        return tabWidget;
    }

    private static QTabWidget.TabPosition toQtTabPosition(StaticPosition tabPosition) {
        return switch (tabPosition) {
            case TOP -> QTabWidget.TabPosition.North;
            case BOTTOM -> QTabWidget.TabPosition.South;
            case LEFT -> QTabWidget.TabPosition.West;
            case RIGHT -> QTabWidget.TabPosition.East;
            // the constructor rejects it already
            case CENTER -> throw new IllegalArgumentException("CENTER is not a valid tab position");
        };
    }

    /**
     * Qt has corner widgets only for a tab bar above or below the content. Next to a West or East bar its style lays the
     * corner widgets out to an empty rectangle, so they are not shown, and still shortens the bar by their size, which
     * leaves a gap before and after the tabs. A layout with its tabs at a side therefore does not install the prefix and
     * suffix at all - this frontend does not show them there.
     */
    private boolean hasCornerWidgets() {
        return myTabPosition == StaticPosition.TOP || myTabPosition == StaticPosition.BOTTOM;
    }

    /**
     * The corner beside the start of the tab bar. {@link QTabWidget#setCornerWidget} tells only the left corner from the
     * right one and the style puts it next to the bar either way, the bottom corners just name where a widget of a bar
     * below the content ends up.
     */
    private Qt.Corner prefixCorner() {
        return myTabPosition == StaticPosition.BOTTOM ? Qt.Corner.BottomLeftCorner : Qt.Corner.TopLeftCorner;
    }

    /**
     * @see #prefixCorner()
     */
    private Qt.Corner suffixCorner() {
        return myTabPosition == StaticPosition.BOTTOM ? Qt.Corner.BottomRightCorner : Qt.Corner.TopRightCorner;
    }

    @Override
    protected void initialize(QTabWidget component) {
        super.initialize(component);

        myRestoringTabs = true;
        try {
            for (DesktopQtTabImpl tab : myTabs) {
                tab.initialize(component, this);
            }

            DesktopQtTabImpl selectedTab = mySelectedTab;
            int selectedIndex = selectedTab == null ? -1 : selectedTab.getIndex();
            component.setCurrentIndex(selectedIndex == -1 ? myTabs.size() - 1 : selectedIndex);
        }
        finally {
            myRestoringTabs = false;
        }

        component.currentChanged.connect(this::onCurrentChanged);

        applyCornerComponent(myPrefixComponent, prefixCorner());
        applyCornerComponent(mySuffixComponent, suffixCorner());

        applyPagePadding();

        installTabPopupMenu(component);
    }

    @Override
    @RequiredUIAccess
    public void addStyle(TabbedLayoutStyle style) {
        switch (style) {
            case NO_PADDING -> {
                myNoPadding = true;
                applyPagePadding();
            }
        }
    }

    private void applyPagePadding() {
        QTabWidget tabWidget = myComponent;
        if (tabWidget == null || tabWidget.isDisposed()) {
            return;
        }

        QStackedWidget pages = tabWidget.findChild(QStackedWidget.class, "qt_tabwidget_stackedwidget");
        if (pages != null) {
            int padding = myNoPadding ? 0 : DesktopQtSpace.toPixels(Space.MEDIUM);
            pages.setContentsMargins(padding, padding, padding, padding);
        }
    }

    /**
     * What a tab bar shows beside its tabs - the toolbar of the editor window sits at the trailing end of the row
     * rather than in the content, so the widget is given to the corner qt keeps for it. A bar along a side has no such
     * corner, see {@link #hasCornerWidgets()}.
     */
    @RequiredUIAccess
    private void applyCornerComponent(@Nullable Component component, Qt.Corner corner) {
        if (myComponent == null || myComponent.isDisposed() || !hasCornerWidgets()) {
            return;
        }

        if (!(component instanceof QtComponentDelegate<?> delegate)) {
            myComponent.setCornerWidget(null, corner);
            return;
        }

        delegate.setParent(this);
        delegate.bind(myComponent, null);

        myComponent.setCornerWidget(delegate.toQtComponent(), corner);
    }

    /**
     * One handler on the bar rather than one per tab: the bar is a single widget, so every tab installing its own
     * would leave the last one to overwrite the policy and the rest to fire alongside it. The tab under the
     * pointer is resolved from the position of the click instead.
     */
    private void installTabPopupMenu(QTabWidget tabWidget) {
        QTabBar tabBar = tabWidget.tabBar();

        DesktopQtActionContextMenu.installOn(
            tabBar,
            position -> {
                DesktopQtTabImpl tab = tabAt(tabBar, position);
                if (tab == null || tab.getPopupGroupId() == null) {
                    return CompletableFuture.completedFuture(null);
                }

                return CustomActionsSchema.getCorrectedGroupAsync(tab.getPopupGroupId());
            },
            ActionPlaces.EDITOR_TAB_POPUP,
            position -> {
                DesktopQtTabImpl tab = tabAt(tabBar, position);
                return tab == null ? DataManager.getInstance().getDataContext() : tab.createDataContext();
            }
        );
    }

    private void onCurrentChanged(int index) {
        if (myRestoringTabs) {
            return;
        }

        DesktopQtTabImpl tab = tabAtIndex(index);
        mySelectedTab = tab;

        if (tab != null) {
            getListenerDispatcher(TabSelectEvent.class).onEvent(new TabSelectEvent(this, tab));
        }
    }

    private @Nullable DesktopQtTabImpl tabAtIndex(int index) {
        if (index == -1) {
            return null;
        }

        for (DesktopQtTabImpl tab : myTabs) {
            if (tab.getIndex() == index) {
                return tab;
            }
        }

        return null;
    }

    private @Nullable DesktopQtTabImpl tabAt(QTabBar tabBar, QPoint position) {
        int index = tabBar.tabAt(position);
        if (index == -1) {
            return null;
        }

        for (DesktopQtTabImpl tab : myTabs) {
            if (tab.getIndex() == index) {
                return tab;
            }
        }

        return null;
    }

    private void init(DesktopQtTabImpl tab) {
        tab.setTabbedLayout(this);

        QTabWidget tabWidget = myComponent;

        if (tabWidget != null && !tabWidget.isDisposed()) {
            tab.initialize(tabWidget, this);

            tabWidget.setCurrentIndex(tab.getIndex());
        }
        else {
            mySelectedTab = tab;
        }
    }

    @RequiredUIAccess
    void selectUnrealized(DesktopQtTabImpl tab) {
        if (!myTabs.contains(tab) || mySelectedTab == tab) {
            return;
        }

        mySelectedTab = tab;

        getListenerDispatcher(TabSelectEvent.class).onEvent(new TabSelectEvent(this, tab));
    }

    private void forgetTab(DesktopQtTabImpl tab) {
        tab.detach();
        tab.setTabbedLayout(null);

        if (mySelectedTab == tab) {
            mySelectedTab = null;
        }
    }

    @Override
    public Tab createTab() {
        return new DesktopQtTabImpl();
    }

    @Override
    @RequiredUIAccess
    public Tab addTab(Tab tab, Component component) {
        DesktopQtTabImpl qtTab = (DesktopQtTabImpl) tab;

        myTabs.add(qtTab);
        qtTab.setComponent(component);

        init(qtTab);
        return tab;
    }

    @Override
    @RequiredUIAccess
    public Tab addTab(String tabName, Component component) {
        DesktopQtTabImpl tab = new DesktopQtTabImpl();
        tab.setRenderer((t, p) -> p.append(tabName));

        myTabs.add(tab);
        tab.setComponent(component);
        tab.update();

        init(tab);
        return tab;
    }

    @Override
    @RequiredUIAccess
    public void removeTab(Tab tab) {
        if (tab instanceof DesktopQtTabImpl qtTab && myTabs.remove(qtTab)) {
            forgetTab(qtTab);
        }
    }

    @Override
    @RequiredUIAccess
    public void removeAll() {
        for (DesktopQtTabImpl tab : new ArrayList<>(myTabs)) {
            forgetTab(tab);
        }

        myTabs.clear();
    }

    @Override
    @RequiredUIAccess
    public void remove(Component component) {
        for (DesktopQtTabImpl tab : new ArrayList<>(myTabs)) {
            if (tab.getComponent() == component) {
                removeTab(tab);
            }
        }
    }

    @Override
    public void forEachChild(@RequiredUIAccess Consumer<Component> consumer) {
        for (DesktopQtTabImpl tab : new ArrayList<>(myTabs)) {
            QtComponentDelegate<?> component = tab.getComponent();
            if (component != null) {
                consumer.accept(component);
            }
        }
    }
}
