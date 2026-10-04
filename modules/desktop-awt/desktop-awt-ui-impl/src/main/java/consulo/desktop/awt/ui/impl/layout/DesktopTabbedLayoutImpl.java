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
package consulo.desktop.awt.ui.impl.layout;

import consulo.desktop.awt.ui.impl.event.DesktopAWTInputDetails;
import consulo.desktop.awt.ui.impl.facade.FromSwingComponentWrapper;
import consulo.desktop.awt.ui.impl.base.SwingComponentDelegate;
import consulo.ui.Component;
import consulo.ui.StaticPosition;
import consulo.ui.Tab;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.event.TabSelectEvent;
import consulo.ui.internal.TabSelectionUtil;
import consulo.ui.ex.awt.JBTabbedPane;
import consulo.ui.ex.awtUnsafe.TargetAWT;
import consulo.ui.layout.TabbedLayout;
import consulo.ui.layout.TabbedLayoutStyle;
import org.jspecify.annotations.Nullable;

import javax.swing.*;
import java.util.ArrayList;
import java.util.List;

/**
 * @author VISTALL
 * @since 2016-06-14
 */
public class DesktopTabbedLayoutImpl extends SwingComponentDelegate<JTabbedPane> implements TabbedLayout {
    class MyTabbedPane extends JBTabbedPane implements FromSwingComponentWrapper {
        MyTabbedPane(int tabPlacement) {
            super(tabPlacement);
        }

        @Override
        public Component toUIComponent() {
            return DesktopTabbedLayoutImpl.this;
        }
    }

    private final List<DesktopTabImpl> myTabs = new ArrayList<>();

    private final StaticPosition myTabPosition;
    private final int myTabPlacement;

    private @Nullable Component myPrefixComponent;
    private @Nullable Component mySuffixComponent;

    /**
     * @throws IllegalArgumentException if {@code tabPosition} is {@link StaticPosition#CENTER}, which is not a side
     */
    public DesktopTabbedLayoutImpl(StaticPosition tabPosition) {
        myTabPosition = tabPosition;
        myTabPlacement = toTabPlacement(tabPosition);
    }

    /**
     * The placement is fixed for the lifetime of the layout, so the pane gets it when it is created.
     */
    @Override
    protected JTabbedPane createComponent() {
        JTabbedPane pane = new MyTabbedPane(myTabPlacement);
        pane.addChangeListener(event -> {
            int index = pane.getSelectedIndex();
            if (index >= 0 && index < myTabs.size()) {
                getListenerDispatcher(TabSelectEvent.class)
                    .onEvent(new TabSelectEvent(this, myTabs.get(index), DesktopAWTInputDetails.currentEvent(pane)));
            }
        });
        return pane;
    }

    private static int toTabPlacement(StaticPosition tabPosition) {
        return switch (tabPosition) {
            case TOP -> SwingConstants.TOP;
            case BOTTOM -> SwingConstants.BOTTOM;
            case LEFT -> SwingConstants.LEFT;
            case RIGHT -> SwingConstants.RIGHT;
            case CENTER -> throw new IllegalArgumentException("CENTER is not a valid tab position");
        };
    }

    @Override
    public StaticPosition getTabPosition() {
        return myTabPosition;
    }

    @Override
    public Tab createTab() {
        return new DesktopTabImpl(this);
    }

    @Override
    public void addStyle(TabbedLayoutStyle style) {
        switch (style) {
            case NO_PADDING -> ((JBTabbedPane) toAWTComponent()).setTabComponentInsets(null);
        }
    }

    @Override
    @RequiredUIAccess
    public Tab addTab(Tab tab, Component component) {
        DesktopTabImpl desktopTab = (DesktopTabImpl) tab;

        desktopTab.setComponent(component);

        JTabbedPane pane = toAWTComponent();
        pane.addTab("", TargetAWT.to(component));

        int index = pane.getTabCount() - 1;
        pane.setTabComponentAt(index, desktopTab.getTabComponent());
        myTabs.add(desktopTab);

        desktopTab.update();

        applyEnabled(desktopTab);

        return tab;
    }

    @Override
    @RequiredUIAccess
    public Tab addTab(String tabName, Component component) {
        Tab tab = createTab();
        tab.setRenderer((t, p) -> p.append(tabName));
        return addTab(tab, component);
    }

    @Override
    public void removeTab(Tab tab) {
        DesktopTabImpl desktopTab = (DesktopTabImpl) tab;

        int index = myTabs.indexOf(desktopTab);
        if (index == -1) {
            return;
        }

        toAWTComponent().removeTabAt(index);
        myTabs.remove(index);
    }

    int indexOf(DesktopTabImpl tab) {
        return myTabs.indexOf(tab);
    }

    void applyEnabled(DesktopTabImpl tab) {
        tab.updateEnabledLook();

        int index = myTabs.indexOf(tab);
        if (index == -1) {
            return;
        }

        JTabbedPane pane = toAWTComponent();
        boolean enabled = tab.isEnabled();
        if (!enabled && pane.getSelectedIndex() == index) {
            int target = TabSelectionUtil.findSelectionOnDisable(index, myTabs.size(), i -> myTabs.get(i).isEnabled());
            if (target != -1) {
                pane.setSelectedIndex(target);
            }
        }

        pane.setEnabledAt(index, enabled);
    }

    @Override
    public void setPrefixComponent(@Nullable Component prefixComponent) {
        myPrefixComponent = prefixComponent;
        JComponent pane = toAWTComponent();
        pane.putClientProperty("JTabbedPane.leadingComponent", prefixComponent == null ? null : TargetAWT.to(prefixComponent));
    }

    @Override
    public @Nullable Component getPrefixComponent() {
        return myPrefixComponent;
    }

    @Override
    public void setSuffixComponent(@Nullable Component suffixComponent) {
        mySuffixComponent = suffixComponent;
        JComponent pane = toAWTComponent();
        pane.putClientProperty("JTabbedPane.trailingComponent", suffixComponent == null ? null : TargetAWT.to(suffixComponent));
    }

    @Override
    public @Nullable Component getSuffixComponent() {
        return mySuffixComponent;
    }
}
