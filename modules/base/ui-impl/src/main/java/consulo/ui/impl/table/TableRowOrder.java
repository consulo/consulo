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
package consulo.ui.impl.table;

import consulo.ui.model.FlatDataModel;
import consulo.ui.model.LazyFlatDataModel;
import it.unimi.dsi.fastutil.ints.IntArrays;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * The order a table shows the rows of its model in. It is the model order itself until a column sorts it, so an
 * unsorted table reads only the rows it shows; a sort reads the whole model once and keeps the permutation until
 * the model changes.
 *
 * @author VISTALL
 * @since 2026-10-18
 */
public final class TableRowOrder<Item> {
    private final FlatDataModel<Item> myModel;

    private @Nullable Comparator<? super Item> myComparator;
    private int @Nullable [] mySorted;

    public TableRowOrder(FlatDataModel<Item> model) {
        myModel = model;
    }

    public FlatDataModel<Item> getModel() {
        return myModel;
    }

    public int size() {
        return myModel.getSize();
    }

    public boolean isSorted() {
        return myComparator != null;
    }

    public void sort(@Nullable Comparator<? super Item> comparator) {
        myComparator = comparator;
        mySorted = null;
    }

    public void modelChanged() {
        mySorted = null;
    }

    public int toModelIndex(int row) {
        int[] sorted = sorted();
        return sorted == null ? row : sorted[row];
    }

    public Item get(int row) {
        return myModel.get(toModelIndex(row));
    }

    public int rowOf(Item item) {
        int modelIndex = myModel.indexOf(item);
        int[] sorted = sorted();
        if (modelIndex < 0 || sorted == null) {
            return modelIndex;
        }

        for (int row = 0; row < sorted.length; row++) {
            if (sorted[row] == modelIndex) {
                return row;
            }
        }
        return -1;
    }

    public List<Item> fetch(int offset, int limit) {
        int size = size();
        int end = Math.min(size, offset + Math.max(0, limit));
        if (offset >= end) {
            return List.of();
        }

        int[] sorted = sorted();
        if (sorted == null && myModel instanceof LazyFlatDataModel<Item> lazy) {
            return lazy.fetch(offset, end - offset);
        }

        List<Item> page = new ArrayList<>(end - offset);
        for (int row = offset; row < end; row++) {
            page.add(myModel.get(sorted == null ? row : sorted[row]));
        }
        return page;
    }

    private int @Nullable [] sorted() {
        Comparator<? super Item> comparator = myComparator;
        if (comparator == null) {
            return null;
        }

        int[] sorted = mySorted;
        if (sorted == null || sorted.length != myModel.getSize()) {
            List<Item> items = new ArrayList<>(myModel.getSize());
            for (Item item : myModel) {
                items.add(item);
            }

            sorted = new int[items.size()];
            for (int i = 0; i < sorted.length; i++) {
                sorted[i] = i;
            }
            IntArrays.mergeSort(sorted, (left, right) -> comparator.compare(items.get(left), items.get(right)));
            mySorted = sorted;
        }
        return sorted;
    }
}
