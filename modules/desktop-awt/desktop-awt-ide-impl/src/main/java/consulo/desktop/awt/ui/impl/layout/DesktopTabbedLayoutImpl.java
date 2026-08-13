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

import consulo.desktop.awt.facade.FromSwingComponentWrapper;
import consulo.desktop.awt.ui.impl.base.SwingComponentDelegate;
import consulo.ide.impl.idea.ui.tabs.impl.JBEditorTabs;
import consulo.ui.Component;
import consulo.ui.Tab;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.event.TabSelectEvent;
import consulo.ui.ex.action.ActionManager;
import consulo.ui.ex.awt.tab.TabInfo;
import consulo.ui.ex.awt.tab.TabsListener;
import consulo.ui.layout.TabbedLayout;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * @author VISTALL
 * @since 2016-06-14
 */
public class DesktopTabbedLayoutImpl extends SwingComponentDelegate<JBEditorTabs> implements TabbedLayout {
    private final Map<TabInfo, Tab> myTabs = new LinkedHashMap<>();

    class MyJTabbedPane extends JBEditorTabs implements FromSwingComponentWrapper {
        public MyJTabbedPane() {
            super(null, ActionManager.getInstance(), null, null);

            setFirstTabOffset(10);

            addListener(new TabsListener() {
                @Override
                public void selectionChanged(TabInfo oldSelection, TabInfo newSelection) {
                    Tab tab = myTabs.get(newSelection);
                    if (tab != null) {
                        getListenerDispatcher(TabSelectEvent.class)
                            .onEvent(new TabSelectEvent(DesktopTabbedLayoutImpl.this, tab));
                    }
                }
            });
        }

        @Override
        public boolean isAlphabeticalMode() {
            return false;
        }

        @Override
        public boolean supportsCompression() {
            return false;
        }

        @Override
        public Component toUIComponent() {
            return DesktopTabbedLayoutImpl.this;
        }
    }

    @Override
    protected JBEditorTabs createComponent() {
        return new MyJTabbedPane();
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

        desktopTab.update();

        toAWTComponent().addTab(desktopTab.getTabInfo());
        myTabs.put(desktopTab.getTabInfo(), desktopTab);

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
        toAWTComponent().removeTab(desktopTab.getTabInfo());
        myTabs.remove(desktopTab.getTabInfo());
    }
}
