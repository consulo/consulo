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
package consulo.ui.impl.model;

import consulo.disposer.Disposable;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.model.FlatDataModelEvent;
import consulo.ui.model.MutableFlatDataModel;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

/**
 * @author VISTALL
 * @since 2026-08-02
 */
public class FlatDataModelImpl<E> implements MutableFlatDataModel<E> {
    private final List<E> myItems = new ArrayList<>();
    private final List<Consumer<FlatDataModelEvent>> myListeners = new CopyOnWriteArrayList<>();

    public FlatDataModelImpl(Collection<? extends E> items) {
        myItems.addAll(items);
    }

    @Override
    public int getSize() {
        return myItems.size();
    }

    @Override
    public E get(int index) {
        return myItems.get(index);
    }

    @Override
    public int indexOf(E item) {
        return myItems.indexOf(item);
    }

    @Override
    public Iterator<E> iterator() {
        return myItems.iterator();
    }

    @Override
    public Disposable addListener(Consumer<FlatDataModelEvent> listener) {
        myListeners.add(listener);
        return () -> myListeners.remove(listener);
    }

    @RequiredUIAccess
    @Override
    public void add(E item, int index) {
        myItems.add(index, item);

        fire(FlatDataModelEvent.of(FlatDataModelEvent.Type.ADDED, index));
    }

    @RequiredUIAccess
    @Override
    public void remove(E item) {
        int index = myItems.indexOf(item);
        if (index == -1) {
            return;
        }

        myItems.remove(index);

        fire(FlatDataModelEvent.of(FlatDataModelEvent.Type.REMOVED, index));
    }

    @RequiredUIAccess
    @Override
    public void removeAll() {
        if (myItems.isEmpty()) {
            return;
        }

        myItems.clear();

        fire(FlatDataModelEvent.reset());
    }

    @RequiredUIAccess
    @Override
    public List<E> replaceAll(Iterable<E> newItems) {
        List<E> oldItems = new ArrayList<>(myItems);

        myItems.clear();
        for (E newItem : newItems) {
            myItems.add(newItem);
        }

        fire(FlatDataModelEvent.reset());

        return oldItems;
    }

    @RequiredUIAccess
    @Override
    public void update(E item) {
        int index = myItems.indexOf(item);
        if (index == -1) {
            return;
        }

        fire(FlatDataModelEvent.of(FlatDataModelEvent.Type.UPDATED, index));
    }

    private void fire(FlatDataModelEvent event) {
        for (Consumer<FlatDataModelEvent> listener : myListeners) {
            listener.accept(event);
        }
    }
}
