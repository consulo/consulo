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
import consulo.ui.Tab;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.event.TabSelectEvent;
import consulo.ui.ex.awt.JBTabbedPane;
import consulo.ui.ex.awtUnsafe.TargetAWT;
import consulo.ui.layout.TabbedLayout;
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
        @Override
        public Component toUIComponent() {
            return DesktopTabbedLayoutImpl.this;
        }
    }

    private final List<DesktopTabImpl> myTabs = new ArrayList<>();

    private @Nullable Component myPrefixComponent;
    private @Nullable Component mySuffixComponent;

    @Override
    protected JTabbedPane createComponent() {
        JTabbedPane pane = new MyTabbedPane();
        pane.addChangeListener(event -> {
            int index = pane.getSelectedIndex();
            if (index >= 0 && index < myTabs.size()) {
                getListenerDispatcher(TabSelectEvent.class)
                    .onEvent(new TabSelectEvent(this, myTabs.get(index), DesktopAWTInputDetails.currentEvent(pane)));
            }
        });
        return pane;
    }

    @Override
    public Tab createTab() {
        return new DesktopTabImpl(this);
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
