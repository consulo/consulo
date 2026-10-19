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
package consulo.ide.impl.idea.ide.actions.searcheverywhere;

import consulo.application.util.diff.Diff;
import consulo.application.util.diff.FilesTooBigForDiffException;
import consulo.logging.Logger;
import consulo.searchEverywhere.SearchEverywhereContributor;
import consulo.searchEverywhere.SearchEverywhereFoundElementInfo;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.model.FlatDataModel;
import consulo.ui.model.MutableFlatDataModel;
import org.jspecify.annotations.Nullable;

import java.util.*;
import java.util.stream.Collectors;

/**
 * @author VISTALL
 * @since 2026-10-19
 */
final class UnifiedSearchListModel {
    private static final Logger LOG = Logger.getInstance(UnifiedSearchListModel.class);

    static final Object MORE_ELEMENT = new Object();
    static final Object GROUP_TITLE = new Object();

    private final MutableFlatDataModel<SearchEverywhereFoundElementInfo> myModel = FlatDataModel.of(new ArrayList<>());
    private final List<SearchEverywhereFoundElementInfo> myElements = new ArrayList<>();
    private final Map<SearchEverywhereContributor<?>, SearchEverywhereFoundElementInfo> myTitles = new IdentityHashMap<>();

    private boolean myShowGroupTitles;
    private boolean myResultsExpired;

    static boolean isTitle(SearchEverywhereFoundElementInfo info) {
        return info.getElement() == GROUP_TITLE;
    }

    MutableFlatDataModel<SearchEverywhereFoundElementInfo> getDataModel() {
        return myModel;
    }

    boolean isResultsExpired() {
        return myResultsExpired;
    }

    void expireResults() {
        myResultsExpired = true;
    }

    int getSize() {
        return myElements.size();
    }

    boolean isEmpty() {
        return myElements.isEmpty();
    }

    SearchEverywhereFoundElementInfo getInfo(int index) {
        return myElements.get(index);
    }

    int indexOf(SearchEverywhereFoundElementInfo info) {
        for (int i = 0; i < myElements.size(); i++) {
            if (myElements.get(i) == info) {
                return i;
            }
        }
        return -1;
    }

    List<SearchEverywhereFoundElementInfo> getInfos() {
        return Collections.unmodifiableList(myElements);
    }

    Collection<Object> getFoundItems(SearchEverywhereContributor<?> contributor) {
        return myElements.stream()
            .filter(info -> info.getContributor() == contributor && info.getElement() != MORE_ELEMENT)
            .map(SearchEverywhereFoundElementInfo::getElement)
            .collect(Collectors.toList());
    }

    boolean hasMoreElements(SearchEverywhereContributor<?> contributor) {
        return myElements.stream().anyMatch(info -> info.getElement() == MORE_ELEMENT && info.getContributor() == contributor);
    }

    @RequiredUIAccess
    void addElements(List<? extends SearchEverywhereFoundElementInfo> items) {
        if (items.isEmpty()) {
            return;
        }

        Map<SearchEverywhereContributor<?>, List<SearchEverywhereFoundElementInfo>> itemsMap = new LinkedHashMap<>();
        for (SearchEverywhereFoundElementInfo info : items) {
            itemsMap.computeIfAbsent(info.getContributor(), contributor -> new ArrayList<>()).add(info);
        }
        itemsMap.values().forEach(list -> list.sort(Comparator.comparingInt(SearchEverywhereFoundElementInfo::getPriority).reversed()));

        List<SearchEverywhereFoundElementInfo> keptItems = new ArrayList<>();
        if (myResultsExpired) {
            retainContributors(itemsMap.keySet());
            clearMoreItems();

            itemsMap.forEach((contributor, list) -> {
                Object[] oldItems = getFoundItems(contributor).toArray();
                Object[] newItems = list.stream().map(SearchEverywhereFoundElementInfo::getElement).toArray();
                try {
                    Diff.Change change = Diff.buildChanges(oldItems, newItems);
                    applyChange(change, contributor, list);
                    collectKeptItems(contributor, list, keptItems);
                }
                catch (FilesTooBigForDiffException e) {
                    LOG.error("Cannot calculate diff for updated search results");
                }
            });
            myResultsExpired = false;
        }
        else {
            itemsMap.values().forEach(list -> list.forEach(this::insertSorted));
        }

        syncModel();

        for (SearchEverywhereFoundElementInfo kept : keptItems) {
            myModel.update(kept);
        }
    }

