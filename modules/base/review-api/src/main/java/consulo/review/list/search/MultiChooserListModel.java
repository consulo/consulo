// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
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
package consulo.review.list.search;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * A special list model which allows choosing multiple items from the list
 * Ensures items are not added to the list twice
 */
final class MultiChooserListModel<T> {
    private final Set<T> myChosenItems = new LinkedHashSet<>();
    private final Set<T> myItemsSet = new LinkedHashSet<>();
    private final List<T> myItems = new ArrayList<>();

    int getSize() {
        return myItems.size();
    }

    List<T> getItems() {
        return List.copyOf(myItems);
    }

    boolean isChosen(T value) {
        return myChosenItems.contains(value);
    }

    List<T> getChosenItems() {
        List<T> result = new ArrayList<>();
        for (T item : myItems) {
            if (myChosenItems.contains(item)) {
                result.add(item);
            }
        }
        return result;
    }

    void setChosen(Collection<T> toChose) {
        myChosenItems.clear();
        for (T item : toChose) {
            if (myItems.contains(item)) {
                myChosenItems.add(item);
            }
        }
    }

    void toggleChosen(T item) {
        if (!myItems.contains(item)) {
            return;
        }
        if (!myChosenItems.remove(item)) {
            myChosenItems.add(item);
        }
    }

    void add(List<T> newList) {
        for (T item : newList) {
            if (myItemsSet.add(item)) {
                myItems.add(item);
            }
        }
    }

    /**
     * Updates the list of items, ensuring that chosen items stay in the list
     */
    void retainChosenAndUpdate(List<T> newList) {
        myItems.clear();
        myItemsSet.clear();

        for (T chosen : myChosenItems) {
            myItems.add(chosen);
            myItemsSet.add(chosen);
        }

        for (T item : newList) {
            if (myItemsSet.add(item)) {
                myItems.add(item);
            }
        }
    }

    void removeAllExceptChosen() {
        myItems.clear();
        myItemsSet.clear();

        for (T chosen : myChosenItems) {
            myItems.add(chosen);
            myItemsSet.add(chosen);
        }
    }
}
