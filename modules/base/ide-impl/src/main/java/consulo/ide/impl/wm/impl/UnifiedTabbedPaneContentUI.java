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
package consulo.ide.impl.wm.impl;

import consulo.localize.LocalizeValue;
import consulo.ui.Button;
import consulo.ui.ButtonStyle;
import consulo.ui.Component;
import consulo.ui.Space;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.content.Content;
import consulo.ui.ex.content.ContentManager;
import consulo.ui.ex.content.ContentUI;
import consulo.ui.ex.content.event.ContentManagerEvent;
import consulo.ui.ex.content.event.ContentManagerListener;
import consulo.ui.ex.localize.UILocalize;
import consulo.ui.layout.DockLayout;
import consulo.ui.layout.HorizontalLayout;
import consulo.ui.style.ComponentColors;
import org.jspecify.annotations.Nullable;

/**
 * Tabs of a content manager for the frontends which render {@link consulo.ui} components rather than swing -
 * a row of tab buttons over the view which is selected. A lone content is the whole of what the manager holds
 * and needs no tab of its own.
 */
public class UnifiedTabbedPaneContentUI implements ContentUI, ContentManagerListener {
    private final DockLayout myRoot = DockLayout.create();
    private final HorizontalLayout myTabs = HorizontalLayout.create(Space.X_SMALL);

    {
        myRoot.top(myTabs);
    }

    private @Nullable ContentManager myManager;

    @Override
    public void setManager(ContentManager manager) {
        myManager = manager;
        manager.addContentManagerListener(this);
    }

    @Override
    @RequiredUIAccess
    public void selectionChanged(ContentManagerEvent event) {
        updateSelection();
    }

    @Override
    @RequiredUIAccess
    public void contentAdded(ContentManagerEvent event) {
        updateSelection();
    }

    @Override
    @RequiredUIAccess
    public void contentRemoved(ContentManagerEvent event) {
        updateSelection();
    }

    @RequiredUIAccess
    private void updateSelection() {
        rebuildTabs();

        Content content = myManager == null ? null : myManager.getSelectedContent();
        Component component = content == null ? null : content.getUIComponent();
        if (component != null) {
            myRoot.center(component);
        }
    }

    @RequiredUIAccess
    private void rebuildTabs() {
        myTabs.removeAll();

        ContentManager manager = myManager;
        if (manager == null || manager.getContentCount() < 2) {
            return;
        }

        Content selected = manager.getSelectedContent();

        for (Content content : manager.getContents()) {
            myTabs.add(createTab(manager, content, content == selected));
        }
    }

    @RequiredUIAccess
    private Component createTab(ContentManager manager, Content content, boolean selected) {
        Button tab = Button.create(LocalizeValue.of(content.getTabName()));
        tab.setIcon(content.getIcon());
        tab.addStyle(ButtonStyle.BORDERLESS);
        tab.addClickListener(event -> manager.setSelectedContent(content, true));

        HorizontalLayout tabLayout = HorizontalLayout.create(Space.NONE);
        tabLayout.add(tab);

        if (selected) {
            tabLayout.setBackgroundColor(ComponentColors.TABBED_LAYOUT_SELECTED_BACKGROUND);
        }

        return tabLayout;
    }

    @Override
    public Component getUIComponent() {
        return myRoot;
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