    @RequiredUIAccess
    void removeElement(Object item, SearchEverywhereContributor<?> contributor) {
        int index = firstIndexOf(contributor);
        if (index < 0) {
            return;
        }

        while (index < myElements.size() && myElements.get(index).getContributor() == contributor) {
            if (item.equals(myElements.get(index).getElement())) {
                removeAt(index);
                syncModel();
                return;
            }
            index++;
        }
    }

    @RequiredUIAccess
    void setHasMore(SearchEverywhereContributor<?> contributor, boolean newVal) {
        int index = lastIndexOf(contributor);
        if (index < 0) {
            return;
        }

        boolean alreadyHas = isMoreElement(index);
        if (alreadyHas && !newVal) {
            removeAt(index);
        }

        if (!alreadyHas && newVal) {
            insert(index + 1, new SearchEverywhereFoundElementInfo(MORE_ELEMENT, 0, contributor));
        }
        syncModel();
    }

    @RequiredUIAccess
    void clear() {
        myElements.clear();
        myModel.removeAll();
    }

    @RequiredUIAccess
    void setShowGroupTitles(boolean showGroupTitles) {
        if (myShowGroupTitles != showGroupTitles) {
            myShowGroupTitles = showGroupTitles;
            syncModel();
        }
    }

    @Nullable SearchEverywhereFoundElementInfo firstItemOfGroup(SearchEverywhereContributor<?> contributor) {
        int index = firstIndexOf(contributor);
        return index < 0 ? null : myElements.get(index);
    }

    boolean isMoreElement(int index) {
        return myElements.get(index).getElement() == MORE_ELEMENT;
    }

    @SuppressWarnings("unchecked")
    <Item> SearchEverywhereContributor<Item> getContributorForIndex(int index) {
        return (SearchEverywhereContributor<Item>) myElements.get(index).getContributor();
    }

    boolean isGroupFirstItem(int index) {
        return index == 0 || myElements.get(index).getContributor() != myElements.get(index - 1).getContributor();
    }

    int getItemsForContributor(SearchEverywhereContributor<?> contributor) {
        int first = firstIndexOf(contributor);
        int last = lastIndexOf(contributor);
        if (first < 0) {
            return 0;
        }
        if (isMoreElement(last)) {
            last -= 1;
        }
        return last - first + 1;
    }

    Map<SearchEverywhereContributor<?>, Collection<SearchEverywhereFoundElementInfo>> getFoundElementsMap() {
        return myElements.stream()
            .filter(info -> info.getElement() != MORE_ELEMENT)
            .collect(Collectors.groupingBy(SearchEverywhereFoundElementInfo::getContributor, Collectors.toCollection(ArrayList::new)));
    }

    @RequiredUIAccess
    void refresh(SearchEverywhereFoundElementInfo info) {
        if (myModel.indexOf(info) >= 0) {
            myModel.update(info);
        }
    }

    @RequiredUIAccess
    private void insertSorted(SearchEverywhereFoundElementInfo info) {
        SearchEverywhereContributor<?> contributor = info.getContributor();
        int first = firstIndexOf(contributor);
        if (first < 0) {
            insert(getInsertionPoint(contributor), info);
            return;
        }

        int index = first;
        while (index < myElements.size()
            && myElements.get(index).getContributor() == contributor
            && !isMoreElement(index)
            && myElements.get(index).getPriority() >= info.getPriority()) {
            index++;
        }
        insert(index, info);
    }

    @RequiredUIAccess
    private void retainContributors(Collection<SearchEverywhereContributor<?>> retainContributors) {
        for (int i = myElements.size() - 1; i >= 0; i--) {
            if (!retainContributors.contains(myElements.get(i).getContributor())) {
                removeAt(i);
            }
        }
    }

    @RequiredUIAccess
    private void clearMoreItems() {
        for (int i = myElements.size() - 1; i >= 0; i--) {
            if (isMoreElement(i)) {
                removeAt(i);
            }
        }
    }

