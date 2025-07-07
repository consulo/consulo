/*
 * Copyright 2000-2009 JetBrains s.r.o.
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
package consulo.ui.ex.awt.tab;

import consulo.component.util.ActiveRunnable;
import consulo.dataContext.DataProvider;
import consulo.ui.ex.RelativePoint;
import consulo.ui.ex.action.ActionGroup;
import consulo.util.concurrent.ActionCallback;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;

public interface JBTabs {

    @Nonnull
    TabInfo addTab(TabInfo info, int index);

    @Nonnull
    TabInfo addTab(TabInfo info);

    @Nonnull
    ActionCallback removeTab(@Nullable TabInfo info);

    void removeAllTabs();

    @Nonnull
    JBTabs setPopupGroup(@Nonnull ActionGroup popupGroup, @Nonnull String place, boolean addNavigationGroup);

    @Nonnull
    ActionCallback select(@Nonnull TabInfo info, boolean requestFocus);

    @Nullable
    TabInfo getSelectedInfo();

    @Nonnull
    TabInfo getTabAt(int tabIndex);

    int getTabCount();

    @Nonnull
    JBTabsPresentation getPresentation();

    @Nullable
    DataProvider getDataProvider();

    @Nullable
    TabInfo getTargetInfo();

    @Nonnull
    JBTabs addTabMouseListener(@Nonnull MouseListener listener);

    JBTabs addListener(@Nonnull TabsListener listener);

    JBTabs setSelectionChangeHandler(SelectionChangeHandler handler);

    @Nonnull
    JComponent getComponent();

    @Nullable
    TabInfo findInfo(MouseEvent event);

    @Nullable
    TabInfo findInfo(Object object);

    int getIndexOf(@Nullable TabInfo tabInfo);

    void requestFocus();

    JBTabs setNavigationActionBinding(String prevActiobId, String nextActionId);

    JBTabs setNavigationActionsEnabled(boolean enabled);

    boolean isDisposed();

    void resetDropOver(TabInfo tabInfo);

    Image startDropOver(TabInfo tabInfo, RelativePoint point);

    void processDropOver(TabInfo over, RelativePoint point);

    interface SelectionChangeHandler {
        @Nonnull
        ActionCallback execute(TabInfo info, boolean requestFocus, @Nonnull ActiveRunnable doChangeSelection);
    }
}
