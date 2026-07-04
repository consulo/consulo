/*
 * Copyright 2000-2024 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
 *
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
package consulo.navigationBar.impl.internal;

import consulo.navigationBar.model.*;
import consulo.ui.UIAccess;
import consulo.ui.annotation.RequiredUIAccess;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Navigation bar view model. All state is confined to the UI thread and exposed via
 * {@link NavBarVmListener} notifications; concurrent popup requests are serialized with
 * an epoch counter (only the latest request chain is allowed to apply its result), and
 * asynchronous steps are chained with {@link CompletableFuture}.
 */
public final class NavBarVmImpl implements NavBarVm {
    private final UIAccess myUiAccess;

    private final List<NavBarVmListener> myListeners = new CopyOnWriteArrayList<>();

    private List<NavBarItemVmImpl> myItems;

    private int mySelectedIndex = -1;

    private @Nullable NavBarPopupVmImpl<DefaultNavBarPopupItem> myPopup;

    // skip context updates that do not change anything
    private @Nullable List<NavBarVmItem> myLastContextItems;

    // a stale popup request chain drops itself once it notices the epoch has moved on
    private int myPopupRequestEpoch;

    @RequiredUIAccess
    public NavBarVmImpl(List<NavBarVmItem> initialItems) {
        if (initialItems.isEmpty()) {
            throw new IllegalArgumentException("initial items must not be empty");
        }
        myUiAccess = UIAccess.current();
        myItems = mapIndexed(initialItems);
    }

    @RequiredUIAccess
    public void contextItemsChanged(List<NavBarVmItem> contextItems) {
        if (contextItems.equals(myLastContextItems)) {
            return;
        }
        myLastContextItems = contextItems;
        if (!contextItems.isEmpty()) {
            myItems = mapIndexed(contextItems);
            fireItemsChanged();
            mySelectedIndex = -1;
            fireSelectedIndexChanged();
            popupRequest(-1);
            setPopup(null);
        }
    }

    @Override
    public List<? extends NavBarItemVm> getItems() {
        return myItems;
    }

    @Override
    public int getSelectedIndex() {
        return mySelectedIndex;
    }

    @Override
    public @Nullable NavBarPopupVm<?> getPopup() {
        return myPopup;
    }

    @Override
    @RequiredUIAccess
    public List<NavBarVmItem> selection() {
        NavBarPopupVmImpl<DefaultNavBarPopupItem> popup = myPopup;
        if (popup != null) {
            List<NavBarVmItem> result = new ArrayList<>();
            for (DefaultNavBarPopupItem selected : popup.getSelectedItems()) {
                result.add(selected.getItem());
            }
            return result;
        }
        else {
            int selectedIndex = mySelectedIndex;
            List<NavBarItemVmImpl> items = myItems;
            if (selectedIndex >= 0 && selectedIndex < items.size()) {
                return List.of(items.get(selectedIndex).myItem);
            }
            // no explicit selection - fall back to the tail item, so that actions invoked
            // on the bar still get the context of the deepest element
            if (!items.isEmpty()) {
                return List.of(items.get(items.size() - 1).myItem);
            }
        }
        return List.of();
    }

    @Override
    @RequiredUIAccess
    public void shiftSelectionTo(SelectionShift shift) {
        int size = myItems.size();
        switch (shift) {
            case FIRST -> setSelectedIndex(0);
            case PREV -> setSelectedIndex(Math.floorMod(mySelectedIndex - 1 + size, size));
            case NEXT -> setSelectedIndex(Math.floorMod(mySelectedIndex + 1 + size, size));
            case LAST -> setSelectedIndex(size - 1);
        }
    }

    @Override
    @RequiredUIAccess
    public void selectTail(boolean withPopupOpen) {
        int shiftToLeft = withPopupOpen ? 1 : 0;

        List<NavBarItemVmImpl> items = myItems;
        int i = Math.max(items.size() - 1 - shiftToLeft, 0);
        items.get(i).select();

        if (withPopupOpen) {
            showPopup();
        }
    }

    @Override
    @RequiredUIAccess
    public void showPopup() {
        popupRequest(mySelectedIndex);
    }

    @Override
    public void addListener(NavBarVmListener listener) {
        myListeners.add(listener);
    }

    @Override
    public void removeListener(NavBarVmListener listener) {
        myListeners.remove(listener);
    }

