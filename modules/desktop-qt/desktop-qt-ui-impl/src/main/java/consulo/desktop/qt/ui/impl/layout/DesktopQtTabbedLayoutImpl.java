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

    private @Nullable Component myPrefixComponent;
    private @Nullable Component mySuffixComponent;

    private boolean myNoPadding;

    private @Nullable DesktopQtTabImpl mySelectedTab;

    private boolean myRestoringTabs;

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

        applyCornerComponent(prefixComponent, Qt.Corner.TopLeftCorner);
    }

    @Override
    public @Nullable Component getPrefixComponent() {
        return myPrefixComponent;
    }

    @Override
    @RequiredUIAccess
    public void setSuffixComponent(@Nullable Component suffixComponent) {
        mySuffixComponent = suffixComponent;

        applyCornerComponent(suffixComponent, Qt.Corner.TopRightCorner);
    }

    @Override
    public @Nullable Component getSuffixComponent() {
        return mySuffixComponent;
    }

    @Override
    protected QTabWidget createQt(QWidget parent) {
        QTabWidget tabWidget = new DesktopQtTabWidget(parent);
        tabWidget.setDocumentMode(true);
        tabWidget.tabBar().setDrawBase(false);
        tabWidget.setTabPosition(QTabWidget.TabPosition.North);
        return tabWidget;
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

        applyCornerComponent(myPrefixComponent, Qt.Corner.TopLeftCorner);
        applyCornerComponent(mySuffixComponent, Qt.Corner.TopRightCorner);

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
     * rather than in the content, so the widget is given to the corner qt keeps for it.
     */
    @RequiredUIAccess
    private void applyCornerComponent(@Nullable Component component, Qt.Corner corner) {
        if (myComponent == null || myComponent.isDisposed()) {
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
        QTabWidget tabWidget = toQtComponent();

        if (tabWidget != null) {
            tab.initialize(tabWidget, this);

            tabWidget.setCurrentIndex(tab.getIndex());
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
            qtTab.detach();
        }
    }

    @Override
    @RequiredUIAccess
    public void removeAll() {
        for (DesktopQtTabImpl tab : new ArrayList<>(myTabs)) {
            tab.detach();
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
