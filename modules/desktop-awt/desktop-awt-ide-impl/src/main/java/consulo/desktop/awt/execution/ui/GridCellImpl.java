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

package consulo.desktop.awt.execution.ui;

import consulo.application.ui.DimensionService;
import consulo.dataContext.DataProvider;
import consulo.execution.impl.internal.ui.layout.ViewContextEx;
import consulo.execution.impl.internal.ui.layout.action.CloseViewAction;
import consulo.execution.impl.internal.ui.layout.action.MinimizeViewAction;
import consulo.execution.internal.layout.GridCell;
import consulo.execution.internal.layout.Tab;
import consulo.execution.internal.layout.View;
import consulo.execution.internal.layout.ViewContext;
import consulo.execution.ui.layout.PlaceInGrid;
import consulo.ide.impl.idea.ui.tabs.impl.JBEditorTabs;
import consulo.ide.impl.idea.ui.tabs.impl.TabLabel;
import consulo.ide.impl.idea.ui.tabs.impl.singleRow.ScrollableSingleRowLayout;
import consulo.ide.impl.idea.ui.tabs.impl.singleRow.SingleRowLayout;
import consulo.ui.Point2D;
import consulo.ui.Size2D;
import consulo.ui.ex.RelativePoint;
import consulo.ui.ex.action.ActionGroup;
import consulo.ui.ex.awt.NonOpaquePanel;
import consulo.ui.ex.awt.UIUtil;
import consulo.ui.ex.awt.tab.JBTabs;
import consulo.ui.ex.awt.tab.TabInfo;
import consulo.ui.ex.awt.tab.TabsListener;
import consulo.ui.ex.awtUnsafe.TargetAWT;
import consulo.ui.ex.content.Content;
import consulo.ui.ex.content.ContentManager;
import consulo.ui.ex.popup.JBPopup;
import consulo.util.collection.MutualMap;
import consulo.util.concurrent.ActionCallback;
import consulo.util.dataholder.Key;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.HashSet;
import java.util.Set;

public class GridCellImpl implements GridCell {
    private final GridImpl myContainer;

    private final MutualMap<Content, TabInfo> myContents = new MutualMap<>(true);
    private final Set<Content> myMinimizedContents = new HashSet<>();

    private final JBTabs myTabs;
    private final GridImpl.Placeholder myPlaceholder;
    private final PlaceInGrid myPlaceInGrid;

    private final ViewContextEx myContext;
    private JBPopup myPopup;

