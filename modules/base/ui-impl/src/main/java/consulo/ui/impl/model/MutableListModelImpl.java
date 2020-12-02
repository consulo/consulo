/*
 * Copyright 2013-2016 consulo.io
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
import com.intellij.util.EventDispatcher;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.model.MutableListModel;
import consulo.ui.model.MutableListModelListener;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * @author VISTALL
 * @since 12-Jun-16
 */
public class MutableListModelImpl<E> extends ImmutableListModelImpl<E> implements MutableListModel<E> {
  private EventDispatcher<MutableListModelListener> myDispatcher = EventDispatcher.create(MutableListModelListener.class);

  public MutableListModelImpl(Collection<? extends E> items) {
    super(items);
  }

  @RequiredUIAccess
  @Override
  public void add(@Nonnull E item, int index) {
    myItems.add(index, item);

    myDispatcher.getMulticaster().itemAdded(item);
  }

  @RequiredUIAccess
  @Override
  public void remove(@Nonnull E item) {
    myItems.remove(item);

    myDispatcher.getMulticaster().itemRemoved(item);
  }

  @RequiredUIAccess
  @Override
  public void removeAll() {
    ArrayList<E> old = new ArrayList<>(myItems);

    myItems.clear();

    for (E item : old) {
      myDispatcher.getMulticaster().itemRemoved(item);
    }
  }

  @RequiredUIAccess
  @Override
  public List<E> replaceAll(@Nonnull Iterable<E> newItems) {
    List<E> oldItems = new ArrayList<>(myItems);

    myItems.clear();

    for (E oldItem : oldItems) {
      myDispatcher.getMulticaster().itemRemoved(oldItem);
    }

    for (E newItem : newItems) {
      myItems.add(newItem);
    }

    for (E newItem : newItems) {
      myDispatcher.getMulticaster().itemAdded(newItem);
    }

    return oldItems;
  }

  @Override
  public Disposable adddListener(@Nonnull MutableListModelListener<E> modelListener) {
    myDispatcher.addListener(modelListener);
    return () -> myDispatcher.removeListener(modelListener);
  }
}
