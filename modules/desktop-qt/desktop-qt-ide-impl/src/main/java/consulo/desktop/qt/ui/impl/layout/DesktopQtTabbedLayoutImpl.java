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
import consulo.ui.layout.TabbedLayout;
import consulo.dataContext.DataManager;
import consulo.desktop.qt.ui.impl.action.DesktopQtActionContextMenu;
import consulo.ui.ex.action.ActionGroup;
import consulo.ui.ex.action.ActionPlaces;
import consulo.ui.ex.action.CustomActionsSchema;
import io.qt.core.QPoint;
import io.qt.core.Qt;
import io.qt.widgets.QTabBar;
import io.qt.widgets.QTabWidget;
import io.qt.widgets.QWidget;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * @author VISTALL
 * @since 2026-08-16
 */
public class DesktopQtTabbedLayoutImpl extends QtComponentDelegate<QTabWidget> implements TabbedLayout {
    private final List<DesktopQtTabImpl> myTabs = new ArrayList<>();

    private @Nullable Component myPrefixComponent;
    private @Nullable Component mySuffixComponent;

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
        QTabWidget tabWidget = new QTabWidget(parent);
        tabWidget.setDocumentMode(true);
        tabWidget.setTabPosition(QTabWidget.TabPosition.North);
        return tabWidget;
    }

    @Override
    protected void initialize(QTabWidget component) {
        super.initialize(component);

        // the bar draws a cross for every tab, and a tab which was given no close handler takes its own back off
        component.setTabsClosable(true);
        component.tabCloseRequested.connect(this::closeTab);

        for (DesktopQtTabImpl tab : myTabs) {
            tab.initialize(component, this);
        }

        component.setCurrentIndex(myTabs.size() - 1);

        applyCornerComponent(myPrefixComponent, Qt.Corner.TopLeftCorner);
        applyCornerComponent(mySuffixComponent, Qt.Corner.TopRightCorner);

        installTabPopupMenu(component);
    }

    @RequiredUIAccess
    private void closeTab(int index) {
        for (DesktopQtTabImpl tab : myTabs) {
            if (tab.getIndex() == index) {
                tab.close();
                return;
            }
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
                    return null;
                }

                return CustomActionsSchema.getInstance().getCorrectedAction(tab.getPopupGroupId()) instanceof ActionGroup group
                    ? group
                    : null;
            },
            ActionPlaces.EDITOR_TAB_POPUP,
            position -> {
                DesktopQtTabImpl tab = tabAt(tabBar, position);
                return tab == null ? DataManager.getInstance().getDataContext() : tab.createDataContext();
            }
        );
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