    @RequiredUIAccess
    private void applyChange(
        Diff.Change change,
        SearchEverywhereContributor<?> contributor,
        List<SearchEverywhereFoundElementInfo> newItems
    ) {
        int firstItemIndex = firstIndexOf(contributor);
        if (firstItemIndex < 0) {
            firstItemIndex = getInsertionPoint(contributor);
        }

        for (Diff.Change ch : toRevertedList(change)) {
            for (int i = ch.deleted - 1; i >= 0; i--) {
                removeAt(firstItemIndex + ch.line0 + i);
            }

            for (int i = 0; i < ch.inserted; i++) {
                insert(firstItemIndex + ch.line0 + i, newItems.get(ch.line1 + i));
            }
        }
    }

    private void collectKeptItems(
        SearchEverywhereContributor<?> contributor,
        List<SearchEverywhereFoundElementInfo> newItems,
        List<SearchEverywhereFoundElementInfo> keptItems
    ) {
        int first = firstIndexOf(contributor);
        if (first < 0) {
            return;
        }

        for (int i = 0; i < newItems.size() && first + i < myElements.size(); i++) {
            SearchEverywhereFoundElementInfo kept = myElements.get(first + i);
            SearchEverywhereFoundElementInfo found = newItems.get(i);
            if (kept != found && kept.getContributor() == contributor && Objects.equals(kept.getElement(), found.getElement())) {
                kept.setPresentation(found.getPresentation());
                keptItems.add(kept);
            }
        }
    }

    private static List<Diff.Change> toRevertedList(Diff.Change change) {
        List<Diff.Change> res = new ArrayList<>();
        while (change != null) {
            res.add(0, change);
            change = change.link;
        }
        return res;
    }

    @RequiredUIAccess
    private void syncModel() {
        List<SearchEverywhereFoundElementInfo> desired = new ArrayList<>(myElements.size() + myTitles.size());
        SearchEverywhereContributor<?> previous = null;
        for (SearchEverywhereFoundElementInfo info : myElements) {
            SearchEverywhereContributor<?> contributor = info.getContributor();
            if (myShowGroupTitles && contributor != previous) {
                desired.add(myTitles.computeIfAbsent(contributor, c -> new SearchEverywhereFoundElementInfo(GROUP_TITLE, 0, c)));
            }
            desired.add(info);
            previous = contributor;
        }

        Set<SearchEverywhereFoundElementInfo> desiredSet = Collections.newSetFromMap(new IdentityHashMap<>());
        desiredSet.addAll(desired);

        for (int i = myModel.getSize() - 1; i >= 0; i--) {
            SearchEverywhereFoundElementInfo info = myModel.get(i);
            if (!desiredSet.contains(info)) {
                myModel.remove(info);
            }
        }

        for (int i = 0; i < desired.size(); i++) {
            SearchEverywhereFoundElementInfo info = desired.get(i);
            if (i < myModel.getSize() && myModel.get(i) == info) {
                continue;
            }
            if (myModel.indexOf(info) >= 0) {
                myModel.remove(info);
            }
            myModel.add(info, i);
        }
    }

    private void insert(int index, SearchEverywhereFoundElementInfo info) {
        myElements.add(index, info);
    }

    private void removeAt(int index) {
        myElements.remove(index);
    }

    private int firstIndexOf(SearchEverywhereContributor<?> contributor) {
        for (int i = 0; i < myElements.size(); i++) {
            if (myElements.get(i).getContributor() == contributor) {
                return i;
            }
        }
        return -1;
    }

    private int lastIndexOf(SearchEverywhereContributor<?> contributor) {
        for (int i = myElements.size() - 1; i >= 0; i--) {
            if (myElements.get(i).getContributor() == contributor) {
                return i;
            }
        }
        return -1;
    }

    private int getInsertionPoint(SearchEverywhereContributor<?> contributor) {
        if (myElements.isEmpty()) {
            return 0;
        }

        int index = lastIndexOf(contributor);
        if (index >= 0) {
            return isMoreElement(index) ? index : index + 1;
        }

        int weight = contributor.getSortWeight();
        for (int i = 0; i < myElements.size(); i++) {
            if (myElements.get(i).getContributor().getSortWeight() > weight) {
                return i;
            }
        }
        return myElements.size();
    }
}