    @RequiredUIAccess
    private void setSelectedIndex(int newIndex) {
        int unselected = mySelectedIndex;
        if (unselected == newIndex) {
            return;
        }
        mySelectedIndex = newIndex;

        List<NavBarItemVmImpl> items = myItems;
        // Sometimes the selected index may come here before the new items value even if the items value was set before the index.
        // This check prevents OutOfBounds exception.
        if (newIndex < items.size()) {
            if (unselected >= 0 && unselected < items.size()) {
                items.get(unselected).setSelected(false);
            }
            if (newIndex >= 0) {
                items.get(newIndex).setSelected(true);
            }
            if (myPopup != null) {
                popupRequest(newIndex);
            }
        }
        fireSelectedIndexChanged();
    }

    @RequiredUIAccess
    private void popupRequest(int index) {
        myPopupRequestEpoch++;
        handlePopupRequest(index, myPopupRequestEpoch);
    }

    @RequiredUIAccess
    private void handlePopupRequest(int index, int epoch) {
        setPopup(null); // hide previous popup
        if (index < 0) {
            return;
        }
        List<NavBarItemVmImpl> items = myItems;
        if (index >= items.size()) {
            return;
        }
        items.get(index).myItem.children().whenCompleteAsync((children, throwable) -> {
            if (epoch != myPopupRequestEpoch) {
                return;
            }
            if (throwable != null || children == null || children.isEmpty()) {
                return;
            }
            NavBarVmItem nextItem = index + 1 < items.size() ? items.get(index + 1).myItem : null;
            List<DefaultNavBarPopupItem> popupItems = new ArrayList<>(children.size());
            for (NavBarVmItem child : children) {
                popupItems.add(new DefaultNavBarPopupItem(child));
            }
            List<NavBarVmItem> pathItems = new ArrayList<>(index + 1);
            for (int i = 0; i <= index; i++) {
                pathItems.add(items.get(i).myItem);
            }
            popupLoop(pathItems, new NavBarPopupVmImpl<>(
                popupItems,
                Math.max(nextItem == null ? -1 : children.indexOf(nextItem), 0)
            ), epoch);
        }, myUiAccess);
    }

    @RequiredUIAccess
    private void popupLoop(List<NavBarVmItem> items, NavBarPopupVmImpl<DefaultNavBarPopupItem> popup, int epoch) {
        if (epoch != myPopupRequestEpoch) {
            return;
        }
        setPopup(popup);
        popup.getResult().whenCompleteAsync((selected, throwable) -> {
            if (throwable != null) {
                // cancellation of the popup
                if (epoch == myPopupRequestEpoch) {
                    setPopup(null);
                }
                return;
            }
            NavBarVmItem selectedChild = selected.getItem();
            autoExpand(selectedChild).whenCompleteAsync((expandResult, expandThrowable) -> {
                if (epoch != myPopupRequestEpoch) {
                    return;
                }
                if (expandThrowable != null || expandResult == null) {
                    return;
                }
                if (expandResult instanceof ExpandResult.NavigateTo navigateTo) {
                    fireActivationRequested(navigateTo.target());
                    return;
                }
                ExpandResult.NextPopup nextPopup = (ExpandResult.NextPopup) expandResult;
                List<NavBarVmItem> newItems = new ArrayList<>(items);
                newItems.addAll(nextPopup.expanded());
                int lastIndex = newItems.size() - 1;
                List<NavBarItemVmImpl> newItemVms = mapIndexed(newItems);
                newItemVms.get(lastIndex).setSelected(true);
                myItems = newItemVms;
                fireItemsChanged();
                mySelectedIndex = lastIndex;
                fireSelectedIndexChanged();
                List<DefaultNavBarPopupItem> popupItems = new ArrayList<>(nextPopup.children().size());
                for (NavBarVmItem child : nextPopup.children()) {
                    popupItems.add(new DefaultNavBarPopupItem(child));
                }
                popupLoop(newItems, new NavBarPopupVmImpl<>(popupItems, 0), epoch);
            }, myUiAccess);
        }, myUiAccess);
    }

    private void setPopup(@Nullable NavBarPopupVmImpl<DefaultNavBarPopupItem> popup) {
        if (myPopup == popup) {
            return;
        }
        myPopup = popup;
        for (NavBarVmListener listener : myListeners) {
            listener.popupChanged(popup);
        }
    }

    private void fireItemsChanged() {
        for (NavBarVmListener listener : myListeners) {
            listener.itemsChanged(myItems);
        }
    }

    private void fireSelectedIndexChanged() {
        for (NavBarVmListener listener : myListeners) {
            listener.selectedIndexChanged(mySelectedIndex);
        }
    }

    private void fireActivationRequested(NavBarVmItem item) {
        for (NavBarVmListener listener : myListeners) {
            listener.activationRequested(item);
        }
    }