    public GridCellImpl(ViewContextEx context, @Nonnull GridImpl container, GridImpl.Placeholder placeholder, PlaceInGrid placeInGrid) {
        myContext = context;
        myContainer = container;

        myPlaceInGrid = placeInGrid;
        myPlaceholder = placeholder;
        myTabs = new JBEditorTabs(myContext.getProject(), myContext.getActionManager(), myContext.getFocusManager(), container) {
            @Override
            protected SingleRowLayout createSingleRowLayout() {
                return new ScrollableSingleRowLayout(this);
            }

            @Override
            public boolean paintSelection() {
                return false;
            }

            @Override
            public void processDropOver(TabInfo over, RelativePoint point) {
                ((RunnerContentUiImpl) myContext).myTabs.processDropOver(over, point);
            }

            @Override
            public Image startDropOver(TabInfo tabInfo, RelativePoint point) {
                return ((RunnerContentUiImpl) myContext).myTabs.startDropOver(tabInfo, point);
            }

            @Override
            public void resetDropOver(TabInfo tabInfo) {
                ((RunnerContentUiImpl) myContext).myTabs.resetDropOver(tabInfo);
            }

            @Override
            protected TabLabel createTabLabel(TabInfo info) {
                return new TabLabel(this, info) {
                    @Override
                    public void setAlignmentToCenter(boolean toCenter) {
                        super.setAlignmentToCenter(false);
                    }
                };
            }
        }.setDataProvider(new DataProvider() {
            @Override
            @Nullable
            public Object getData(@Nonnull final Key dataId) {
                if (ViewContext.CONTENT_KEY == dataId) {
                    TabInfo target = myTabs.getTargetInfo();
                    if (target != null) {
                        return new Content[]{getContentFor(target)};
                    }
                }
                else if (ViewContext.CONTEXT_KEY == dataId) {
                    return myContext;
                }

                return null;
            }
        });
        myTabs.getPresentation().setSideComponentVertical(!context.getLayoutSettings().isToolbarHorizontal()).setStealthTabMode(true).setFocusCycle(false).setPaintFocus(true).setTabDraggingEnabled(true)
            .setSideComponentOnTabs(false);

        myTabs.addTabMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(final MouseEvent e) {
                if (UIUtil.isCloseClick(e)) {
                    // see RunnerContentUi tabMouseListener as well
                    closeOrMinimize(e);
                }
            }
        });
        rebuildPopupGroup();
        myTabs.addListener(new TabsListener.Adapter() {

            @Override
            public void beforeSelectionChanged(TabInfo oldSelection, TabInfo newSelection) {
                if (oldSelection != null && myContext.isStateBeingRestored()) {
                    saveUiState();
                }
            }

            @Override
            public void selectionChanged(final TabInfo oldSelection, final TabInfo newSelection) {
                updateSelection(myTabs.getComponent().isShowing());

                if (!myTabs.getComponent().isShowing()) {
                    return;
                }

                if (newSelection != null) {
                    newSelection.stopAlerting();
                }
            }
        });
    }

    public void rebuildPopupGroup() {
        myTabs.setPopupGroup(myContext.getCellPopupGroup(ViewContext.CELL_POPUP_PLACE), ViewContext.CELL_POPUP_PLACE, true);
    }

    public PlaceInGrid getPlaceInGrid() {
        return myPlaceInGrid;
    }

    void add(final Content content) {
        if (myContents.containsKey(content)) {
            return;
        }
        myContents.put(content, null);

        revalidateCell(() -> myTabs.addTab(createTabInfoFor(content)));

        updateSelection(myTabs.getComponent().getRootPane() != null);
    }

    void remove(Content content) {
        if (!myContents.containsKey(content)) {
            return;
        }

        final TabInfo info = getTabFor(content);
        myContents.remove(content);

        revalidateCell(() -> myTabs.removeTab(info));

        updateSelection(myTabs.getComponent().getRootPane() != null);
    }

    private void revalidateCell(Runnable contentAction) {
        if (myContents.size() == 0) {
            myPlaceholder.removeAll();
            myTabs.removeAllTabs();

            if (myPopup != null) {
                myPopup.cancel();
                myPopup = null;
            }
        }
        else {
            if (myPlaceholder.isNull()) {
                myPlaceholder.setContent(myTabs.getComponent());
            }

            contentAction.run();
        }

        restoreProportions();

        myTabs.getComponent().revalidate();
        myTabs.getComponent().repaint();
    }

    void setHideTabs(boolean hide) {
        myTabs.getPresentation().setHideTabs(hide);
    }

    private TabInfo createTabInfoFor(Content content) {
        final TabInfo tabInfo =
            updatePresentation(new TabInfo(new ProviderWrapper(content, myContext)), content).setObject(content).setPreferredFocusableComponent(content.getPreferredFocusableComponent())
                .setActionsContextComponent(content.getActionsContextComponent());

        myContents.remove(content);
        myContents.put(content, tabInfo);

        ActionGroup group = (ActionGroup) myContext.getActionManager().getAction(RunnerContentUiImpl.VIEW_TOOLBAR);
        tabInfo.setTabLabelActions(group, ViewContext.CELL_TOOLBAR_PLACE);
        tabInfo.setDragOutDelegate(((RunnerContentUiImpl) myContext).myDragOutDelegate);
        return tabInfo;
    }

    @Nullable
    private static TabInfo updatePresentation(TabInfo info, Content content) {
        if (info == null) {
            return null;
        }

        return info.
            setIcon(content.getIcon()).
            setText(content.getDisplayName()).
            setTooltipText(content.getDescription()).
            setActionsContextComponent(content.getActionsContextComponent()).
            setActions(content.getActions(), content.getPlace());
    }

    public ActionCallback select(final Content content, final boolean requestFocus) {
        final TabInfo tabInfo = myContents.getValue(content);
        return tabInfo != null ? myTabs.select(tabInfo, requestFocus) : ActionCallback.DONE;
    }

    public void processAlert(final Content content, final boolean activate) {
        if (myMinimizedContents.contains(content)) {
            return;
        }

        TabInfo tab = getTabFor(content);
        if (tab == null) {
            return;
        }
        if (myTabs.getSelectedInfo() != tab) {
            if (activate) {
                tab.fireAlert();
            }
            else {
                tab.stopAlerting();
            }
        }
    }

    public void updateTabPresentation(Content content) {
        updatePresentation(myTabs.findInfo(content), content);
    }

    public boolean isMinimized(Content content) {
        return myMinimizedContents.contains(content);
    }

    public boolean contains(Component c) {
        return myTabs.getComponent().isAncestorOf(c);
    }

    private static class ProviderWrapper extends NonOpaquePanel implements DataProvider {
        Content myContent;
        ViewContext myContext;

        private ProviderWrapper(final Content content, final ViewContext context) {
            super(new BorderLayout());
            myContent = content;
            myContext = context;
            add(content.getComponent(), BorderLayout.CENTER);
        }

        @Override
        @Nullable
        public Object getData(@Nonnull final Key dataId) {
            if (ViewContext.CONTENT_KEY == dataId) {
                return new Content[]{myContent};
            }
            else if (ViewContext.CONTEXT_KEY == dataId) {
                return myContext;
            }
            return null;
        }
    }

    @Nullable
    TabInfo getTabFor(Content content) {
        return myContents.getValue(content);
    }

    @Nonnull
    private Content getContentFor(TabInfo tab) {
        return myContents.getKey(tab);
    }

    public void setToolbarHorizontal(final boolean horizontal) {
        myTabs.getPresentation().setSideComponentVertical(!horizontal);
    }

    public ActionCallback restoreLastUiState() {
        final ActionCallback result = new ActionCallback();

        restoreProportions();

        Content[] contents = getContents();
        int window = 0;
        for (Content each : contents) {
            final View view = myContainer.getStateFor(each);
            if (view.isMinimizedInGrid()) {
                minimize(each);
            }
            window = view.getWindow();
        }
        final Tab tab = myContainer.getTab();
        final boolean detached = (tab != null && tab.isDetached(myPlaceInGrid)) || window != myContext.getWindow();
        if (detached && contents.length > 0) {
            if (tab != null) {
                tab.setDetached(myPlaceInGrid, false);
            }
            myContext.detachTo(window, this).notifyWhenDone(result);
        }
        else {
            result.setDone();
        }

        return result;
    }

    public Content[] getContents() {
        return myContents.getKeys().toArray(new Content[myContents.size()]);
    }

    @Override
    public int getContentCount() {
        return myContents.size();
    }

    public void saveUiState() {
        saveProportions();

        for (Content each : myContents.getKeys()) {
            saveState(each, false);
        }

        for (Content each : myMinimizedContents) {
            saveState(each, true);
        }

        final DimensionService service = DimensionService.getInstance();
        final Dimension size = myContext.getContentManager().getComponent().getSize();
        service.setSize(getDimensionKey(), TargetAWT.from(size), myContext.getProject());
        if (myContext.getWindow() != 0) {
            final Window frame = SwingUtilities.getWindowAncestor(myPlaceholder);
            if (frame != null) {
                service.setLocation(getDimensionKey(), TargetAWT.from(frame.getLocationOnScreen()));
            }
        }
    }

    public void saveProportions() {
        myContainer.saveSplitterProportions(myPlaceInGrid);
    }

    private void saveState(Content content, boolean minimized) {
        View state = myContext.getStateFor(content);
        state.setMinimizedInGrid(minimized);
        state.setPlaceInGrid(myPlaceInGrid);
        state.assignTab(myContainer.getTabIndex());
        state.setWindow(myContext.getWindow());
    }

    public void restoreProportions() {
        myContainer.restoreLastSplitterProportions(myPlaceInGrid);
    }

    public void updateSelection(final boolean isShowing) {
        ContentManager contentManager = myContext.getContentManager();
        if (contentManager.isDisposed()) {
            return;
        }

        for (Content each : myContents.getKeys()) {
            final TabInfo eachTab = getTabFor(each);
            boolean isSelected = eachTab != null && myTabs.getSelectedInfo() == eachTab;
            if (isSelected && isShowing) {
                contentManager.addSelectedContent(each);
            }
            else {
                contentManager.removeFromSelection(each);
            }
        }

        for (Content each : myMinimizedContents) {
            contentManager.removeFromSelection(each);
        }
    }

    public void minimize(Content[] contents) {
        myContext.saveUiState();

        for (final Content each : contents) {
            myMinimizedContents.add(each);
            remove(each);
            boolean isShowing = myTabs.getComponent().getRootPane() != null;
            updateSelection(isShowing);
            myContainer.minimize(each, () -> restore(each));
        }
    }

    @Nullable
    public Point2D getLocation() {
        return DimensionService.getInstance().getLocation(getDimensionKey(), myContext.getProject());
    }

    @Nullable
    public Size2D getSize() {
        return DimensionService.getInstance().getSize(getDimensionKey(), myContext.getProject());
    }

    private String getDimensionKey() {
        return "GridCell.Tab." + myContainer.getTab().getIndex() + "." + myPlaceInGrid.name();
    }

    public boolean isValidForCalculateProportions() {
        return getContentCount() > 0;
    }

    public void setToolbarBefore(final boolean before) {
        myTabs.getPresentation().setSideComponentBefore(before);
    }

    @Override
    public void minimize(Content content) {
        minimize(new Content[]{content});
    }

    public void closeOrMinimize(MouseEvent e) {
        TabInfo tabInfo = myTabs.findInfo(e);
        if (tabInfo == null) {
            return;
        }

        Content content = getContentFor(tabInfo);
        if (CloseViewAction.isEnabled(new Content[]{content})) {
            CloseViewAction.perform(myContext, content);
        }
        else if (MinimizeViewAction.isEnabled(myContext, getContents(), ViewContext.CELL_TOOLBAR_PLACE)) {
            minimize(content);
        }
    }

    ActionCallback restore(Content content) {
        myMinimizedContents.remove(content);
        return ActionCallback.DONE;
    }
}
