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

import consulo.dataContext.DataProvider;
import consulo.disposer.Disposable;
import consulo.disposer.Disposer;
import consulo.execution.impl.internal.ui.layout.TabImpl;
import consulo.execution.impl.internal.ui.layout.ViewContextEx;
import consulo.execution.internal.layout.*;
import consulo.execution.ui.layout.PlaceInGrid;
import consulo.ide.impl.idea.openapi.ui.ThreeComponentsSplitter;
import consulo.ui.ex.awt.NullableComponent;
import consulo.ui.ex.awt.Splitter;
import consulo.ui.ex.awt.Wrapper;
import consulo.ui.ex.awt.tab.JBTabsPresentation;
import consulo.ui.ex.content.Content;
import consulo.util.concurrent.ActionCallback;
import consulo.util.dataholder.Key;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import javax.swing.*;
import java.awt.*;
import java.util.List;
import java.util.*;

public class GridImpl extends Wrapper implements Grid, Disposable, DataProvider {
    private final ThreeComponentsSplitter myTopSplit = new ThreeComponentsSplitter(false, true);
    private final Splitter mySplitter = new Splitter(true);

    private final Map<PlaceInGrid, GridCellImpl> myPlaceInGrid2Cell = new EnumMap<>(PlaceInGrid.class);

    private final String mySessionName;

    private final List<Content> myContents = new ArrayList<>();
    private final Map<Content, GridCellImpl> myContent2Cell = new HashMap<>();

    private final Comparator<Content> myContentComparator =
        (o1, o2) -> getCellFor(o1).getPlaceInGrid().compareTo(getCellFor(o2).getPlaceInGrid());

    private final ViewContextEx myViewContext;

    public GridImpl(ViewContextEx viewContext, String sessionName) {
        myViewContext = viewContext;
        mySessionName = sessionName;

        Disposer.register(myViewContext, this);
        Disposer.register(this, myTopSplit);

        Placeholder left = new Placeholder();
        myPlaceInGrid2Cell.put(PlaceInGrid.left, new GridCellImpl(myViewContext, this, left, PlaceInGrid.left));
        Placeholder center = new Placeholder();
        myPlaceInGrid2Cell.put(PlaceInGrid.center, new GridCellImpl(myViewContext, this, center, PlaceInGrid.center));
        Placeholder right = new Placeholder();
        myPlaceInGrid2Cell.put(PlaceInGrid.right, new GridCellImpl(myViewContext, this, right, PlaceInGrid.right));
        Placeholder bottom = new Placeholder();
        myPlaceInGrid2Cell.put(PlaceInGrid.bottom, new GridCellImpl(myViewContext, this, bottom, PlaceInGrid.bottom));

        setContent(mySplitter);
        setOpaque(false);
        setFocusCycleRoot(true);


        myTopSplit.setFirstComponent(left);
        myTopSplit.setInnerComponent(center);
        myTopSplit.setLastComponent(right);
        myTopSplit.setMinSize(48);
        mySplitter.setFirstComponent(myTopSplit);
        mySplitter.setSecondComponent(bottom);
    }

    @Override
    public void addNotify() {
        super.addNotify();

        processAddToUi(true);
    }

    @Override
    public void removeNotify() {
        super.removeNotify();

        processRemoveFromUi();
    }

    public void processAddToUi(boolean restoreProportions) {
        if (restoreProportions) {
            for (final GridCellImpl cell : myPlaceInGrid2Cell.values()) {
                cell.restoreProportions();
            }
        }

        updateSelection(true);
    }


    public void processRemoveFromUi() {
        if (Disposer.isDisposed(this)) {
            return;
        }

        updateSelection(false);
    }

    private void updateSelection(boolean isShowing) {
        for (GridCellImpl each : myPlaceInGrid2Cell.values()) {
            each.updateSelection(isShowing);
        }
    }


