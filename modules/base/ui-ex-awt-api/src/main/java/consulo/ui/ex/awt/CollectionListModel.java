// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.ui.ex.awt;

import consulo.util.collection.ContainerUtil;
import org.jetbrains.annotations.NonNls;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import javax.swing.*;
import java.util.*;

public class CollectionListModel<T> extends AbstractListModel<T> implements EditableModel {
  private final List<T> myItems;

  public CollectionListModel(@Nonnull Collection<? extends T> items) {
    myItems = new ArrayList<>(items);
  }

  @SuppressWarnings("UnusedParameters")
  public CollectionListModel(@Nonnull List<T> items, boolean useListAsIs) {
    myItems = items;
  }

  public CollectionListModel(@Nonnull List<? extends T> items) {
    myItems = new ArrayList<>(items);
  }

  @SafeVarargs
  public CollectionListModel(@Nonnull T... items) {
    myItems = ContainerUtil.newArrayList(items);
  }

  @Nonnull
  protected final List<T> getInternalList() {
    return myItems;
  }

  @Override
  public int getSize() {
    return myItems.size();
  }

  @Override
  public T getElementAt(int index) {
    return myItems.get(index);
  }

  public void add(T element) {
    int i = myItems.size();
    myItems.add(element);
    fireIntervalAdded(this, i, i);
  }

  public void add(int i, T element) {
    myItems.add(i, element);
    fireIntervalAdded(this, i, i);
  }

  public void add(@Nonnull List<? extends T> elements) {
    addAll(myItems.size(), elements);
  }

  public void addAll(int index, @Nonnull List<? extends T> elements) {
    if (elements.isEmpty()) return;

    myItems.addAll(index, elements);
    fireIntervalAdded(this, index, index + elements.size() - 1);
  }

  public void remove(@Nonnull T element) {
    int index = getElementIndex(element);
    if (index != -1) {
      remove(index);
    }
  }

  public void setElementAt(@Nonnull T item, int index) {
    itemReplaced(myItems.set(index, item), item);
    fireContentsChanged(this, index, index);
  }

  @SuppressWarnings("UnusedParameters")
  protected void itemReplaced(@Nonnull T existingItem, @Nullable T newItem) {
  }

  public void remove(int index) {
    T item = myItems.remove(index);
    if (item != null) {
      itemReplaced(item, null);
    }
    fireIntervalRemoved(this, index, index);
  }

  public void removeAll() {
    int size = myItems.size();
    if (size > 0) {
      myItems.clear();
      fireIntervalRemoved(this, 0, size - 1);
    }
  }

  public void contentsChanged(@Nonnull T element) {
    int i = myItems.indexOf(element);
    fireContentsChanged(this, i, i);
  }

  public void allContentsChanged() {
    fireContentsChanged(this, 0, myItems.size() - 1);
  }

  public void sort(Comparator<? super T> comparator) {
    Collections.sort(myItems, comparator);
  }

  @Nonnull
  public List<T> getItems() {
    return Collections.unmodifiableList(myItems);
  }

  public void replaceAll(@Nonnull List<? extends T> elements) {
    removeAll();
    add(elements);
  }

  @Override
  public void addRow() {
  }

  @Override
  public void removeRow(int index) {
    remove(index);
  }

  @Override
  public void exchangeRows(int oldIndex, int newIndex) {
    Collections.swap(myItems, oldIndex, newIndex);
    fireContentsChanged(this, oldIndex, oldIndex);
    fireContentsChanged(this, newIndex, newIndex);
  }

  @Override
  public boolean canExchangeRows(int oldIndex, int newIndex) {
    return true;
  }

  @NonNls
  @Override
  public String toString() {
    return getClass().getName() + " (" + getSize() + " elements)";
  }

  public List<T> toList() {
    return new ArrayList<>(myItems);
  }

  public int getElementIndex(T item) {
    return myItems.indexOf(item);
  }

  public boolean isEmpty() {
    return myItems.isEmpty();
  }

  public boolean contains(T item) {
    return getElementIndex(item) >= 0;
  }

  public void removeRange(int fromIndex, int toIndex) {
    if (fromIndex > toIndex) {
      throw new IllegalArgumentException("fromIndex must be <= toIndex");
    }
    for (int i = toIndex; i >= fromIndex; i--) {
      itemReplaced(myItems.remove(i), null);
    }
    fireIntervalRemoved(this, fromIndex, toIndex);
  }

}
