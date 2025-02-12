/*
 * Copyright 2000-2017 JetBrains s.r.o.
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
package consulo.ide.impl.idea.ide.util.gotoByName;

import consulo.ui.UIAccess;
import consulo.ui.ex.awt.CollectionListModel;
import consulo.ui.ex.tree.TreeAnchorizer;
import consulo.util.collection.ContainerUtil;

import javax.swing.*;
import javax.swing.event.ListDataEvent;
import javax.swing.event.ListDataListener;
import java.util.List;

/**
 * @author peter
 */
class SmartPointerListModel<T> extends AbstractListModel<T> implements ModelDiff.Model<T> {
  private final CollectionListModel<Object> myDelegate = new CollectionListModel<>();
  private TreeAnchorizer myTreeAnchorizer;

  SmartPointerListModel() {
    myTreeAnchorizer = TreeAnchorizer.getService();
    myDelegate.addListDataListener(new ListDataListener() {
      @Override
      public void intervalAdded(ListDataEvent e) {
        fireIntervalAdded(e.getSource(), e.getIndex0(), e.getIndex1());
      }

      @Override
      public void intervalRemoved(ListDataEvent e) {
        fireIntervalRemoved(e.getSource(), e.getIndex0(), e.getIndex1());
      }

      @Override
      public void contentsChanged(ListDataEvent e) {
        fireContentsChanged(e.getSource(), e.getIndex0(), e.getIndex1());
      }
    });
  }

  @Override
  public int getSize() {
    return myDelegate.getSize();
  }

  @Override
  public T getElementAt(int index) {
    UIAccess.assertIsUIThread();
    return unwrap(myDelegate.getElementAt(index));
  }

  private Object wrap(T element) {
    return myTreeAnchorizer.createAnchor(element);
  }

  @SuppressWarnings("unchecked")
  private T unwrap(Object at) {
    return (T) myTreeAnchorizer.retrieveElement(at);
  }

  @Override
  public void addToModel(int idx, T element) {
    UIAccess.assertIsUIThread();
    myDelegate.add(Math.min(idx, getSize()), wrap(element));
  }

  @Override
  public void addAllToModel(int index, List<? extends T> elements) {
    UIAccess.assertIsUIThread();
    myDelegate.addAll(Math.min(index, getSize()), ContainerUtil.map(elements, this::wrap));
  }

  @Override
  public void removeRangeFromModel(int start, int end) {
    UIAccess.assertIsUIThread();
    if (start < getSize() && !isEmpty()) {
      myDelegate.removeRange(start, Math.min(end, getSize() - 1));
    }
  }

  boolean isEmpty() {
    return getSize() == 0;
  }

  void removeAll() {
    myDelegate.removeAll();
  }

  boolean contains(T elem) {
    return getItems().indexOf(elem) >= 0;
  }

  List<T> getItems() {
    return ContainerUtil.map(myDelegate.getItems(), this::unwrap);
  }
}