    void add(final Content content) {
        GridCellImpl cell = getCellFor(content);
        cell.add(content);
        myContents.add(content);
        myContent2Cell.put(content, cell);
        Collections.sort(myContents, myContentComparator);
    }

    void remove(final Content content) {
        getCellFor(content).remove(content);
        myContents.remove(content);
        myContent2Cell.remove(content);
    }

    public void setToolbarHorizontal(boolean horizontal) {
        for (final GridCellImpl cell : myPlaceInGrid2Cell.values()) {
            cell.setToolbarHorizontal(horizontal);
        }
    }

    @Override
    public GridCellImpl getCellFor(final Content content) {
        // check if the content is already in some cell
        GridCellImpl current = myContent2Cell.get(content);
        if (current != null) {
            return current;
        }
        // view may be shared between several contents with the same ID in different cells
        // (temporary contents like "Dump Stack" or "Console Result")
        View view = getStateFor(content);
        final GridCellImpl cell = myPlaceInGrid2Cell.get(view.getPlaceInGrid());
        assert cell != null : "Unknown place in grid: " + view.getPlaceInGrid().name();
        return cell;
    }

    View getStateFor(final Content content) {
        return myViewContext.getStateFor(content);
    }

    public boolean updateGridUI() {
        var isHidden = myViewContext.getLayoutSettings().isTabLabelsHidden();
        for (final GridCellImpl cell : myPlaceInGrid2Cell.values()) {
            if (isHidden) {
                cell.setHideTabs(cell.getContentCount() == 1);
            }
            else {
                cell.setHideTabs(myContents.size() == 1);
            }
        }

        final Content onlyContent = myContents.get(0);

        return onlyContent.getSearchComponent() != null;
    }

    public boolean isEmpty() {
        return myContent2Cell.isEmpty();
    }

    public ActionCallback restoreLastUiState() {
        final ActionCallback result = new ActionCallback(myPlaceInGrid2Cell.values().size());
        for (final GridCellImpl cell : myPlaceInGrid2Cell.values()) {
            cell.restoreLastUiState().notifyWhenDone(result);
        }

        return result;
    }

    public void saveUiState() {
        for (final GridCellImpl cell : myPlaceInGrid2Cell.values()) {
            cell.saveUiState();
        }
    }

    @Nullable
    public Tab getTabIndex() {
        return getTab();
    }

    public ActionCallback select(final Content content, final boolean requestFocus) {
        return getCellFor(content).select(content, requestFocus);
    }

    public void processAlert(final Content content, final boolean activate) {
        GridCellImpl cell = getCellFor(content);
        cell.processAlert(content, activate);
    }

    @Nullable
    public GridCellImpl findCell(final Content content) {
        return myContent2Cell.get(content);
    }

    public void rebuildTabPopup() {
        final List<Content> contents = getContents();
        for (Content each : contents) {
            GridCellImpl cell = findCell(each);
            if (cell != null) {
                cell.rebuildPopupGroup();
            }
        }
    }

    public boolean isMinimized(Content content) {
        return getCellFor(content).isMinimized(content);
    }


    static class Placeholder extends Wrapper implements NullableComponent {

        private JComponent myContent;

        public CellTransform.Restore detach() {
            if (getComponentCount() == 1) {
                myContent = (JComponent) getComponent(0);
                removeAll();
            }

            if (getParent() instanceof JComponent) {
                ((JComponent) getParent()).revalidate();
                getParent().repaint();
            }

            return () -> {
                if (myContent != null) {
                    setContent(myContent);
                    myContent = null;
                }
                return ActionCallback.DONE;
            };
        }

        @Override
        public void doLayout() {
            super.doLayout();
            Component child = getComponentCount() == 1 ? getComponent(0) : null;
            if (child instanceof JBTabsPresentation) {
                if (!((JBTabsPresentation) child).isHideTabs()) {
                    Rectangle bounds = child.getBounds();
                    bounds.y--;
                    bounds.height++;
                    child.setBounds(bounds);
                }
            }
        }
    }

    @Override
    public void dispose() {

    }

