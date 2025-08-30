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
package consulo.versionControlSystem.log.impl.internal;

import consulo.disposer.Disposable;
import consulo.disposer.Disposer;
import consulo.project.Project;
import consulo.project.ui.internal.ToolWindowManagerEx;
import consulo.project.ui.wm.ToolWindowManager;
import consulo.project.ui.wm.ToolWindowManagerListener;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.content.Content;
import consulo.ui.ex.content.TabbedContent;
import consulo.ui.ex.content.event.ContentManagerEvent;
import consulo.ui.ex.content.event.ContentManagerListener;
import consulo.ui.ex.toolWindow.ToolWindow;
import consulo.util.collection.ContainerUtil;
import consulo.versionControlSystem.VcsToolWindow;
import consulo.versionControlSystem.log.impl.internal.data.VcsLogFilterer;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import kava.beans.PropertyChangeEvent;
import kava.beans.PropertyChangeListener;

public class VcsLogTabsWatcher implements Disposable {
    private static final String TOOLWINDOW_ID = VcsToolWindow.ID;

    @Nonnull
    private final PostponableLogRefresher myRefresher;

    @Nonnull
    private final ToolWindowManagerEx myToolWindowManager;
    @Nonnull
    private final MyRefreshPostponedEventsListener myPostponedEventsListener;
    @Nullable
    private ToolWindow myToolWindow;

    public VcsLogTabsWatcher(@Nonnull Project project, @Nonnull PostponableLogRefresher refresher, @Nonnull Disposable parentDisposable) {
        myRefresher = refresher;
        myToolWindowManager = ToolWindowManagerEx.getInstanceEx(project);

        myPostponedEventsListener = new MyRefreshPostponedEventsListener();
        project.getMessageBus().connect(this).subscribe(ToolWindowManagerListener.class, myPostponedEventsListener);

        installContentListener();

        Disposer.register(parentDisposable, this);
    }

    @Nullable
    private String getSelectedTabName() {
        if (myToolWindow != null && myToolWindow.isVisible()) {
            Content content = myToolWindow.getContentManager().getSelectedContent();
            if (content != null) {
                return content.getTabName();
            }
        }
        return null;
    }

    @Nonnull
    public Disposable addTabToWatch(@Nonnull String contentTabName, @Nonnull VcsLogFilterer filterer) {
        return myRefresher.addLogWindow(new VcsLogTab(filterer, contentTabName));
    }

    private void installContentListener() {
        ToolWindow window = myToolWindowManager.getToolWindow(TOOLWINDOW_ID);
        if (window != null) {
            myToolWindow = window;
            myToolWindow.getContentManager().addContentManagerListener(myPostponedEventsListener, this);
        }
    }

    @Override
    public void dispose() {
        if (myToolWindow != null) {
            for (Content content : myToolWindow.getContentManager().getContents()) {
                if (content instanceof TabbedContent) {
                    content.removePropertyChangeListener(myPostponedEventsListener);
                }
            }
        }
    }

    public class VcsLogTab extends PostponableLogRefresher.VcsLogWindow {
        @Nonnull
        private final String myTabName;

        public VcsLogTab(@Nonnull VcsLogFilterer filterer, @Nonnull String tabName) {
            super(filterer);
            myTabName = tabName;
        }

        @Override
        public boolean isVisible() {
            String selectedTab = getSelectedTabName();
            return selectedTab != null && myTabName.equals(selectedTab);
        }
    }

    private class MyRefreshPostponedEventsListener implements ToolWindowManagerListener, PropertyChangeListener, ContentManagerListener {

        private void selectionChanged() {
            String tabName = getSelectedTabName();
            if (tabName != null) {
                selectionChanged(tabName);
            }
        }

        private void selectionChanged(String tabName) {
            PostponableLogRefresher.VcsLogWindow logWindow = ContainerUtil.find(myRefresher.getLogWindows(),
                window -> window instanceof VcsLogTab && ((VcsLogTab) window).myTabName.equals(tabName));
            if (logWindow != null) {
                myRefresher.filtererActivated(logWindow.getFilterer(), false);
            }
        }

        @RequiredUIAccess
        @Override
        public void selectionChanged(ContentManagerEvent event) {
            if (ContentManagerEvent.ContentOperation.add.equals(event.getOperation())) {
                selectionChanged(event.getContent().getTabName());
            }
        }

        @RequiredUIAccess
        @Override
        public void contentAdded(ContentManagerEvent event) {
            Content content = event.getContent();
            if (content instanceof TabbedContent) {
                content.addPropertyChangeListener(this);
            }
        }

        @RequiredUIAccess
        @Override
        public void contentRemoved(ContentManagerEvent event) {
            Content content = event.getContent();
            if (content instanceof TabbedContent) {
                content.removePropertyChangeListener(this);
            }
        }

        @Override
        public void stateChanged(ToolWindowManager toolWindowManager) {
            selectionChanged();
        }

        @Override
        public void toolWindowRegistered(@Nonnull String id) {
            if (id.equals(TOOLWINDOW_ID)) {
                installContentListener();
            }
        }

        @Override
        public void propertyChange(PropertyChangeEvent evt) {
            if (evt.getPropertyName().equals(Content.PROP_COMPONENT)) {
                selectionChanged();
            }
        }
    }
}
