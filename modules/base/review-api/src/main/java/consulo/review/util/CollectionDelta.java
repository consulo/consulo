// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.review.util;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class CollectionDelta<T> {
    private final Collection<T> myNewCollection;
    private final Collection<T> myNewItems;
    private final Collection<T> myRemovedItems;

    public CollectionDelta(Collection<T> oldCollection, Collection<T> newCollection) {
        myNewCollection = newCollection;
        myNewItems = subtract(newCollection, asSet(oldCollection));
        myRemovedItems = subtract(oldCollection, asSet(newCollection));
    }

    public Collection<T> getNewCollection() {
        return myNewCollection;
    }

    public Collection<T> getNewItems() {
        return myNewItems;
    }

    public Collection<T> getRemovedItems() {
        return myRemovedItems;
    }

    public boolean isEmpty() {
        return myNewItems.isEmpty() && myRemovedItems.isEmpty();
    }

    private static <T> Set<T> asSet(Collection<T> collection) {
        return collection instanceof Set<T> set ? set : new HashSet<>(collection);
    }

    private static <T> Collection<T> subtract(Collection<T> collection, Set<T> excluded) {
        List<T> result = new ArrayList<>();
        for (T item : collection) {
            if (!excluded.contains(item)) {
                result.add(item);
            }
        }
        return result;
    }
}
