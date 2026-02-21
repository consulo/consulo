/*
 * Copyright 2000-2010 JetBrains s.r.o.
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
package consulo.util.collection;

import consulo.annotation.UsedInPlugin;

import java.util.ArrayList;
import java.util.List;

/**
 * @author irengrig
 */
@UsedInPlugin
public class BigArray<T> implements StepList<T> {
    private final int mySize2Power;
    private final List<List<T>> myList;
    private int myPack;
    private int mySize;

    // pack size = 2^size2Power
    public BigArray(int size2Power) {
        assert (size2Power > 1) && (size2Power < 16);
        mySize2Power = size2Power;
        myList = new ArrayList<>();
        myPack = (int) Math.pow(2, size2Power);
        mySize = 0;
    }

    @Override
    public T get(int idx) {
        int itemNumber = idx >> mySize2Power;
        return myList.get(itemNumber).get(idx ^ (itemNumber << mySize2Power));
    }

    @Override
    public void add(T t) {
        List<T> commits;
        if (myList.isEmpty() || (myList.get(myList.size() - 1).size() == myPack)) {
            commits = new ArrayList<>(myPack);
            myList.add(commits);
        }
        else {
            commits = myList.get(myList.size() - 1);
        }
        ++mySize;
        commits.add(t);
    }

    @Override
    public ReadonlyList<T> cut(int idxFromIncluded) {
        if (idxFromIncluded >= getSize()) {
            return EMPTY;
        }
        int itemNumber = idxFromIncluded >> mySize2Power;
        int insideIdx = idxFromIncluded ^ (itemNumber << mySize2Power);

        List<T> start = myList.get(itemNumber);
        NotRegularReadonlyList<T> result = new NotRegularReadonlyList<>(
            new ArrayList<>(myList.subList(itemNumber + 1, myList.size())),
            mySize2Power,
            start.subList(insideIdx, start.size())
        );
        myList.set(itemNumber, new ArrayList<>(start.subList(0, insideIdx)));
        for (int i = myList.size() - 1; i > itemNumber; --i) {
            myList.remove(i);
        }
        mySize = myList.isEmpty() ? 0 : (((myList.size() - 1) * myPack + myList.get(myList.size() - 1).size()));
        return result;
    }

    @Override
    public void ensureCapacity(int minCapacity) {
    }

    @Override
    public int getSize() {
        //if (myList.isEmpty()) return 0;
        //return ((myList.size() - 1) * myPack + myList.get(myList.size() - 1).size());
        return mySize;
    }

    public void clear() {
        myList.clear();
        mySize = 0;
    }

    private static class NotRegularReadonlyList<T> implements ReadonlyList<T> {
        private final int mySize2Power;
        private final List<T> myStart;
        private final List<List<T>> myList;
        private int myPack;

        private NotRegularReadonlyList(List<List<T>> list, int size2Power, List<T> start) {
            myList = list;
            mySize2Power = size2Power;
            myStart = start;
            myPack = (int) Math.pow(2, size2Power);
        }

        @Override
        public T get(int idx) {
            if (idx < myStart.size()) {
                return myStart.get(idx);
            }
            int corrected = idx - myStart.size();
            int itemNumber = corrected >> mySize2Power;
            return myList.get(itemNumber).get(corrected ^ (itemNumber << mySize2Power));
        }

        @Override
        public int getSize() {
            if (myList.isEmpty()) {
                return myStart.size();
            }
            return ((myList.size() - 1) * myPack + myList.get(myList.size() - 1).size()) + myStart.size();
        }
    }
}