    private List<NavBarItemVmImpl> mapIndexed(List<NavBarVmItem> items) {
        List<NavBarItemVmImpl> result = new ArrayList<>(items.size());
        for (int i = 0; i < items.size(); i++) {
            result.add(new NavBarItemVmImpl(i, items.get(i)));
        }
        return result;
    }

    private final class NavBarItemVmImpl implements NavBarItemVm {
        private final int myIndex;
        private final NavBarVmItem myItem;

        private boolean mySelected;

        NavBarItemVmImpl(int index, NavBarVmItem item) {
            myIndex = index;
            myItem = item;
        }

        @Override
        public NavBarItemPresentationData getPresentation() {
            return (NavBarItemPresentationData) myItem.getPresentation();
        }

        @Override
        public boolean isFirst() {
            return myIndex == 0;
        }

        @Override
        public boolean isLast() {
            return myIndex == myItems.size() - 1;
        }

        @Override
        public boolean isSelected() {
            return mySelected;
        }

        void setSelected(boolean value) {
            if (mySelected != value) {
                mySelected = value;
                for (NavBarVmListener listener : myListeners) {
                    listener.itemSelectionChanged(this, value);
                }
            }
        }

        @Override
        public boolean isNextSelected() {
            return myIndex == mySelectedIndex - 1;
        }

        @Override
        public boolean isInactive() {
            return mySelectedIndex != -1 && mySelectedIndex < myIndex;
        }

        @Override
        @RequiredUIAccess
        public void select() {
            setSelectedIndex(myIndex);
        }

        @Override
        @RequiredUIAccess
        public void showPopup() {
            NavBarVmImpl.this.showPopup();
        }

        @Override
        public void activate() {
            fireActivationRequested(myItem);
        }

        @Override
        public String toString() {
            return myItem.toString();
        }
    }

    private sealed interface ExpandResult {
        record NavigateTo(NavBarVmItem target) implements ExpandResult {
        }

        record NextPopup(List<NavBarVmItem> expanded, List<NavBarVmItem> children) implements ExpandResult {
        }
    }

    private static CompletableFuture<@Nullable ExpandResult> autoExpand(NavBarVmItem child) {
        return child.expand().thenCompose(result -> {
            NavBarItemExpandResultData data = (NavBarItemExpandResultData) result;
            if (data == null) {
                return CompletableFuture.completedFuture(null);
            }
            if (data.children().isEmpty() || data.navigateOnClick()) {
                return CompletableFuture.completedFuture(new ExpandResult.NavigateTo(child));
            }
            return autoExpandStep(child, List.of(), data.children(), data.navigateOnClick());
        });
    }

    // [currentItem] -- is being evaluated
    // [expanded] -- list of the elements starting from the original child argument and up to [currentItem]. Both exclusively
    // [children] -- children of the [currentItem]
    //
    // No automatic navigation in this cycle!
    // It is only allowed as reaction to a users click
    // at the popup item, i.e. at first iteration before the cycle
    private static CompletableFuture<@Nullable ExpandResult> autoExpandStep(
        NavBarVmItem currentItem,
        List<NavBarVmItem> expanded,
        List<NavBarVmItem> children,
        boolean navigateOnClick
    ) {
        switch (children.size()) {
            case 0 -> {
                // No children, [currentItem] is an only leaf on its branch, but no auto navigation allowed
                // So returning the previous state
                return CompletableFuture.completedFuture(new ExpandResult.NextPopup(expanded, List.of(currentItem)));
            }
            case 1 -> {
                if (navigateOnClick) {
                    // [currentItem] is navigation target regardless of its children count, but no auto navigation allowed
                    // So returning the previous state
                    return CompletableFuture.completedFuture(new ExpandResult.NextPopup(expanded, List.of(currentItem)));
                }
                else {
                    // Performing auto-expand, keeping invariant
                    List<NavBarVmItem> newExpanded = new ArrayList<>(expanded);
                    newExpanded.add(currentItem);
                    NavBarVmItem nextItem = children.get(0);
                    return nextItem.expand().thenCompose(fetch -> {
                        NavBarItemExpandResultData data = (NavBarItemExpandResultData) fetch;
                        if (data == null) {
                            return CompletableFuture.completedFuture(null);
                        }
                        return autoExpandStep(nextItem, newExpanded, data.children(), data.navigateOnClick());
                    });
                }
            }
            default -> {
                // [currentItem] has several children, so return it with current [expanded] trace.
                List<NavBarVmItem> newExpanded = new ArrayList<>(expanded);
                newExpanded.add(currentItem);
                return CompletableFuture.completedFuture(new ExpandResult.NextPopup(newExpanded, children));
            }
        }
    }
}