    void saveSplitterProportions(final PlaceInGrid placeInGrid) {
        if (getRootPane() == null) {
            return;
        }
        final Rectangle bounds = getBounds();
        if (bounds.width == 0 && bounds.height == 0) {
            return;
        }

        final GridCellImpl cell = myPlaceInGrid2Cell.get(placeInGrid);

        if (!cell.isValidForCalculateProportions()) {
            return;
        }

        final TabImpl tab = (TabImpl) getTab();

        if (tab != null) {
            switch (placeInGrid) {
                case left:
                    tab.setLeftProportion(getLeftProportion());
                    break;
                case right:
                    tab.setRightProportion(getRightProportion());
                    break;
                case bottom:
                    tab.setBottomProportion(getBottomPropertion());
                case center:
                    break;
            }
        }
    }

    @Nullable
    public Tab getTab() {
        return myViewContext.getTabFor(this);
    }

    void restoreLastSplitterProportions(PlaceInGrid placeInGrid) {
        if (getRootPane() == null) {
            return;
        }
        if (!RunnerContentUiImpl.ensureValid(this)) {
            return;
        }

        final TabImpl tab = (TabImpl) getTab();
        if (tab != null) {
            switch (placeInGrid) {
                case left:
                    setLeftProportion(tab.getLeftProportion());
                    break;
                case right:
                    setRightProportion(tab.getRightProportion());
                    break;
                case bottom:
                    mySplitter.setProportion(tab.getBottomProportion());
                    break;
                case center:
                    break;
            }
        }
    }


    float getLeftProportion() {
        final float totalSize = myTopSplit.getOrientation() ? myTopSplit.getHeight() : myTopSplit.getWidth();
        final float componentSize = myTopSplit.getFirstSize();

        return componentSize / (totalSize - 2.0f * myTopSplit.getDividerWidth());
    }

    void setLeftProportion(float proportion) {
        final int totalSize = myTopSplit.getOrientation() ? myTopSplit.getHeight() : myTopSplit.getWidth();
        myTopSplit.setFirstSize((int) (proportion * (float) (totalSize - 2 * myTopSplit.getDividerWidth())));
    }

    float getRightProportion() {
        final float totalSize = myTopSplit.getOrientation() ? myTopSplit.getHeight() : myTopSplit.getWidth();
        final float componentSize = myTopSplit.getLastSize();

        return componentSize / (totalSize - 2.0f * myTopSplit.getDividerWidth());
    }

    float getBottomPropertion() {
        final float totalSize = mySplitter.getOrientation() ? mySplitter.getHeight() : mySplitter.getWidth();
        final float componentSize =
            mySplitter.getOrientation() ? mySplitter.getFirstComponent().getHeight() : mySplitter.getFirstComponent().getWidth();

        return componentSize / (totalSize - mySplitter.getDividerWidth());
    }

    void setRightProportion(float proportion) {
        final int componentSize = myTopSplit.getOrientation() ? myTopSplit.getHeight() : myTopSplit.getWidth();
        myTopSplit.setLastSize((int) (proportion * (float) (componentSize - 2 * myTopSplit.getDividerWidth())));
    }

    public void setToolbarBefore(boolean before) {
        for (final GridCellImpl cell : myPlaceInGrid2Cell.values()) {
            cell.setToolbarBefore(before);
        }
    }

    @Override
    public List<Content> getContents() {
        return myContents;
    }

    public void minimize(final Content content, final CellTransform.Restore restore) {
        myViewContext.getCellTransform().minimize(content, restore::restoreInGrid);
    }

    @Override
    @Nullable
    public Object getData(@Nonnull final Key<?> dataId) {
        if (ViewContext.CONTEXT_KEY == dataId) {
            return myViewContext;
        }
        else if (ViewContext.CONTENT_KEY == dataId) {
            List<Content> contents = getContents();
            return contents.toArray(new Content[contents.size()]);
        }
        return null;
    }
}
