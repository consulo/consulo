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
package consulo.versionControlSystem.log.impl.internal.ui;

import consulo.disposer.Disposable;
import consulo.ui.model.FlatDataModelEvent;
import consulo.ui.model.LazyFlatDataModel;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

/**
 * @author VISTALL
 * @since 2026-10-18
 */
final class UnifiedVcsLogRowModel implements LazyFlatDataModel<Integer> {
    private final VcsLogCommitModel myCommitModel;
    private final List<Consumer<FlatDataModelEvent>> myListeners = new CopyOnWriteArrayList<>();

    UnifiedVcsLogRowModel(VcsLogCommitModel commitModel) {
        myCommitModel = commitModel;
    }

    @Override
    public int getSize() {
        return myCommitModel.getRowCount();
    }

    @Override
    public Integer get(int index) {
        return index;
    }

    @Override
    public int indexOf(Integer item) {
        return item >= 0 && item < getSize() ? item : -1;
    }

    @Override
    public List<Integer> fetch(int offset, int limit) {
        int end = Math.min(getSize(), offset + Math.max(0, limit));
        List<Integer> rows = new ArrayList<>(Math.max(0, end - offset));
        for (int row = offset; row < end; row++) {
            rows.add(row);
        }
        return rows;
    }

    @Override
    public Disposable addListener(Consumer<FlatDataModelEvent> listener) {
        myListeners.add(listener);
        return () -> myListeners.remove(listener);
    }

    void fireReset() {
        fire(FlatDataModelEvent.reset());
    }

    void fireAllUpdated() {
        int size = getSize();
        if (size > 0) {
            fire(FlatDataModelEvent.of(FlatDataModelEvent.Type.UPDATED, 0, size - 1));
        }
    }

    private void fire(FlatDataModelEvent event) {
        for (Consumer<FlatDataModelEvent> listener : myListeners) {
            listener.accept(event);
        }
    }

    @Override
    public Iterator<Integer> iterator() {
        int size = getSize();
        return new Iterator<>() {
            private int myRow;

            @Override
            public boolean hasNext() {
                return myRow < size;
            }

            @Override
            public Integer next() {
                if (myRow >= size) {
                    throw new NoSuchElementException();
                }
                return myRow++;
            }
        };
    }
}
