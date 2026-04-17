// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
/*
 * Copyright 2013-2025 consulo.io
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
package consulo.collaboration.util;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Set;

public final class CollectionDelta<T> {
    private final Collection<T> myNewItems;
    private final Collection<T> myRemovedItems;
    private final Collection<T> myNewCollection;

    @SuppressWarnings("unchecked")
    public CollectionDelta(Collection<T> oldCollection, Collection<T> newCollection) {
        myNewCollection = newCollection;

        Set<T> oldSet = oldCollection instanceof Set ? (Set<T>) oldCollection : new LinkedHashSet<>(oldCollection);
        Set<T> newSet = newCollection instanceof Set ? (Set<T>) newCollection : new LinkedHashSet<>(newCollection);

        Set<T> added = new LinkedHashSet<>(newSet);
        added.removeAll(oldSet);
        myNewItems = added;

        Set<T> removed = new LinkedHashSet<>(oldSet);
        removed.removeAll(newSet);
        myRemovedItems = removed;
    }

    public Collection<T> getNewItems() {
        return myNewItems;
    }

    public Collection<T> getRemovedItems() {
        return myRemovedItems;
    }

    public Collection<T> getNewCollection() {
        return myNewCollection;
    }

    public boolean isEmpty() {
        return myNewItems.isEmpty() && myRemovedItems.isEmpty();
    }
}
