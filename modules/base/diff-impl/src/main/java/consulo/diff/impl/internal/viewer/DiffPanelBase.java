/*
 * Copyright 2000-2015 JetBrains s.r.o.
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
package consulo.diff.impl.internal.viewer;

import consulo.dataContext.DataSink;
import consulo.dataContext.UiDataProvider;
import consulo.diff.DiffContext;
import consulo.project.Project;
import consulo.ui.Component;
import consulo.ui.Space;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.layout.DockLayout;
import consulo.ui.layout.VerticalLayout;
import consulo.ui.layout.WrappedLayout;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public abstract class DiffPanelBase implements UiDataProvider {
    protected final @Nullable Project myProject;
    private final UiDataProvider myDataProvider;
    protected final DiffContext myContext;

    private final List<Component> myPersistentNotifications = new ArrayList<>();
    private final List<Component> myNotifications = new ArrayList<>();

    private final DockLayout myRootLayout;
    protected final WrappedLayout myContentPanel;
    protected final VerticalLayout myNotificationsPanel;

    private final WrappedLayout myNorthPanel;
    private final WrappedLayout mySouthPanel;

    private final Map<String, Component> myCards = new HashMap<>();

    protected @Nullable String myCurrentCard;

    @RequiredUIAccess
    public DiffPanelBase(@Nullable Project project, UiDataProvider provider, DiffContext context) {
        myProject = project;
        myDataProvider = provider;
        myContext = context;

        myContentPanel = WrappedLayout.create();
        myNotificationsPanel = VerticalLayout.create(Space.NONE);

        myNorthPanel = WrappedLayout.create();
        mySouthPanel = WrappedLayout.create();

        myRootLayout = DockLayout.create(Space.NONE);
        myRootLayout.top(myNorthPanel);
        myRootLayout.center(myContentPanel);
        myRootLayout.bottom(mySouthPanel);
        myRootLayout.putUserData(UiDataProvider.KEY, this);
    }

    public Component getComponent() {
        return myRootLayout;
    }

    @RequiredUIAccess
    public void setTopPanel(@Nullable Component component) {
        myNorthPanel.set(component);
    }

    @RequiredUIAccess
    public void setBottomPanel(@Nullable Component component) {
        mySouthPanel.set(component);
    }

    protected void addCard(String card, Component component) {
        myCards.put(card, component);
    }

    @RequiredUIAccess
    protected void setCurrentCard(String card) {
        setCurrentCard(card, true);
    }

    @RequiredUIAccess
    protected void setCurrentCard(String card, boolean keepFocus) {
        boolean restoreFocus = keepFocus && myContext.isFocused();

        myContentPanel.set(myCards.get(card));
        myCurrentCard = card;

        if (restoreFocus) {
            myContext.requestFocus();
        }
    }

    @Override
    public void uiDataSnapshot(DataSink sink) {
        sink.uiDataSnapshot(myDataProvider);
    }

    //
    // Notifications
    //

    @RequiredUIAccess
    public void setPersistentNotifications(List<Component> components) {
        myPersistentNotifications.clear();
        myPersistentNotifications.addAll(components);
        rebuildNotifications();
    }

    @RequiredUIAccess
    public void resetNotifications() {
        myNotifications.clear();
        rebuildNotifications();
    }

    @RequiredUIAccess
    public void addNotification(Component notification) {
        myNotifications.add(notification);
        myNotificationsPanel.add(notification);
    }

    @RequiredUIAccess
    private void rebuildNotifications() {
        myNotificationsPanel.removeAll();
        for (Component notification : myPersistentNotifications) {
            myNotificationsPanel.add(notification);
        }
        for (Component notification : myNotifications) {
            myNotificationsPanel.add(notification);
        }
    }
}
